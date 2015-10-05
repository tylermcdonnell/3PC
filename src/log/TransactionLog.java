package log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import action.Action;

/**
 * Stable storage abstraction for the 3PC protocol. Provides utilities for 
 * logging any Action to disk that implements the Serializable interface.
 *
 */
public class TransactionLog {
	
	/**
	 * Location of log on disk.
	 */
	private File file;
	
	/**
	 * Memory copy of log.
	 */
	private ArrayList<Action> log;
	
	/**
	 * Default constructor.
	 * @param reset		true to reset log on disk
	 * @param filename	where the log is stored
	 */
	public TransactionLog(boolean reset, String filename)
	{
		this.log = new ArrayList<Action>();
		this.file = new File(filename);
		
		if(reset)
		{
			try
			{
				Files.deleteIfExists(this.file.toPath());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			readFromDisk();
		}
	}

	/**
	 * Returns the current transaction log.
	 * @return
	 */
	public ArrayList<Action> read()
	{
		return this.log;
	}
	
	/**
	 * Log a new action to stable storage.
	 * @param action to be logged
	 * @return true if successful
	 */
	public boolean log(Action action)
	{
		log.add(action);
		return saveToDisk();
	}
	
	/**
	 * Writes log to disk.
	 * @return true if successful
	 */
	private boolean saveToDisk()
	{
		try
		{
			FileOutputStream streamOut 	 = new FileOutputStream(this.file);
			ObjectOutputStream objectOut = new ObjectOutputStream(streamOut);
			objectOut.writeObject(this.log);
			objectOut.close();
			streamOut.close();
			return true;
		}
		catch (Exception e)
		{
			System.out.println("Exception while writing log to disk: ");
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Reads log from disk
	 * @return true if successful
	 */
	@SuppressWarnings("unchecked")
	private boolean readFromDisk()
	{
		try
		{
			FileInputStream streamIn 	= new FileInputStream(this.file);
			ObjectInputStream objectIn 	= new ObjectInputStream(streamIn);
			this.log 					= (ArrayList<Action>)objectIn.readObject();
			objectIn.close();
			streamIn.close();
			return true;
		}
		catch (Exception e)
		{
			System.out.println("Exception while reading log from disk: ");
			e.printStackTrace();
			return false;
		}
	}
}
