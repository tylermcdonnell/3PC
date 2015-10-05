package action;

import java.io.Serializable;

/**
 * Abstraction for the PRECOMMIT message used in the 3PC protocol.
 *
 */
public class Precommit extends Action implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Message being voted on
	 */
	private String message;
	
	/**
	 * Default constructor
	 */
	public Precommit(Integer transactionID, Integer senderID, String message)
	{
		super(transactionID, senderID);
		this.message = message;
	}

	@Override
	public String toString() {
		return "PRECOMMIT: " + this.message;
	}
}
