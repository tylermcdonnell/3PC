package action;

import java.io.Serializable;

/**
 * Abstraction for ABORT and NO in the 3PC protocol. When logged to stable storage,
 * this is an ABORT. We can also use this action to mean a NO vote when sent to
 * other processes. (Notice they are equivalent).
 *
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
	public Abort(Integer transactionID, Integer senderID, Integer destinationID, String message)
	{
		super(transactionID, senderID, destinationID);
		this.message = message;
	}
}
