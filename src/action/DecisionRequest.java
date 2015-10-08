package action;

import java.io.Serializable;
import playlist.PlaylistAction;

/**
 * Abstraction for a DEC-REQ message in the 3PC protocol.
 *
 * Use Cases:
 * 
 * (1) A process dies and comes back up. It can ask other
 * processes if any of them has reached a DECISION.
 */
public class DecisionRequest extends Action implements Serializable {

	private static final long serialVersionUID = 1L;

	public DecisionRequest(Integer transactionID, Integer senderID, Integer destinationID, PlaylistAction playlistAction)
	{
		super(transactionID, senderID, destinationID, playlistAction);
	}

	@Override
	public String toString() {
		return "DecisionRequest [senderID=" + senderID + ", destinationID=" + destinationID + ", transactionID="
				+ transactionID + ", playlistAction=" + playlistAction + "]";
	}
}
