import java.util.Hashtable;

import action.Action;
import action.Start3PC;

public class CommitHandler {

	/**
	 * Possible 3PC states.
	 */
	private enum State 
	{
		ABORTED, UNCERTAIN, COMMITTABLE, COMMITTED
	}
	
	/**
	 * State for all active transactions.
	 */
	private Hashtable<Integer, State> transactions;
	
	/**
	 * Public handler for incoming actions.
	 * @param action
	 */
	public void handle(Action action)
	{
		State transactionState = transactions.get(action.id);
		if(transactionState == null)
		{
			// Create a new transaction
		}
		
		switch (transactionState)
		{
			case ABORTED:
				if (action instanceof Start3PC)
				{
					vote();
				}
			case UNCERTAIN:
				break;
			case COMMITTABLE:
				break;
			case COMMITTED:
				break;
		}
	}
	
	private void vote()
	{
		
	}
}
