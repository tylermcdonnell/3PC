package action;

import java.io.Serializable;

public class KeepAlive extends Action implements Serializable {

	private static final long serialVersionUID = 1L;

	public KeepAlive(Integer transID, Integer senderID, Integer destinationID) 
	{
		super(transID, senderID, destinationID);
	}

}
