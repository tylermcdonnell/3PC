package action;

import java.io.Serializable;

/**
 * Abstraction for an UP log that can be written to stable storage.
 * Rather than containing a set, this simply writes a single value
 * that, taken mod N represents the UP set at any given time.
 */
public class UP extends Action implements Serializable 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Message being aborted
	 */
	private Integer UP;
	
	/**
	 * Default constructor.
	 */
	public UP(Integer transactionID, Integer senderID, Integer destinationID, Integer UP)
	{
		super(transactionID, senderID, destinationID);
		this.UP = UP;
	}
	
	/**
	 * Returns current UP "set".
	 * @return
	 */
	public Integer getUP()
	{
		return this.UP;
	}

	@Override
	public String toString() {
		return "UP [UP=" + UP + ", senderID=" + senderID + ", destinationID=" + destinationID + ", transactionID="
				+ transactionID + "]";
	}
}
