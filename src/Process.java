import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import action.*;
import framework.NetController;
import log.TransactionLog;

public class Process implements Runnable {

	/**
	 * Possible 3PC roles.
	 * Coordinator: Process "coordinating" the 3PC protocol.
	 * Participant: Process voting in 3PC protocol.
	 *
	 */
	private enum Role
	{
		Coordinator, Participant
	}
	
	/**
	 * Possible 3PC states.
	 * Aborted: 	The process has not voted, has voted NO, or has received an ABORT.
	 * Uncertain:	The process has voted YES but not received a PRECOMMIT or ABORT.
	 * Committable:	The process has received PRECOMMIT, but has not received COMMIT.
	 * Committed:	The process has received and decided to COMMIT. 
	 */
	private enum State 
	{
		Aborted, Uncertain, Committable, Committed
	}
	
	/**
	 * The state kept for each 3PC transaction.
	 */
	private class Transaction
	{
		Role role;
		State state;
		
		Transaction(Role role, State state)
		{
			this.role = role;
			this.state = state;
		}
	}
	
	/**
	 * Action + target for a message to be sent to another process.
	 */
	private class Message
	{
		Integer destinationId;
		Action action;
		
		Message(Integer destinationId, Action action)
		{
			this.destinationId = destinationId;
			this.action = action;
		}
	}
	
	/**
	 * State for all active transactions.
	 */
	private Hashtable<Integer, Transaction> transactions;
	
	/**
	 * Possible vote decisions.
	 */
	private enum Decide
	{
		Yes, No
	}
	
	/**
	 * An outgoing queue of PROTOCOL messages. This is used to support the testing command
	 * partialMessage. During the core part of the main processing loop, we only enqueue 
	 * messages in the outgoing queue. At the end of every processing loop, we send the
	 * messages that have been enqueued over the socket. 
	 * 
	 * Consider the following concrete scenario: 
	 * Three processes: coordinator p(0) and participants and p(1) and p(2).
	 * partialMessage(0,1) tells the coordinator to halt after sending one message to p(1).
	 * We might then choose to kill p(2) and resume execution so that the message to p(2) is
	 * not delivered. With the protocol queue, both of these outgoing messages would be 
	 * enqueued, but only the first would be sent before HALT. Later on, when RESUME is
	 * sent, the message to p(2) is still first in the queue.
	 */
	private LinkedList<Message> protocolSendQueue;
	
	/**
	 * Buffered queue of received protocol messages (i.e., keep-alives have been filtered)
	 */
	private LinkedList<Message> protocolRecvQueue;
	
	/**
	 * Buffered list of received keep-alive messages to report to monitor.
	 */
	private LinkedList<KeepAlive> recvKeepAlive;
	
	/**
	 * This can be used to alter the next vote decision. YES by default.
	 */
	Decide nextDecision = Decide.Yes;
	
	/**
	 * Stable storage
	 */
	private TransactionLog dtLog;
	
	/**
	 * This process' ID.
	 */
	private Integer id = 0;
	
	private ProcessMonitor monitor;
	
	private NetController network;
	
	/**
	 * Control variable used to halt PROTOCOL progress in main loop.
	 * Note: does not affect heartbeat monitor.
	 */
	private boolean halted;

	public Process(Integer id, NetController network)
	{
		this.id = id;
		this.network = network;
		//this.dtLog = new TransactionLog(true, logName);
	}
	
	/**
	 * This is the "life" of the process. i.e., this is the main processing loop.
	 */
	public void run()
	{
		while(true)
		{
			// TODO: Receive all messages over network and filter them into
			// 		 appropriate queues.
			
			Collection<Integer> deadProcesses = monitor.monitor(recvKeepAlive);
			
			if(!halted)
			{
				// TODO: Process received message queue
				
				// TODO: Report dead processes as timeouts for all transactions.
			}
		}
	}
	
	/**
	 * Public handler for incoming actions.
	 * @param action
	 */
	public void handle(Integer sender, Action action)
	{
		Transaction transaction = transactions.get(action.transactionID);
		if(transaction == null)
		{
			createTransaction(action.transactionID, Role.Participant, State.Aborted);
		}
		
		if (action instanceof Start3PC)
		{
			vote((Start3PC)action);
		}
	}
	
	/**
	 * Creates a new 3PC transaction.
	 * @param transactionId	ID of transaction
	 * @param initialRole	Coordinator or Participant
	 * @param initialState	Aborted, Uncertain, Committable, or Committed
	 */
	private void createTransaction(Integer transactionId, Role initialRole, State initialState)
	{
		try
		{
			this.transactions.put(transactionId, new Transaction(initialRole, initialState));
		}
		catch (Exception ex)
		{
			// This really shouldn't ever happen. Maybe just fail here.
			throw ex;
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
			// This should never happen. Maybe just fail here.
			throw ex;
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
			// This should never happen. Maybe just fail here.
			throw ex;
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
	
	private void voteYes(Start3PC start3PC)
	{
		dtLog.log(new Yes(start3PC.transactionID, this.id, "", start3PC.getParticipants()));
		
		// TODO: Send YES to coordinator
	}
	
	private void voteNo(Start3PC start3PC)
	{
		abort(start3PC.transactionID);
		
		// TODO: Send NO to coordinator
	}
	
	private void abort(Integer transactionId)
	{
		dtLog.log(new Abort(id, this.id, ""));
	}
}
