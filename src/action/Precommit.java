package action;

import java.io.Serializable;

/**
 * Abstraction for a PRECOMMIT message in the 3PC protocol.
 *
 * Use Cases:
 * 
 * (1) The coordinator sends PRECOMMIT to notify processes
 * that it has received YES votes from all processes.
 * 
 * (2) The coordinator and/or participant upon receipt of 
 * PRECOMMIT will log a PRECOMMIT to its DT log. Note that
 * though the classical 3PC protocol says this is optional,
 * it is actually necessary. (See below if interested).
 * 
 * ------------- Why you MUST log PRECOMMIT -------------
 * 
 * Suppose processes do not log PRECOMMIT. Suppose all 
 * processes vote YES and the coordinator sends out PRECOMMITs
 * to all processes. Suppose that the coordinator then 
 * receives ACKs from every process. It decides COMMIT and
 * logs COMMIT to stable storage. Then, it dies. Then, all
 * participants die immediately after, and they all come back
 * up. The participants can initiate an election protocol with
 * one another and, since no PRECOMMITs were logged, they are
 * all UNCERTAIN (the last action they see in the DT log is
 * COMMIT). Their termination protocol will thus decide ABORT.
 * Then, the coordinator comes back up, and he has already 
 * decided COMMIT. Uh-oh.
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
	public Precommit(Integer transactionID, Integer senderID, Integer destinationID, String message)
	{
		super(transactionID, senderID, destinationID);
		this.message = message;
	}

	@Override
	public String toString() {
		return "PRECOMMIT: " + this.message;
	}
}
