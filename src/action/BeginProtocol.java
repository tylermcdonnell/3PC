package action;

import java.io.Serializable;

import playlist.PlaylistAction;

/**
 * This class does not correspond to an action in the 3PC protocol.
 * It is used by the controller to coordinate new transactions.
 */
public class BeginProtocol extends Action implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public BeginProtocol(Integer transactionID, Integer senderID, Integer destinationID, PlaylistAction playlistAction) 
	{
		super(transactionID, senderID, destinationID, playlistAction);
	}

	@Override
	public String toString() {
		return "BeginProtocol [senderID=" + senderID + ", destinationID=" + destinationID + ", transactionID="
				+ transactionID + ", playlistAction = " + this.playlistAction.toString() + "]";
	}
	

}
