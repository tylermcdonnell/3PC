package log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;

import playlist.Playlist;

/**
 * Stable storage abstraction for the 3PC protocol. Provides utilities for 
 * logging a Playlist (must be Serializable) to disk.
 *
 */
public class PlaylistLog {
	
	/**
	 * Location of Playlist on disk.
	 */
	private File file;
	
	/**
	 * Memory copy of Playlist.
	 */
	private Playlist playlist;
	
	/**
	 * Default constructor.
	 * 
	 * @param reset, true to reset the Playlist on disk
	 * @param filename, where the Playlist is stored
	 */
	public PlaylistLog(boolean reset, String filename)
	{
		this.playlist = new Playlist();
		this.file = new File(filename);
		
		if (reset)
		{
			try
			{
				Files.deleteIfExists(this.file.toPath());
			}
			catch (Exception e)
			{
				System.out.println("Failed to delete previous Playlist "
						+ "file from disk. Should never happen.");
				e.printStackTrace();
			}
		}
		else
		{
			readFromDisk();
		}
	}

	/**
	 * Returns the current playlist.
	 * 
	 * @return the current playlist.
	 */
	public Playlist read()
	{
		return this.playlist;
	}
	
	/**
	 * Log a new Playlist to stable storage.
	 * 
	 * @param playlistCommand, an ArrayList of Strings that describe a
	 * modification to the Playlist.
	 * @param transID, the transaction associated with this modification.
	 * 
	 * @return true if successful
	 */
	public boolean log(ArrayList<String> playlistCommand, int transID) throws Exception
	{
		System.out.println("transID: " + transID + ", logging new Playlist to disk!");
		
		// Delete the old File (we want to write a fresh copy of the
		// Playlist entirely.
		try
		{
			Files.deleteIfExists(this.file.toPath());
		}
		catch (Exception e)
		{
			System.out.println("Failed to delete previous Playlist "
					+ "file from disk. Should never happen.");
			e.printStackTrace();
		}
		
		// Add, Edit, or Delete.
		String cmd = playlistCommand.get(0);
		
		if (cmd.equals("Add"))
		{
			this.playlist.add(playlistCommand.get(1), playlistCommand.get(2), transID);
		}
		else if (cmd.equals("Edit"))
		{
			this.playlist.edit(playlistCommand.get(1), playlistCommand.get(2), playlistCommand.get(3), transID);
		}
		else if (cmd.equals("Delete"))
		{
			this.playlist.remove(playlistCommand.get(1), transID);
		}
		else
		{
			System.out.println("Invalid operating being done to Playlist. Terminating.");
			System.exit(-1);
		}
		
		return saveToDisk();
	}
	
	/**
	 * Write Playlist to disk.
	 * 
	 * @return true if successful
	 */
	private boolean saveToDisk()
	{
		try
		{
			FileOutputStream streamOut = new FileOutputStream(this.file);
			ObjectOutputStream objectOut = new ObjectOutputStream(streamOut);
			objectOut.writeObject(this.playlist);
			objectOut.close();
			streamOut.close();
			return true;
		}
		catch (Exception e)
		{
			System.out.println("Exception while writing Playlist to disk: ");
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Read Playlist from disk.
	 * 
	 * @return true if successful
	 */
	private boolean readFromDisk()
	{
		try
		{
			FileInputStream streamIn = new FileInputStream(this.file);
			ObjectInputStream objectIn = new ObjectInputStream(streamIn);
			this.playlist = (Playlist)objectIn.readObject();
			objectIn.close();
			streamIn.close();
			return true;
		}
		catch (Exception e)
		{
			System.out.println("Exception while reading Playlist from disk: ");
			e.printStackTrace();
			return false;
		}
	}
}
