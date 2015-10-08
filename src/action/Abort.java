package action;

import java.io.Serializable;

import playlist.PlaylistAction;

/**
 * Abstraction for an ABORT message in the 3PC protocol.
 * 
 * Use Cases:
 * 
 * (1) A process decides no and logs ABORT to its DT log.
 * 
 * (2) A process sends its vote NO (i.e., ABORT) to the coordinator
 * in response to a START3PC (i.e., VOTE-REQ).
 * 
 * (3) A process receives a STATE-REQ from an elected coordinator
 * and has previously decided ABORT. 
 * 
 * (4) A process knows the decision is ABORT and sends ABORT
 * in response to a DEC-REQ (i.e., a dead process just came 
 * up and is querying everyone to see if anyone knows the 
 * decision).
 */
public class Abort extends Action implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Message being aborted
	 */
	private String message;
	
	@Override
	public String toString() {
		return "ABORT: " + this.message;
	}

	/**
	 * Default constructor.
	 */
	public Abort(Integer transactionID, Integer senderID, Integer destinationID, String message, PlaylistAction playlistAction)
	{
		super(transactionID, senderID, destinationID, playlistAction);
		this.message = message;
	}
}
