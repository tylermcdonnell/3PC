import java.util.Collection;
import java.util.Hashtable;
import action.*;
import log.TransactionLog;

public class Process {

	private enum Identity
	{
		IsCoordinator, IsParticipant
	}
	
	/**
	 * Possible 3PC states.
	 */
	private enum State 
	{
		Aborted, Uncertain, Committable, Committed
	}
	
	/**
	 * Stable storage
	 */
	private TransactionLog dtLog;
	
	/**
	 * State for all active transactions.
	 */
	private Hashtable<Integer, State> transactions;
	
	/**
	 * Next vote decision. YES by default.
	 */
	private boolean voteYes = true;
	
	public Process(String logName)
	{
		dtLog = new TransactionLog(true, logName);
	}
	
	/**
	 * Public handler for incoming actions.
	 * @param action
	 */
	public void handle(Integer sender, Action action)
	{
		State state = transactions.get(action.id);
		if(state == null)
		{
			state = setState(action.id, State.Aborted);
		}
		
		if (action instanceof Start3PC)
		{
			vote((Start3PC)action);
		}
	}
	
	/**
	 * Creates or updates a transaction to be in defined state.
	 * @param id			id of process you want to set
	 * @param initialState	desired state
	 */
	private State setState(Integer id, State state)
	{
		try
		{
			this.transactions.put(id, state);
			return state;
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
	 */
	private void vote(Start3PC start3PC)
	{ 
		if (voteYes)
		{
			voteYes(start3PC);
		}
		else
		{
			voteNo(start3PC);
		}
		
		// Default should be YES.
		voteYes = true;
	}
	
	private void voteYes(Start3PC start3PC)
	{
		// Log YES
		dtLog.log(new Yes(start3PC.id, "", start3PC.getParticipants()));
		
		// Send YES to coordinator
	}
	
	private void voteNo(Start3PC start3PC)
	{
		// Log ABORT
		abort(start3PC.id);
		
		// Send NO to coordinator
	}
	
	private void abort(Integer id)
	{
		dtLog.log(new Abort(id, ""));
	}
}
