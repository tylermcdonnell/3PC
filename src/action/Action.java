package action;

import java.io.Serializable;

import playlist.PlaylistAction;

public class Action implements Serializable {

	@Override
	public String toString() {
		return "Action [senderID=" + senderID + ", destinationID=" + destinationID + ", transactionID=" + transactionID
				+ ", playlistAction=" + playlistAction + "]";
	}


	private static final long serialVersionUID = 1L;

	// Who is sending this action.
	public int senderID;
	
	// Who is the destination for this action.
	public int destinationID;
	
	// Each music action (add, edit, remove) has a transaction ID associated
	// with it.  Each message in the entire run of the 3PC protocol associated
	// with this music action will include this ID in all of the messages passed
	// back and forth.
	public Integer transactionID;
	
	// Each action will contain the playlist action associated with this round
	// of the protocol.
	public PlaylistAction playlistAction;
	
	
	public Action(Integer transactionID, Integer senderID, Integer destinationID, PlaylistAction playlistAction)
	{
		this.transactionID = transactionID;
		this.senderID = senderID;
		this.destinationID = destinationID;
		this.playlistAction = playlistAction;
	}
}
