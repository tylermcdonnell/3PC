package action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import playlist.PlaylistAction;

/**
 * Abstraction for the START3PC and VOTE-REQ messages used in the 3PC protocol. If 
 * logged to stable storage, it is a START3PC, but it may be sent to other processes
 * in which case it behaves as VOTE-REQ. (Notice they are equivalent).
 *
 */
public class Start3PC extends Action implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * All participants in 3PC transaction.
	 */
	private ArrayList<Integer> participants;
	
	/**
	 * Message being voted on
	 */
	private String message;
	
	public Start3PC(Integer transactionID, Integer senderID, Integer destinationID, String message, Collection<Integer> participants, PlaylistAction playlistAction)
	{
		super(transactionID, senderID, destinationID, playlistAction);
		this.participants = new ArrayList<Integer>(participants);
		this.message = message;
	}
	
	@Override
	public String toString() {
		return "Start3PC [participants=" + participants + ", message=" + message + ", senderID=" + senderID
				+ ", destinationID=" + destinationID + ", transactionID=" + transactionID + ", playlistAction="
				+ playlistAction + "]";
	}

	public Collection<Integer> getParticipants()
	{
		return this.participants;
	}
}
