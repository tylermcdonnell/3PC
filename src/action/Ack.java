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
	
	/**
	 * Target action of 3PC transaction.
	 */
	private String message;
	
	@Override
	public String toString() {
		return "ACK: " + this.message;
	}

	public Ack(Integer transactionID, Integer senderID, Integer destinationID, String message, PlaylistAction playlistAction)
	{
		super(transactionID, senderID, destinationID, playlistAction);
		this.message = message;
	}

}