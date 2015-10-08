package action;

import java.io.Serializable;

import playlist.PlaylistAction;

/**
 * Abstraction for a STATE-REQ message in the 3PC protocol.
 *
 * Use Cases:
 * 
 * (1) A process is elected coordinator. It sends a STATE-REQ
 * to all processes as part of the termination protocol.
 */
public class StateRequest extends Action implements Serializable {

	private static final long serialVersionUID = 1L;

	public StateRequest(Integer transactionID, Integer senderID, Integer destinationID, PlaylistAction playlistAction)
	{
		super(transactionID, senderID, destinationID, playlistAction);
	}

	@Override
	public String toString() {
		return "StateRequest [senderID=" + senderID + ", destinationID=" + destinationID + ", transactionID="
				+ transactionID + "]";
	}
}
