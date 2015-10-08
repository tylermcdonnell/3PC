package playlist;

import java.io.Serializable;
import java.util.ArrayList;


/**
 * Wrapper class for an action on the Playlist (e.g., add, delete, or edit).
 * This encapsulates the action that must be done to a process' Playlist upon
 * a commit decision, and can be sent along in messages between processes.
 * 
 * @author Mike Feilbach
 *
 */
public class PlaylistAction implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	ArrayList<String> command;
	
	public PlaylistAction(ArrayList<String> command)
	{
		this.command = command;
	}
	
	public ArrayList<String> getCommand()
	{
		return this.command;
	}
	
	@Override
	public String toString()
	{
		if (this.command.get(0).equals("Add"))
		{
			return "Add <" + command.get(1) + ", " + command.get(2) + ">";
		}
		else if (this.command.get(0).equals("Edit"))
		{
			return "Edit <" + command.get(1) + ", ?> --> " + "<" + command.get(2) + ", " + command.get(3) + ">";
		}
		else if (this.command.get(0).equals("Delete"))
		{
			return "Delete <" + command.get(1) + ">";
		}
		else
		{
			System.out.println("Invalid operation in this PlaylistAction "
					+ "when calling toString(). Terminating.");
			System.exit(-1);
		}
		
		return null;
	}

}
