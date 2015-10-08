package action;

import java.io.Serializable;

/**
 * Abstraction for an UNCERTAIN message in the 3PC protocol.
 * 
 * Use Cases:
 * 
 * (1) A process sends UNCERTAIN in response to an elected
 * coordinator's STATE-REQ if the process believes the last
 * action it took was logging a YES vote and never received
 * either PRECOMMIT or ABORT.
 */
public class Uncertain extends Action implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public Uncertain(Integer transactionID, Integer senderID, Integer destinationID)
	{
		super(transactionID, senderID, destinationID);
	}

	@Override
	public String toString() {
		return "Uncertain [senderID=" + senderID + ", destinationID=" + destinationID + ", transactionID="
				+ transactionID + "]";
	}
}