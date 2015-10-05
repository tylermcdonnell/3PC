package action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Abstraction for a YES vote in the 3PC protocol. Is logged to stable storage
 * prior to being sent to other processes.
 *
 */
public class Yes extends Action implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * All participants in 3PC transaction.
	 */
	private ArrayList<Integer> participants;
	
	/**
	 * Message being voted on
	 */
	private String message;
	
	/**
	 * Constructor
	 * @param participants processes involved in this 3PC transaction
	 */
	public Yes(Integer transactionID, Integer senderID, Collection<Integer> participants)
	{
		super(transactionID, senderID);
		this.participants = new ArrayList<Integer>(participants);
	}
	
	@Override
	public String toString() {
		return "Yes: " + this.message + " [participants=" + participants + "]";
	}
	
	public Collection<Integer> getParticipants()
	{
		return this.participants;
	}
}
