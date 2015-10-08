package action;

import java.io.Serializable;

import playlist.PlaylistAction;

/**
 * Message-passing construct to initiate the 3PC protocol.
 * 
 * This is NOT part of the 3PC protocol. The CONTROLLER merely
 * uses this to tell processes that they "want" to initiate
 * the 3PC protocol for some playlist-related functionality.
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
