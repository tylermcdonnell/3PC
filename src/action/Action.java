package action;

import java.io.Serializable;

public class Action implements Serializable {

	private static final long serialVersionUID = 1L;

	// Who is sending this action.
	public int senderID;
	
	// Each music action (add, edit, remove) has a transaction ID associated
	// with it.  Each message in the entire run of the 3PC protocol associated
	// with this music action will include this ID in all of the messages passed
	// back and forth.
	public Integer transactionID;
	
	
	public Action(Integer transactionID, Integer senderID)
	{
		this.transactionID = transactionID;
		this.senderID = senderID;
	}
}
