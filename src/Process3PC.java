import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import action.*;
import framework.NetController;
import log.PlaylistLog;
import log.TransactionLog;
import playlist.Playlist;
import playlist.PlaylistAction;

public class Process3PC implements Runnable {
	
	// Possible 3PC roles.
	// Coordinator: Process "coordinating" the 3PC protocol.
	// Participant: Process voting in 3PC protocol.
	public enum Role
	{
		Coordinator, Participant;
	}
	
	// Possible 3PC states.
	// Aborted: 	The process has not voted, has voted NO, or has received an ABORT.
	// Uncertain:	The process has voted YES but not received a PRECOMMIT or ABORT.
	// Committable:	The process has received PRECOMMIT, but has not received COMMIT.
	// Committed:	The process has received and decided to COMMIT. 
	public enum State 
	{
		Aborted, Uncertain, Committable, Committed
	}
	
	/**
	 * The state kept for each 3PC transaction.
	 */
	private class Transaction
	{
		// The ID of this transaction, i.e., one instance of 3PC.
		Integer id;
		
		// The action underlying this 3PC protocol.
		PlaylistAction playlistAction;
		
		// These are the role and state of this process with regard
		// to this transaction (i.e., one instance of 3PC).
		Role role;
		State state;
		
		// Used to count votes if the process if the coordinator of transaction.
		Integer voteCount;
		Integer yesCount;
		
		// Used to count ACKs
		ArrayList<Integer> acks;
		
		// This is used to assess timeouts from relevant individuals during a transaction.
		ArrayList<Integer> waitingOn;
		
		boolean committed;
		boolean aborted;
		
		// This is the UP list for the transaction. This is really tricky.
		// We only "remove" a process from the UP list when (a) it is the 
		// coordinator and (b) we observe that it has failed. We do not
		// remove processes that fail at any point from the UP list. So,
		// we will increment UP if the current UP value is the ID of the
		// person we believe to be the coordinator and we observe that they
		// are dead. Additionally, if we receive a STATE-REQ from a process
		// of an ID higher than our current UP, we will set UP to the ID of
		// the sender of that STATE-REQ, as we can conclude that they are 
		// the new coordinator. FINALLY, it is possible that leader election
		// will continue even after a total failure, so we % UP by the number
		// of processes N. 
		Integer UP;
		
		// *******************************************************************
		// * State used ONLY if the process is elected coordinator.          *
		// *******************************************************************
		
		// This is a boolean value indicating that this process is currently
		// the elected coordinator and thus in the termination protocol.
		boolean inTerminationProtocol;
		
		// The list of participants live at the moment the termination protocol begins.
		Collection<Integer> terminationParticipants;
		
		// The list of termination participants who responded Committable
		Collection<Integer> terminationCommittable;
		
		// The list of termination participants who responded Uncertain
		Collection<Integer> terminationUncertain;
		
		Transaction(Integer transactionId, Role role, State state, PlaylistAction action)
		{
			this.id 			= transactionId;
			this.role 			= role;
			this.state 			= state;		
			this.yesCount 		= 0;
			this.voteCount  	= 0;
			this.acks 			= new ArrayList<Integer>();
			this.waitingOn  	= new ArrayList<Integer>();
			
			this.committed 		= false;
			this.aborted   		= false;
			
			this.UP 			= 0;
			this.playlistAction = action;
			
			this.inTerminationProtocol = false;
			this.terminationParticipants  = new ArrayList<Integer>();
			this.terminationCommittable   = new ArrayList<Integer>();
			this.terminationUncertain     = new ArrayList<Integer>();
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
	
	// Stable storage -- 3Pc data.
	private TransactionLog dtLog;
	
	// Stable storage -- Playlist data.
	private PlaylistLog playlistLog;
	
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
		this.playlistLog            = new PlaylistLog(clearStableStorage, "process" + this.id.toString() + "Playlist.log");
		this.protocolRecvQueue 		= new LinkedList<Action>();
		this.protocolSendQueue		= new LinkedList<Action>();
		this.recvKeepAlive			= new LinkedList<KeepAlive>();
		this.transactions 			= new Hashtable<Integer, Transaction>();
		this.monitor				= new ProcessMonitor(this.id, numProcs, this.network, 1000, 250);
		this.messageCount 			= 0;
		this.haltCount    			= Integer.MAX_VALUE;
		
		if (clearStableStorage == false)
		{
			System.out.println("Recovering...");
			recover();
		}
	}
	
	public synchronized void start(Integer transactionId, PlaylistAction playlistAction)
	{
		synchronized(this.protocolRecvQueue)
		{
			this.protocolRecvQueue.add(new BeginProtocol(transactionId, this.id, this.id, playlistAction));
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
	
	public void nextDecision(boolean decision)
	{
		if (decision)
		{
			this.nextDecision = Decide.Yes;
		}
		else
		{
			this.nextDecision = Decide.No;
		}
	}
	
	/**
	 * Read through DT log (stable storage) to initialize transaction states.
	 */
	private void recover()
	{
		// TODO: MIKE: when recovering, if there is a COMMIT in the stable storage
		// for a specific transaction, make sure that the edit/delete/add was carried
		// out in the playlistStorage.  At the top of the playlistStorage file
		// will be a list of transactions which were finished.  We assume that a write
		// to the playlistStorage log is atomic -- either all or nothing is written. This
		// is valid given our implementation, because the chances of a write to
		// stable storage being interrupted by some sort of kill command is negligible.
		
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
			if (a instanceof Start3PC)
			{
				this.transactions.put(a.transactionID, new Transaction(a.transactionID, Role.Participant, State.Uncertain, a.playlistAction));
			}
			if (a instanceof Yes)
			{
				this.transactions.put(a.transactionID, new Transaction(a.transactionID, Role.Participant, State.Uncertain, a.playlistAction));
			}
			if (a instanceof Precommit)
			{
				this.transactions.put(a.transactionID, new Transaction(a.transactionID, Role.Participant, State.Committable, a.playlistAction));
			}
			if (a instanceof Abort)
			{
				this.transactions.put(a.transactionID, new Transaction(a.transactionID, Role.Participant, State.Aborted, a.playlistAction));
				this.transactions.get(a.transactionID).aborted = true;
			}
			if (a instanceof Commit)
			{
				this.transactions.put(a.transactionID, new Transaction(a.transactionID, Role.Participant, State.Committed, a.playlistAction));
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
			if (t.state == State.Uncertain || t.state == State.Committable)
			{
				System.out.println("Process " + this.id + " is asking other processes for decisions.");
				sendDecisionRequest(t, getListOfAllProcesses(this.id));
			}
			if (t.state == State.Aborted)
			{
				sendAbort(t, this.monitor.getLive());
			}
			if (t.state == State.Committed)
			{	
				sendCommit(t.id, this.monitor.getLive(), t.playlistAction);
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
			// Receive all messages from the network and filter them into Keep-Alive and
			// protocol queues.
			receiveAll();
			
			// Update statuses of processes with received keep-alive messages.
			// Get processes that are currently dead.
			Collection<Integer> deadProcesses = monitor.monitor(recvKeepAlive);
			
			//******************************************************************
			//* Below is protocol only (no keep-alive stuff).
			//******************************************************************
			
			// A process is halted if and only if it is given a sendPartial command.
			if (!this.halted)
			{
				// Process all received messages.
				synchronized(this.protocolRecvQueue)
				{
					for (Iterator<Action> i = this.protocolRecvQueue.iterator(); i.hasNext();)
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
							// MIKE: Pass in null for the PlaylistAction because this is not relevant
							// here.
							handle(new Timeout(t.id, deadProcess, this.id, null));
						}
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
	 * @return True if this process is the current coordinator of the transaction
	 * with the specified ID.
	 */
	public boolean isCoordinator(Integer id)
	{
		Transaction t = this.transactions.get(id);
		if(t == null)
		{
			return false;
		}
		else
		{
			return t.role == Role.Coordinator;
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
		
		// This means that this Action is the first message this process has
		// received about this transaction. That means this process is not
		// currently the coordinator of this transaction. Typically, the
		// first message a process receives will be a START3PC (i.e., VOTE-REQ),
		// but in certain failure cases, it is possible for the process to
		// receive a different initial message.
		if (transaction == null)
		{
			transaction = createTransaction(action.transactionID, Role.Participant, State.Aborted, action.playlistAction);
		}
		
		//*******************************************************************************
		//*******************************************************************************
		//* HANDLING THESE ACTIONS IS THE SAME REGARDLESS OF THE PROCESS'S CURRENT STATE*
		//*******************************************************************************
		//*******************************************************************************
		
		// CONTROLLER selected this process to begin 3PC as coordinator.
		if (action instanceof BeginProtocol)
		{
			start3PC(transaction, (BeginProtocol)action);
		}
		
		// Coordinator timed out; carry out election protocol.
		if (action instanceof Timeout && action.senderID == transaction.UP)
		{
			transaction.UP += 1;
			electionProtocol(transaction);
		}
		
		// Receives STATE-REQ from an elected coordinator. If the ID of this elected
		// coordinator is less than the value of our UP set, we can ignore them. If
		// the value of their ID is higher than our UP set, we must update our UP
		// set to reflect that they are the new coordinator.
		if (action instanceof StateRequest)
		{
			System.out.println("Process " + this.id + " RECEIVED STATE REQUEST: " + action.senderID + " " + transaction.UP);
			if (action.senderID >= transaction.UP)
			{
				System.out.println("Process " + this.id + " answering " + action.senderID);
				transaction.role 	= Role.Participant;
				transaction.UP 		= action.senderID;
				respondToStateRequest((StateRequest)action, transaction);
			}
		}
		
		// Receives DEC-REQ from a process that has died and recovered. We can respond
		// to this if we have decided either COMMIT or ABORT. Otherwise, send UNCERTAIN.
		if (action instanceof DecisionRequest)
		{
			respondToDecisionRequest((DecisionRequest)action, transaction);
		}
		
		// If this process receives a YOU-ARE-ELECTED message, it is now the coordinator.
		// Begin termination protocol by sending STATE-REQ to all participants.
		if (action instanceof YouAreElected && transaction.role == Role.Participant)
		{
			transaction.role 					= Role.Coordinator;
			transaction.inTerminationProtocol 	= true;
			transaction.terminationParticipants = this.monitor.getLive();
			sendStateRequests(transaction);
		}
		
		if (transaction.inTerminationProtocol)
		{
			if (action instanceof Commit)
			{
				commit(transaction);
				transaction.inTerminationProtocol = false;
				sendCommit(transaction.id, transaction.terminationParticipants, transaction.playlistAction);
				return; // Termination protocol complete.
			}
			else if (action instanceof Abort)
			{
				abort(transaction);
				transaction.inTerminationProtocol = false;
				sendAbort(transaction, transaction.terminationParticipants);
				return; // Termination protocol complete.
			}
			else if (action instanceof Committable)
			{
				transaction.terminationCommittable.add(action.senderID);
			}
			else if (action instanceof Uncertain)
			{
				transaction.terminationUncertain.add(action.senderID);
			}
			
			// If all processes have responded, proceed.
			if (transaction.terminationCommittable.size() + 
				transaction.terminationUncertain.size() ==
				transaction.terminationParticipants.size())
			{
				// If at least one process is COMMITTABLE, send PRE-COMMIT and proceed with
				// regular protocol.
				if (transaction.terminationCommittable.size() > 0)
				{
					sendPrecommit(transaction);
				}
				// If all processes are UNCERTAIN, decide ABORT and send to all participants.
				else
				{
					abort(transaction);
					transaction.inTerminationProtocol = false;
					sendAbort(transaction, transaction.terminationParticipants);
					return; // Termination protocol complete.
				}
			}
		}
		
		// If we receive a COMMIT, someone has decided, and we can apply their decision.
		if (action instanceof Commit)
		{
			commit(transaction);
		}
		
		// If we receive ABORT, someone has decided, and we can apply their decision.
		if (action instanceof Abort)
		{
			abort(transaction);
		}
		
		//*******************************************************************************
		//*******************************************************************************
		//* HANDLING THESE ACTIONS IS STATE-DEPENDENT                                   *
		//*******************************************************************************
		//*******************************************************************************
		
		// If the process receiving the action is ABORTED.
		if (transaction.state == State.Aborted)
		{
			// IF we are coordinator and we got a "YES" vote, this is step (2).
			if (action instanceof Yes && transaction.role == Role.Coordinator)
			{
				countVote(transaction, Decide.Yes, action);
			}
			
			// If we are coordinator and we got a "NO" vote, this is step (2).
			else if (action instanceof Abort && transaction.role == Role.Coordinator)
			{
				countVote(transaction, Decide.No, action);
			}
			
			// We are a participant receiving VOTE-REQ.
			else if (action instanceof Start3PC)
			{
				vote(transaction, (Start3PC)action);
			}
		}
		// If the process receiving the action has voted YES, but not received PRECOMMIT or ABORT.
		else if (transaction.state == State.Uncertain)
		{
			// We are a participant receiving PRE-COMMIT.
			if (action instanceof Precommit)
			{
				precommit(transaction, (Precommit)action);
			}
			
			// 
			else if (action instanceof Abort)
			{
				abort(transaction);
			}
		}
		// If the process receiving the action has received PRECOMMIT, but no COMMIT or ABORT.
		else if (transaction.state == State.Committable)
		{
			if (action instanceof Commit)
			{
				commit(transaction);
			}
			else if (action instanceof Ack && transaction.role == Role.Coordinator)
			{
				// Document ACK.
				processAck((Ack)action, transaction);
			}
			else if (action instanceof Timeout && transaction.role == Role.Coordinator)
			{
				// Even if we don't receive all ACKs, COMMIT and send COMMIT
				// to those who did ACK. Note that by committing, this process
				// moves out of the COMMITTABLE state, so multiple timeouts
				// from different processes will not all generate commit messages.
				commit(transaction);
				sendCommit(transaction.id, transaction.acks, action.playlistAction);
			}
		}
		// If the process has received COMMIT.
		else if (transaction.state == State.Committed)
		{
			
		}
	}
	
	/**
	 * Creates a new 3PC transaction.
	 * @param transactionId	ID of transaction
	 * @param initialRole	Coordinator or Participant
	 * @param initialState	Aborted, Uncertain, Committable, or Committed
	 */
	private Transaction createTransaction(Integer transactionId, Role initialRole, State initialState, PlaylistAction playlistAction)
	{
		Transaction t = new Transaction(transactionId, initialRole, initialState, playlistAction);
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
	private void updateState(Integer transactionId, State state)
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
	private void updateRole(Integer transactionId, Role role)
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
		updateState(t.id, State.Aborted);
		updateRole(t.id, Role.Coordinator);
		
		// Log START3PC.
		this.dtLog.log(new Start3PC(action.transactionID, this.id, this.id, "", participants, action.playlistAction));
		
		// Send VOTE-REQ to all processes.		
		for (int i = 0; i < this.numProcesses; i++)
		{
			if (i != this.id)
			{
				send(new Start3PC(action.transactionID, this.id, i, "", participants, action.playlistAction));
			}
		}
		
		// We are now waiting on responses form all processes.
		t.waitingOn.addAll(getListOfAllProcesses(this.id));
	}
	
	/**
	 * This should only be called when the process this process believes to 
	 * be coordinator is dead. Carry out the election protocol: send URELECTED
	 * to the process with ID = UP % N, where N is the number of processes 
	 * and UP is the UP set, represented as an Integer.
	 */
	private void electionProtocol(Transaction t)
	{
		Integer newCoordinator = t.UP % this.numProcesses;
		send(new YouAreElected(t.id, this.id, newCoordinator, t.playlistAction));
		
		// We are now waiting on this new coordinator.
		t.waitingOn.clear();
		t.waitingOn.add(newCoordinator);
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
		
		updateState(t.id, State.Committable);
		this.dtLog.log(action);
		send(new Ack(t.id, this.id, action.senderID, action.playlistAction));
		
		// We are now waiting on a COMMIT message from the coordinator.
		t.waitingOn.add(action.senderID);
	}
	
	// MIKE: added Action in order to pass in the PlaylistAction to the
	// endVoting method.
	private void countVote(Transaction transaction, Decide vote, Action action)
	{
		transaction.voteCount += 1;
		
		if (vote == Decide.Yes)
		{
			transaction.yesCount += 1;
		}
		
		// All participants have voted.
		if (transaction.voteCount == this.numProcesses - 1)
		{
			endVoting(transaction, action);
		}
	}
	
	private void endVoting(Transaction transaction, Action action)
	{
		// We are no longer waiting on any participants.
		transaction.waitingOn.clear();
		
		// If all participants voted YES, PRECOMMIT and send PRECOMMIT to all.
		if (transaction.yesCount == this.numProcesses - 1)
		{
			sendPrecommit(transaction);
			
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
					send(new Abort(transaction.id, this.id, i, action.playlistAction));
				}
			}
		}
		

	}
	
	private void sendPrecommit(Transaction t)
	{
		updateState(t.id, State.Committable);
		this.dtLog.log(new Precommit(t.id, this.id, this.id, "", t.playlistAction));
		for(int i = 0; i < this.numProcesses; i++)
		{
			if (i !=  this.id)
			{
				send(new Precommit(t.id, this.id, i, "", t.playlistAction));
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
		dtLog.log(new Yes(start3PC.transactionID, this.id, start3PC.senderID, "", start3PC.getParticipants(), start3PC.playlistAction));
		
		// Send YES to coordinator.
		send(new Yes(start3PC.transactionID, this.id, start3PC.senderID, "", start3PC.getParticipants(), start3PC.playlistAction));
		
		// Now uncertain and awaiting coordinator.
		updateState(start3PC.transactionID, State.Uncertain);
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
		send(new Abort(start3PC.transactionID, this.id, start3PC.senderID, start3PC.playlistAction));
	}
	
	private void processAck(Ack action, Transaction transaction)
	{
		transaction.acks.add(action.senderID);
		if (transaction.acks.size() == this.numProcesses - 1)
		{
			commit(transaction);
			sendCommit(transaction.id, transaction.acks, action.playlistAction);
		}
	}
	
	/**
	 * Sends COMMIT to the specified processes.
	 * @param transactionId 	ID of transaction for this COMMIT
	 * @param processes 		List of process IDs
	 */
	private void sendCommit(Integer transactionId, Collection<Integer> processes, PlaylistAction action)
	{
		for(Iterator<Integer> i = processes.iterator(); i.hasNext();)
		{
			send(new Commit(transactionId, this.id, i.next(), action));	
		}
	}
	
	/**
	 * Sends ABORT to the specified processes.
	 * @param transactionId 	ID of transaction for this COMMIT
	 * @param processes 		List of process IDs
	 */
	private void sendAbort(Transaction t, Collection<Integer> processes)
	{
		for(Iterator<Integer> i = processes.iterator(); i.hasNext();)
		{
			send(new Abort(t.id, this.id, i.next(), t.playlistAction));	
		}
	}
	
	private void sendDecisionRequest(Transaction t, Collection<Integer> processes)
	{
		for (Iterator<Integer> i = processes.iterator(); i.hasNext();)
		{
			send(new DecisionRequest(t.id, this.id, i.next(), t.playlistAction));
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
			// MIKE: PlaylistAction is null on a state request -- we have just
			// recovered and are asking others for help, they can tell us
			// the PlaylistAction.
			send(new StateRequest(transactionId, this.id, i.next(), null));	
		}
	}
	
	/**
	 * A process has just been elected coordinator and now sends a STATE-REQ to
	 * all LIVE processes.
	 */
	private void sendStateRequests(Transaction t)
	{
		Collection<Integer> live = this.monitor.getLive();
		for (Iterator<Integer> i = live.iterator(); i.hasNext();)
		{
			send(new StateRequest(t.id, this.id, i.next(), t.playlistAction));
		}
	}
	
	/**
	 * Responds to a STATE-REQ by an elected coordinator. Responds with either
	 * COMMIT, ABORT, COMMITTABLE, or UNCERTAIN.
	 */
	private void respondToStateRequest(StateRequest request, Transaction t)
	{
		if (t.committed)
		{
			send(new Commit(request.transactionID, this.id, request.senderID, t.playlistAction));
		}
		else if (t.state == State.Aborted)
		{
			send(new Abort(request.transactionID, this.id, request.senderID, t.playlistAction));
		}
		else if (t.state == State.Committable)
		{
			send(new Committable(request.transactionID, this.id, request.senderID, t.playlistAction));
		}
		else if (t.state == State.Uncertain)
		{
			send(new Uncertain(request.transactionID, this.id, request.senderID, t.playlistAction));
		}
	}
	
	/**
	 * Responds to a DECISION-REQUEST sent by a process that failed and recovered
	 * with either COMMIT or ABORT if either decision has been reached by this process.
	 * Otherwise, send no decision (the protocol is still in progress).
	 * @param request
	 * @param t
	 */
	private void respondToDecisionRequest(DecisionRequest request, Transaction t)
	{
		if (t.committed)
		{
			send(new Commit(request.transactionID, this.id, request.senderID, t.playlistAction));
		}
		if (t.aborted)
		{
			send(new Abort(request.transactionID, this.id, request.senderID, t.playlistAction));
		}
	}
	
	/**
	 * Decides COMMIT: writes to DT log and changes state.
	 * 
	 * @param transactionId Transaction being committed.
	 */
	private void commit(Transaction t)
	{
		if (!t.committed)
		{
			t.committed = true;
			dtLog.log(new Commit(t.id, this.id, this.id, t.playlistAction));
			
			
			// MIKE: start: write the edit/delete/add to the Playlist stable storage.
			ArrayList<String> testCmd = t.playlistAction.getCommand();
			
			try {
				this.playlistLog.log(testCmd, t.id);
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			// MIKE: end: write the edit/delete/add to the Playlist stable storage.
				
			System.out.println(t.id + ": COMMIT by process " + this.id);
			updateState(t.id, State.Committed);
			
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
			t.aborted = true;
			dtLog.log(new Abort(t.id, this.id, this.id, t.playlistAction));
			System.out.println(t.id + ": ABORT by process " + this.id);
			updateState(t.id, State.Aborted);
			
			// We are no longer waiting on anyone. We're done.
			t.waitingOn.clear();
		}
	}
	
	
	/**
	 * Prints this process' Playlist from stable storage.
	 */
	public void printPlaylist()
	{
		Playlist playlist = this.playlistLog.read();
		System.out.println("process " + id + "'s Playlist:");
		playlist.printPlaylist();
	}
}
