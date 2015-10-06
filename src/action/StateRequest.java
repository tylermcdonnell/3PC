package action;

import java.io.Serializable;

/**
 * Abstraction for the STATE-REQ action used in the 3PC protocol. Used
 * to request the current state of a process.
 *
 */
public class StateRequest extends Action implements Serializable {

	private static final long serialVersionUID = 1L;
	
	// Target action of 3PC transaction.
	private String message;
	
	// Integer representation of enumeration. 
	// Note: I'm using this because otherwise, I need to find somewhere
	// 	     else to move enumeration definition.
	private Integer state;
	private Integer role;
	
	@Override
	public String toString() {
		return "COMMIT: " + this.message;
	}

	public StateRequest(Integer transactionID, Integer senderID, Integer destinationID, Integer role, Integer state, String message)
	{
		super(transactionID, senderID, destinationID);
		this.role 		= role;
		this.state 		= state;
		this.message 	= message;
	}
	
	public Integer getState()
	{
		return this.state;
	}
	
	public Integer getRole()
	{
		return this.role;
	}

}
