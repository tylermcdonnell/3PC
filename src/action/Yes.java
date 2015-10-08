package action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Abstraction for the YES message used in the 3PC protocol.
 * 
 * Use Cases:
 * 
 * (1) A process sends YES as its vote in response to a START3PC
 * (i.e., VOTE-REQ) from the coordinator.
 * 
 * (2) A process logs YES as its vote after receiving a START3PC
 * (i.e., VOTE-REQ) from the coordinator.
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
	public Yes(Integer transactionID, Integer senderID, Integer destinationID, String message, Collection<Integer> participants)
	{
		super(transactionID, senderID, destinationID);
		this.participants = new ArrayList<Integer>(participants);
	}
	
	@Override
	public String toString() {
		return "Yes [participants=" + participants + ", message=" + message + ", senderID=" + senderID
				+ ", destinationID=" + destinationID + ", transactionID=" + transactionID + "]";
	}

	public Collection<Integer> getParticipants()
	{
		return this.participants;
	}
}
