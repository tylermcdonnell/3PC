package action;

import java.io.Serializable;

import playlist.PlaylistAction;

public class KeepAlive extends Action implements Serializable {

	private static final long serialVersionUID = 1L;

	public KeepAlive(Integer transID, Integer senderID, Integer destinationID, PlaylistAction playlistAction) 
	{
		super(transID, senderID, destinationID, playlistAction);
	}

}
