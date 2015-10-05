package action;

import java.io.Serializable;

/**
 * Abstraction for the COMMIT action used in the 3PC protocol. Can be sent
 * as a message to other processes and logged to stable storage.
 *
 */
public class Commit extends Action implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Message being committed
	 */
	private String message;
	
	@Override
	public String toString() {
		return "COMMIT: " + this.message;
	}

	public Commit(Integer transactionID, int senderID, String message)
	{
		super(transactionID, senderID);
		this.message = message;
	}

}
