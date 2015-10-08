package action;

import java.io.Serializable;

import playlist.PlaylistAction;
import protocol.ThreePC;

/**
 * Abstraction for the STATE-REQ action used in the 3PC protocol. Is used
 * to request the current state of a process.
 *
 */
public class StateResponse extends Action implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private ThreePC.State state;
	
	private ThreePC.Role role;
	
	// Target action of 3PC transaction.
	private String message;
	
	@Override
	public String toString() {
		return "COMMIT: " + this.message;
	}

	public StateResponse(Integer transactionID, Integer senderID, Integer destinationID, ThreePC.State state, ThreePC.Role role, String message, PlaylistAction playlistAction)
	{
		super(transactionID, senderID, destinationID, playlistAction);
		this.state 		= state;
		this.role 		= role;
		this.message 	= message;
	}
	
	public ThreePC.State getState()
	{
		return this.state;
	}
	
	public ThreePC.Role getRole()
	{
		return this.role;
	}
}
