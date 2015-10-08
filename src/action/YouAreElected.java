package action;

import java.io.Serializable;

/**
 *  * Abstraction for the YOU-ARE-ELECTED message used in the 3PC protocol.
 * 
 * Use Cases:
 * 
 * (1) A process has detected that the current coordinator (or
 * who it believes to be the current coordinator) is dead. The 
 * process will send a YOU-ARE-ELECTED message to the process
 * determined according to the leader election protocol in the UP
 * set to notify it that it is now the coordinator.
 */
public class YouAreElected extends Action implements Serializable 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public YouAreElected(Integer transactionID, Integer senderID, Integer destinationID)
	{
		super(transactionID, senderID, destinationID);
	}

	@Override
	public String toString() {
		return "YouAreElected [senderID=" + senderID + ", destinationID=" + destinationID + ", transactionID="
				+ transactionID + "]";
	}
}
