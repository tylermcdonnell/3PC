import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import action.*;
import framework.NetController;
import log.TransactionLog;
import protocol.ThreePC;

public class Process3PC implements Runnable {
	
	/**
	 * The state kept for each 3PC transaction.
	 */
	private class Transaction
	{
		Integer id;
		ThreePC.Role role;
		ThreePC.State state;
		
		// Used to count votes if the process if the coordinator of transaction.
		Integer voteCount;
		Integer yesCount;
		
		// Used to count ACKs
		ArrayList<Integer> acks;
		
		// This is used to assess timeouts from relevant individuals during a transaction.
		ArrayList<Integer> waitingOn;
		
		boolean committed;
		boolean aborted;
		
		Transaction(Integer transactionId, ThreePC.Role role, ThreePC.State state)
		{
			this.id 		= transactionId;
			this.role 		= role;
			this.state 		= state;		
			this.yesCount 	= 0;
			this.voteCount  = 0;
			this.acks 		= new ArrayList<Integer>();
			this.waitingOn  = new ArrayList<Integer>();
			
			this.committed 	= false;
			this.aborted   	= false;
		}
	}
	
	// Possible vote decisions.
	private enum Decide
	{
		Yes, No
	}
	
	// State for all active transactions.
	private Hashtable<Integer, Transaction> transactions;

	// An outgoing queue of PROTOCOL messages. This is used to support the testing command
	// partialMessage. During the core part of the main processing loop, we only enqueue 
	// messages in the outgoing queue. At the end of every processing loop, we send the
	// messages that have been enqueued over the socket. 
	// 
	// Consider the following concrete scenario: 
	// Three processes: coordinator p(0) and participants and p(1) and p(2).
	// partialMessage(0,1) tells the coordinator to halt after sending one message to p(1).
	// We might then choose to kill p(2) and resume execution so that the message to p(2) is
	// not delivered. With the protocol queue, both of these outgoing messages would be 
	// enqueued, but only the first would be sent before HALT. Later on, when RESUME is
	// sent, the message to p(2) is still first in the queue.
	private LinkedList<Action> protocolSendQueue;
	
	// Buffered queue of received protocol messages (i.e., keep-alives have been filtered)
	private LinkedList<Action> protocolRecvQueue;
	
	// Buffered list of received keep-alive messages to report to monitor.
	private LinkedList<KeepAlive> recvKeepAlive;
	
	// Stable storage
	private TransactionLog dtLog;
	
	// This process's id
	private Integer id = 0;
	
	// Total number of processes (including this process).
	private Integer numProcesses;
	
	// Used to monitor the life of all processes via Keep-Alives.
	private ProcessMonitor monitor;
	
	// This can be used to alter the next vote decision. YES by default.
	private volatile Decide nextDecision = Decide.Yes;

	// A cumulative message count for all PROTOCOL messages.
	// Note: may be modified/read by this process or controller.
	private volatile Integer messageCount;
	
	// When message count hits this number, protocol should halt.
	// Note: may be modified/read by this process or controller.
	private volatile Integer haltCount;
	
	// Control variable used to halt PROTOCOL progress in main loop.
	// Note: does not affect process monitor
	// Note: may be modified/read by this process or controller.
	private volatile boolean halted;
	
	private NetController network;
	
	/**
	 * Constructor.
	 * @param id		ID of this process
	 * @param network	Network to communicate with all other processes
	 * @param numProcs	Total number of processes
	 */
	public Process3PC(Integer id, NetController network, Integer numProcs, boolean clearStableStorage)
	{
		this.id 					= id;
		this.numProcesses			= numProcs;
		this.network 				= network;
		this.dtLog 					= new TransactionLog(clearStableStorage, "process" + this.id.toString() + ".log");
		this.protocolRecvQueue 		= new LinkedList<Action>();
		this.protocolSendQueue		= new LinkedList<Action>();
		this.recvKeepAlive			= new LinkedList<KeepAlive>();
		this.transactions 			= new Hashtable<Integer, Transaction>();
		this.monitor				= new ProcessMonitor(this.id, numProcs, this.network, 2000, 250);
		this.messageCount 			= 0;
		this.haltCount    			= Integer.MAX_VALUE;
		
		if (clearStableStorage == false)
		{
			System.out.println("Recovering...");
			recover();
		}
	}
	
	public synchronized void start(Integer transactionId)
	{
		synchronized(this.protocolRecvQueue)
		{
			this.protocolRecvQueue.add(new BeginProtocol(transactionId, this.id, this.id));
		}
	}
	
	/**
	 * Have this process halt protocol after sending specified number of messages.
	 * @param n
	 */
	public void haltAfter(Integer n)
	{
		this.haltCount = this.messageCount + n;
	}
	
	/**
	 * If this process's protocol has been halted, resume.
	 */
	public void resumeMessages()
	{
		this.halted = false;
		this.haltCount = Integer.MAX_VALUE;
	}
	
	/**
	 * Read through DT log (stable storage) to initialize transaction states.
	 */
	private void recover()
	{
		ArrayList<Action> history = this.dtLog.read();
		
		// Load status of each transaction in history. We should be able to 
	    // simply do this sequentially, since the most recent entry in the
		// DT log should be the most important. For instance, it is possible
		// that we wrote a YES to the log and then later an ABORT. By traversing
		// the DT log sequentially, we would overwrite the UNCERTAIN state
		// with the final ABORT state.
		for (Iterator<Action> i = history.iterator(); i.hasNext();)
		{
			Action a = i.next();
			if (a instanceof Yes)
			{
				this.transactions.put(a.transactionID, new Transaction(a.transactionID, ThreePC.Role.Participant, ThreePC.State.Uncertain));
			}
			if (a instanceof Abort)
			{
				this.transactions.put(a.transactionID, new Transaction(a.transactionID, ThreePC.Role.Participant, ThreePC.State.Aborted));
				this.transactions.get(a.transactionID).aborted = true;
			}
			if (a instanceof Commit)
			{
				this.transactions.put(a.transactionID, new Transaction(a.transactionID, ThreePC.Role.Participant, ThreePC.State.Committed));
				this.transactions.get(a.transactionID).committed = true;
			}
		}
		
		// (1) For all UNCERTAIN transactions, send out to STATE-REQ to all
		// 	   processes. This is necessary in case all other processes have 
		//     come to a decision and are no longer planning to broadcast 
		//     that decision to other processes.
		// (2) For all processes COMMITTED or ABORTED transactions, broadcast
		// 	   decisions to all live nodes.
		for (Iterator<Map.Entry<Integer, Transaction>> i = this.transactions.entrySet().iterator(); i.hasNext();)
		{
			Transaction t = i.next().getValue();
			if (t.state == ThreePC.State.Uncertain)
			{
				System.out.println("Sending STATE REQ");
				sendStateRequest(t.id, getListOfAllProcesses(this.id));
			}
			if (t.state == ThreePC.State.Aborted)
			{
				sendAbort(t.id, this.monitor.getLive());
			}
			if (t.state == ThreePC.State.Committed)
			{
				sendCommit(t.id, this.monitor.getLive());
			}
		}
	}
	
	/**
	 * This is the "life" of the process. i.e., this is the main processing loop.
	 */
	public void run()
	{
		while(true)
		{
			// Pull all messages from network and filter.
			receiveAll();
			
			// Update statuses of processes with received keep-alive messages.
			Collection<Integer> deadProcesses = monitor.monitor(recvKeepAlive);
			
			if(!this.halted)
			{
				// Process all received messages.
				synchronized(this.protocolRecvQueue)
				{
					for(Iterator<Action> i = this.protocolRecvQueue.iterator(); i.hasNext();)
					{
						Action a = i.next();
						i.remove();
						handle(a);
					}
				}

				// Notify transactions waiting on dead processes.
				for (Iterator<Map.Entry<Integer, Transaction>> ti = this.transactions.entrySet().iterator(); ti.hasNext();)
				{
					Map.Entry<Integer, Transaction> entry = ti.next();
					Transaction t = entry.getValue();
					for (Iterator<Integer> pi = deadProcesses.iterator(); pi.hasNext();)
					{
						Integer deadProcess = pi.next();
						if (t.waitingOn.contains(deadProcess))
						{
							handle(new Timeout(t.id, deadProcess, this.id));
						}
					}
				}
				
				for(Iterator<Integer> pi = deadProcesses.iterator(); pi.hasNext();)
				{
					Integer deadProcess = pi.next();

				}
				
				// Send all outgoing messages, constrained by haltCount
				sendAll();
			}
		}
	}
	
	/**
	 * Receive all messages from the network and filter them into Keep-Alive and
	 * protocol queues. This allows us to maintain life monitoring while 
	 * separately pausing the protocol for testing purposes.
	 */
	public void receiveAll()
	{
		List<Action> received = network.getReceived();
		
		for (Iterator<Action> i = received.iterator(); i.hasNext();)
		{
			Action a = i.next();
			if (a instanceof KeepAlive)
			{
				this.recvKeepAlive.add((KeepAlive)a);
			}
			else
			{
				this.protocolRecvQueue.add(a);
			}
		}
	}
	
	/**
	 * Enqueued messages are sent over the socket. We enqueue all messages and 
	 * then send with SendAll so that we can enforce partialMessage.
	 */
	public void sendAll()
	{
		for(Iterator<Action> i = this.protocolSendQueue.iterator(); i.hasNext();)
		{
			if (this.messageCount >= this.haltCount)
			{
				this.halted = true;
				return;
			}
			Action a = i.next();
			i.remove();
			this.network.sendMsg(a.destinationID, a);
			this.messageCount += 1;
		}
	}
	
	/**
	 * Enqueues an action to be sent to target process. Action will actually
	 * be sent over the socket on a call to sendAll subject to halting logic.
	 * @param action	Action to be sent.
	 */
	public void send(Action action)
	{
		this.protocolSendQueue.add(action);
	}
	
	/**
	 * Public handler for incoming actions.
	 * @param action
	 */
	public void handle(Action action)
	{
		System.out.println(action.transactionID + ": Process " + this.id + " receives [" + action.toString() + "] from Process " + action.senderID);
		Transaction transaction = transactions.get(action.transactionID);
		if(transaction == null)
		{
			transaction = createTransaction(action.transactionID, ThreePC.Role.Participant, ThreePC.State.Aborted);
		}
		
		// Controller selected this process to begin 3PC as coordinator.
		if (action instanceof BeginProtocol)
		{
			start3PC(transaction, (BeginProtocol)action);
		}
		
		if (transaction.state == ThreePC.State.Aborted)
		{
			if (action instanceof Yes && transaction.role == ThreePC.Role.Coordinator)
			{
				countVote(transaction, Decide.Yes);
			}
			else if (action instanceof Abort && transaction.role == ThreePC.Role.Coordinator)
			{
				countVote(transaction, Decide.No);
			}
			else if (action instanceof Start3PC)
			{
				vote(transaction, (Start3PC)action);
			}
		}
		else if (transaction.state == ThreePC.State.Uncertain)
		{
			if (action instanceof Precommit)
			{
				precommit(transaction, (Precommit)action);
			}
			else if (action instanceof Abort)
			{
				abort(transaction);
			}
		}
		else if (transaction.state == ThreePC.State.Committable)
		{
			if (action instanceof Commit)
			{
				commit(transaction);
			}
			else if (action instanceof Ack && transaction.role == ThreePC.Role.Coordinator)
			{
				// Document ACK.
				processAck((Ack)action, transaction);
			}
			else if (action instanceof Timeout && transaction.role == ThreePC.Role.Coordinator)
			{
				// Even if we don't receive all ACKs, COMMIT and send COMMIT
				// to those who did ACK. Note that by committing, this process
				// moves out of the COMMITTABLE state, so multiple timeouts
				// from different processes will not all generate commit messages.
				commit(transaction);
				sendCommit(transaction.id, transaction.acks);
			}
		}
		else if (transaction.state == ThreePC.State.Committed)
		{
			
		}
		
		// Handling these commands should be the same independent of state.
		if (action instanceof Commit)
		{
			commit(transaction);
		}
		if (action instanceof Abort)
		{
			abort(transaction);
		}
		if (action instanceof StateRequest)
		{
			respondToStateRequest(transaction.state, transaction.role, (StateRequest)action);
		}
	}
	
	/**
	 * Creates a new 3PC transaction.
	 * @param transactionId	ID of transaction
	 * @param initialRole	Coordinator or Participant
	 * @param initialState	Aborted, Uncertain, Committable, or Committed
	 */
	private Transaction createTransaction(Integer transactionId, ThreePC.Role initialRole, ThreePC.State initialState)
	{
		Transaction t = new Transaction(transactionId, initialRole, initialState);
		try
		{
			this.transactions.put(transactionId, t);
			return t;
		}
		catch (Exception ex)
		{
			System.out.println("ERROR: while trying to create a new transaction.");
			return t;
		}
	}
	
	/**
	 * Updates the state of an existing 3PC transaction.
	 * @param id			id of process to update
	 * @param initialState	new state, Aborted, Uncertain, Committable, Committed
	 */
	private void updateState(Integer transactionId, ThreePC.State state)
	{
		try
		{
			Transaction t = this.transactions.get(transactionId);
			t.state = state;
		}
		catch (NullPointerException ex)
		{
			System.out.println("ERROR: while updating state. This should never happen.");
		}
	}
	
	/**
	 * Updates the role of this process in an existing 3PC transaction.
	 * @param transactionId	id of process to update
	 * @param role			new role, Participant or Coordinator
	 */
	private void updateRole(Integer transactionId, ThreePC.Role role)
	{
		try
		{
			Transaction t = this.transactions.get(transactionId);
			t.role = role;
		}
		catch (NullPointerException ex)
		{
			System.out.println("ERROR: while updating state. This should never happen.");
		}
	}
	
	/**
	 * Returns a collection of all IDs in range (0, numProcesses).
	 * @param exclude Process to exclude (used to exclude self)
	 */
	private Collection<Integer> getListOfAllProcesses(Integer exclude)
	{
		Collection<Integer> list = new LinkedList<Integer>();
		for(int i = 0; i < this.numProcesses; i++)
		{
			if (i != exclude)
			{
				list.add(i);
			}
		}
		return list;
	}
	
	private void start3PC(Transaction t, BeginProtocol action)
	{
		Collection<Integer> participants = getListOfAllProcesses(this.id);
		
		// Controller has selected this process to be coordinator.
		updateState(t.id, ThreePC.State.Aborted);
		updateRole(t.id, ThreePC.Role.Coordinator);
		
		// Log START3PC.
		this.dtLog.log(new Start3PC(action.transactionID, this.id, this.id, "", participants));
		
		// Send VOTE-REQ to all processes.		
		for (int i = 0; i < this.numProcesses; i++)
		{
			if (i != this.id)
			{
				send(new Start3PC(action.transactionID, this.id, i, "", participants));
			}
		}
		
		// We are now waiting on responses form all processes.
		t.waitingOn.addAll(getListOfAllProcesses(this.id));
	}
	
	/**
	 * Upon receipt of PRECOMMIT, advance state to COMMITTABLE
	 * and send an ACK to the coordinator.
	 * @param transaction
	 */
	private void precommit(Transaction t, Precommit action)
	{
		// We are no longer waiting for PRECOMMIT or ABORT.
		t.waitingOn.clear();
		
		updateState(action.transactionID, ThreePC.State.Committable);
		send(new Ack(action.transactionID, this.id, action.senderID, ""));
		
		// We are now waiting on a COMMIT message from the coordinator.
		t.waitingOn.add(action.senderID);
	}
	
	private void countVote(Transaction transaction, Decide vote)
	{
		transaction.voteCount += 1;
		
		if (vote == Decide.Yes)
		{
			transaction.yesCount += 1;
		}
		
		// All participants have voted.
		if (transaction.voteCount == this.numProcesses - 1)
		{
			endVoting(transaction);
		}
	}
	
	private void endVoting(Transaction transaction)
	{
		// We are no longer waiting on any participants.
		transaction.waitingOn.clear();
		
		// If all participants voted YES, PRECOMMIT and send PRECOMMIT to all.
		if (transaction.yesCount == this.numProcesses - 1)
		{
			updateState(transaction.id, ThreePC.State.Committable);
			for(int i = 0; i < this.numProcesses; i++)
			{
				if (i !=  this.id)
				{
					send(new Precommit(transaction.id, this.id, i, ""));
				}
			}
			
			// We are now waiting on ACKs from all participants.
			transaction.waitingOn.addAll(getListOfAllProcesses(this.id));
		}
		// Else, ABORT and send ABORT to all.
		else
		{
			abort(transaction);
			for(int i = 0; i < this.numProcesses; i++)
			{
				if (i != this.id);
				{
					send(new Abort(transaction.id, this.id, i, ""));
				}
			}
		}
		

	}
	
	/**
	 * Process casts 3PC vote: either YES or NO. 
	 * 
	 * If no, process can log ABORT and conclude 3PC.
	 * 
	 * If yes, process advances to UNCERTAIN state.
	 * 
	 * @param start3PC	triggering START3PC action
	 */
	private void vote(Transaction t, Start3PC start3PC)
	{ 
		if (nextDecision == Decide.Yes)
		{
			voteYes(start3PC);
		}
		else if (nextDecision == Decide.No)
		{
			voteNo(t, start3PC);
		}
		
		// Default should be YES.
		nextDecision = Decide.Yes;
		
		// We are now waiting on a response from the coordinator.
		t.waitingOn.add(start3PC.senderID);
	}
	
	/**
	 * Votes YES in response to a VOTE-REQ.
	 * @param start3PC
	 */
	private void voteYes(Start3PC start3PC)
	{
		// Write YES to DT log.
		dtLog.log(new Yes(start3PC.transactionID, this.id, start3PC.senderID, "", start3PC.getParticipants()));
		
		// Send YES to coordinator.
		send(new Yes(start3PC.transactionID, this.id, start3PC.senderID, "", start3PC.getParticipants()));
		
		// Now uncertain and awaiting coordinator.
		updateState(start3PC.transactionID, ThreePC.State.Uncertain);
	}
	
	/**
	 * Votes NO in response to a VOTE-REQ.
	 * @param start3PC VOTE-REQ from a coordinator.
	 */
	private void voteNo(Transaction t, Start3PC start3PC)
	{
		// Write ABORT to DT log.
		abort(t);
		
		// Send ABORT to coordinator.
		send(new Abort(start3PC.transactionID, this.id, start3PC.senderID, ""));
	}
	
	private void processAck(Ack action, Transaction transaction)
	{
		transaction.acks.add(action.senderID);
		if (transaction.acks.size() == this.numProcesses - 1)
		{
			commit(transaction);
			sendCommit(transaction.id, transaction.acks);
		}
	}
	
	/**
	 * Sends COMMIT to the specified processes.
	 * @param transactionId 	ID of transaction for this COMMIT
	 * @param processes 		List of process IDs
	 */
	private void sendCommit(Integer transactionId, Collection<Integer> processes)
	{
		for(Iterator<Integer> i = processes.iterator(); i.hasNext();)
		{
			send(new Commit(transactionId, this.id, i.next(), ""));	
		}
	}
	
	/**
	 * Sends STATE-REQ to specified processes.
	 * @param transactionId 	ID of transaction for this STATEREQ
	 * @param processes			List of process IDs
	 */
	private void sendStateRequest(Integer transactionId, Collection<Integer> processes)
	{
		for(Iterator<Integer> i = processes.iterator(); i.hasNext();)
		{
			send(new StateRequest(transactionId, this.id, i.next(), ""));	
		}
	}
	
	/**
	 * Sends ABORT to specified processes.
	 * @param transactionId		ID of transaction for this ABORT
	 * @param processes			List of process IDs
	 */
	private void sendAbort(Integer transactionId, Collection<Integer> processes)
	{
		for(Iterator<Integer> i = processes.iterator(); i.hasNext();)
		{
			send(new Abort(transactionId, this.id, i.next(), ""));	
		}
	}
	
	/**
	 * Sends current state in response to a STATE-REQ.
	 * @param request The STATE-REQ.
	 */
	private void respondToStateRequest(ThreePC.State state, ThreePC.Role role, StateRequest request)
	{
		System.out.println("Responding to state request.");
		if (state == ThreePC.State.Committed)
		{
			send(new Commit(request.transactionID, this.id, request.senderID, ""));
		}
		//TODO TYLER: I am skeptical that ABORTED is sufficient, since we by default start in this state.
		if (state == ThreePC.State.Aborted)
		{
			send(new Abort(request.transactionID, this.id, request.senderID, ""));
		}
			
	    // TYLER: I don't think we need a separate message type.
		//send(new StateResponse(request.transactionID, this.id, request.senderID, state, role, ""));
	}
	
	/**
	 * Decides COMMIT: writes to DT log and changes state.
	 * @param transactionId Transaction being committed.
	 */
	private void commit(Transaction t)
	{
		if (!t.committed)
		{
			t.committed = true;
			dtLog.log(new Commit(t.id, this.id, this.id, ""));
			System.out.println(t.id + ": COMMIT by process " + this.id);
			updateState(t.id, ThreePC.State.Committed);
			
			// We are no longer waiting on anyone. We're done.
			t.waitingOn.clear();
		}
	}
	
	/**
	 * Decides ABORT: writes to DT log and changes state.
	 * @param transactionId Transaction being aborted.
	 */
	private void abort(Transaction t)
	{
		if (!t.aborted)
		{
			dtLog.log(new Abort(t.id, this.id, this.id, ""));
			System.out.println(t.id + ": ABORT by process " + this.id);
			updateState(t.id, ThreePC.State.Aborted);
			
			// We are no longer waiting on anyone. We're done.
			t.waitingOn.clear();
		}
	}
}
