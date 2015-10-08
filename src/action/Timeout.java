package action;

import java.io.Serializable;

/**
 * Abstraction for the UP set in the 3PC protocol.
 * 
 * Use Cases:
 * 
 * (1) The UP set is written to stable storage periodically. We
 * choose to write it every time the set is altered.
 */
public class Timeout extends Action implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public Timeout(Integer transactionID, Integer senderID, Integer destinationID)
	{
		super(transactionID, senderID, destinationID);
	}

	@Override
	public String toString() {
		return "Timeout [senderID=" + senderID + ", destinationID=" + destinationID + ", transactionID=" + transactionID
				+ "]";
	}

}