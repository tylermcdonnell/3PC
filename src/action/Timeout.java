package action;

import java.io.Serializable;

/**
 * Abstraction for a basic TIMEOUT. Used in the 3PC protocol to reflect receipt
 * of the PRECOMMIT command for a process to the coordinator.
 *
 */
public class Timeout extends Action implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Override
	public String toString() {
		return "TIMEOUT: [process " + this.senderID + "]";
	}

	public Timeout(Integer transactionID, Integer senderID, Integer destinationID)
	{
		super(transactionID, senderID, destinationID);
	}

}