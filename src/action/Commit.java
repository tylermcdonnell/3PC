package action;

import java.io.Serializable;

import playlist.PlaylistAction;

/**
 * Abstraction for an COMMIT message in the 3PC protocol.
 *
 * Use Cases:
 * 
 * (1) A process is either the coordinator and decides COMMIT
 * or receives a COMMIT message from the coordinator and logs
 * it to the DT log.
 * 
 * (2) A process knows the decision is COMMIT and sends COMMIT
 * in response to a DEC-REQ (i.e., a dead process just came 
 * up and is querying everyone to see if anyone knows the 
 * decision).
 */
public class Commit extends Action implements Serializable {

	private static final long serialVersionUID = 1L;

	public Commit(Integer transactionID, Integer senderID, Integer destinationID, String message, PlaylistAction playlistAction)
	{
		super(transactionID, senderID, destinationID, playlistAction);
	}

	@Override
	public String toString() {
		return "Commit [senderID=" + senderID + ", destinationID=" + destinationID + ", transactionID=" + transactionID
				+ ", playlistAction=" + playlistAction + "]";
	}
}
