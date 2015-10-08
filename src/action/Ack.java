package action;

import java.io.Serializable;

import playlist.PlaylistAction;

/**
 * Abstraction for an ACK message in the 3PC protocol.
 *
 * Use Cases:
 * 
 * (1) A process sends an ACK in response to a PRECOMMIT
 * message from the coordinator.
 */
public class Ack extends Action implements Serializable {

	private static final long serialVersionUID = 1L;

	public Ack(Integer transactionID, Integer senderID, Integer destinationID, PlaylistAction playlistAction)
	{
		super(transactionID, senderID, destinationID, playlistAction);
	}

	@Override
	public String toString() {
		return "Ack [senderID=" + senderID + ", destinationID=" + destinationID + ", transactionID=" + transactionID
				+ ", playlistAction=" + playlistAction + "]";
	}
}