package action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Abstraction for the START3PC and VOTE-REQ messages used in the 3PC protocol. If 
 * logged to stable storage, it is a START3PC, but it may be sent to other processes
 * in which case it behaves as VOTE-REQ. (Notice they are equivalent).
 *
 */
public class Start3PC extends Action implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * All participants in 3PC transaction.
	 */
	private ArrayList<Integer> participants;
	
	/**
	 * Message being voted on
	 */
	private String message;
	
	public Start3PC(String message, Collection<Integer> participants)
	{
		this.participants = new ArrayList<Integer>(participants);
		this.message = message;
	}

	@Override
	public String toString() {
		return "START3PC: " + message + " [participants=" + participants + "]";
	}
	
	public Collection<Integer> getParticipants()
	{
		return this.participants;
	}
}
