package action;

import java.io.Serializable;

/**
 * Abstraction for a basic ACK. Used in the 3PC protocol to reflect receipt
 * of the PRECOMMIT command for a process to the coordinator.
 *
 */
public class Ack extends Action implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Target action of 3PC transaction.
	 */
	private String message;
	
	@Override
	public String toString() {
		return "ACK: " + this.message;
	}

	public Ack(Integer transactionID, Integer senderID, Integer destinationID, String message)
	{
		super(transactionID, senderID, destinationID);
		this.message = message;
	}

}