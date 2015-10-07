package action;

import java.io.Serializable;

/**
 * This class does not correspond to an action in the 3PC protocol.
 * It is used by the controller to coordinate new transactions.
 */
public class BeginProtocol extends Action implements Serializable {

	public BeginProtocol(Integer transactionID, Integer senderID, Integer destinationID) 
	{
		super(transactionID, senderID, destinationID);
	}

	@Override
	public String toString() {
		return "BeginProtocol [senderID=" + senderID + ", destinationID=" + destinationID + ", transactionID="
				+ transactionID + "]";
	}
	

}
