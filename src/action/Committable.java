package action;

import java.io.Serializable;

import playlist.PlaylistAction;

/**
 * Abstraction for a COMMITTABLE message in the 3PC protocol.
 * 
 * Use Cases:
 * 
 * (1) A process sends COMMITTABLE in response to an elected
 * coordinator's STATE-REQ if the process received a PRECOMMIT
 * from the previous coordinator but has not received a COMMIT
 * or ABORT.
 */
public class Committable extends Action implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public Committable(Integer transactionID, Integer senderID, Integer destinationID, PlaylistAction playlistAction)
	{
		super(transactionID, senderID, destinationID, playlistAction);
	}

	@Override
	public String toString() {
		return "Committable [senderID=" + senderID + ", destinationID=" + destinationID + ", transactionID="
				+ transactionID + "]";
	}
}