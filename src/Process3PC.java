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
		Integer ackCount;
		
		Transaction(Integer transactionId, ThreePC.Role role, ThreePC.State state)
		{
			this.id 		= transactionId;
			this.role 		= role;
			this.state 		= state;		
			this.yesCount 	= 0;
			this.voteCount  = 0;
			this.ackCount   = 0;
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
	public Process3PC(Integer id, NetController network, Integer numProcs)
	{
		this.id 					= id;
		this.numProcesses			= numProcs;
		this.network 				= network;
		this.dtLog 					= new TransactionLog(true, "process" + this.id.toString() + ".log");
		this.protocolRecvQueue 		= new LinkedList<Action>();
		this.protocolSendQueue		= new LinkedList<Action>();
		this.recvKeepAlive			= new LinkedList<KeepAlive>();
		this.transactions 			= new Hashtable<Integer, Transaction>();
		this.monitor				= new ProcessMonitor(this.id, numProcs, this.network, 2000, 250);
		this.messageCount 			= 0;
		this.haltCount    			= Integer.MAX_VALUE;
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

				// Report TIMEOUT from this process for all transactions.
				for(Iterator<Integer> pi = deadProcesses.iterator(); pi.hasNext();)
				{
					Integer deadProcess = pi.next();
					for(Iterator<Map.Entry<Integer, Transaction>> ti = this.transactions.entrySet().iterator(); ti.hasNext();)
					{
						Map.Entry<Integer, Transaction> entry = ti.next();
						Integer transactionId = entry.getKey();
						handle(new Timeout(transactionId, deadProcess, this.id));
					}
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
			start3PC((BeginProtocol)action);
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
				vote((Start3PC)action);
			}
		}
		else if (transaction.state == ThreePC.State.Uncertain)
		{
			if (action instanceof Precommit)
			{
				precommit((Precommit)action);
			}
			else if (action instanceof Abort)
			{
				abort(transaction.id);
			}
		}
		else if (transaction.state == ThreePC.State.Committable)
		{
			if (action instanceof Commit)
			{
				commit(transaction.id);
			}
			else if (action instanceof Ack && transaction.role == ThreePC.Role.Coordinator)
			{
				processAck(transaction);
			}
		}
		else if (transaction.state == ThreePC.State.Committed)
		{
			
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
	 */
	private Collection<Integer> getListOfAllProcesses()
	{
		Collection<Integer> list = new LinkedList<Integer>();
		for(int i = 0; i < this.numProcesses; i++)
		{
			list.add(i);
		}
		return list;
	}
	
	private void start3PC(BeginProtocol action)
	{
		Collection<Integer> participants = getListOfAllProcesses();
		
		// Controller has selected this process to be coordinator.
		updateState(action.transactionID, ThreePC.State.Aborted);
		updateRole(action.transactionID, ThreePC.Role.Coordinator);
		
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
	}
	
	/**
	 * Upon receipt of PRECOMMIT, advance state to COMMITTABLE
	 * and send an ACK to the coordinator.
	 * @param transaction
	 */
	private void precommit(Precommit action)
	{
		updateState(action.transactionID, ThreePC.State.Committable);
		send(new Ack(action.transactionID, this.id, action.senderID, ""));
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
		}
		// Else, ABORT and send ABORT to all.
		else
		{
			abort(transaction.id);
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
	private void vote(Start3PC start3PC)
	{ 
		if (nextDecision == Decide.Yes)
		{
			voteYes(start3PC);
		}
		else if (nextDecision == Decide.No)
		{
			voteNo(start3PC);
		}
		
		// Default should be YES.
		nextDecision = Decide.Yes;
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
	private void voteNo(Start3PC start3PC)
	{
		// Write ABORT to DT log.
		abort(start3PC.transactionID);
		
		// Send ABORT to coordinator.
		send(new Abort(start3PC.transactionID, this.id, start3PC.senderID, ""));
	}
	
	private void processAck(Transaction transaction)
	{
		transaction.ackCount += 1;
		if (transaction.ackCount == this.numProcesses - 1)
		{
			commit(transaction.id);
			
			for(int i = 0; i < this.numProcesses; i++)
			{
				if (i != this.id)
				{
					send(new Commit(transaction.id, this.id, i, ""));	
				}
			}
		}
	}
	
	/**
	 * Sends current state in response to a STATE-REQ.
	 * @param request The STATE-REQ.
	 */
	private void respondToStateRequest(ThreePC.State state, ThreePC.Role role, StateRequest request)
	{
		send(new StateResponse(request.transactionID, this.id, request.senderID, state, role, ""));
	}
	
	/**
	 * Decides COMMIT: writes to DT log and changes state.
	 * @param transactionId Transaction being committed.
	 */
	private void commit(Integer transactionId)
	{
		dtLog.log(new Commit(transactionId, this.id, this.id, ""));
		System.out.println(transactionId + ": COMMIT by process " + this.id);
		updateState(transactionId, ThreePC.State.Committed);
	}
	
	/**
	 * Decides ABORT: writes to DT log and changes state.
	 * @param transactionId Transaction being aborted.
	 */
	private void abort(Integer transactionId)
	{
		dtLog.log(new Abort(transactionId, this.id, this.id, ""));
		System.out.println(transactionId + ": ABORT by process " + this.id);
		updateState(transactionId, ThreePC.State.Aborted);
	}
}
