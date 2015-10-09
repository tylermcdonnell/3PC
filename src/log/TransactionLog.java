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
			
			primeLog();
		}
		else
		{
			readFromDisk();
		}
	}
	
	
	/**
	 * Upon creating a new File on disk, it must be written to by the
	 * process who created it before the process may be killed and recover
	 * information from the log. Therefore, write something to this file
	 * and then delete what was written (write again), in order to
	 * "prime" the file.
	 */
	private void primeLog()
	{
		saveToDisk();
	}
	
	/**
	 * Returns the current transaction log.
	 * @return
	 */
	public synchronized ArrayList<Action> read()
	{
		return getLogCopy();
	}
	
	private ArrayList<Action> getLogCopy()
	{
		synchronized(this.log)
		{
			ArrayList<Action> logCopy = new ArrayList<Action>(this.log.size());
			for(Action item: this.log)
			{
				logCopy.add(item);
			}
			return logCopy;
		}
	}
	
	/**
	 * Log a new action to stable storage.
	 * @param action to be logged
	 * @return true if successful
	 */
	public boolean log(Action action)
	{
		synchronized(this.log)
		{
			this.log.add(action);
		}
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
			synchronized(this.log)
			{
				objectOut.writeObject(this.log);
			}
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
			synchronized(this.log)
			{
				this.log = (ArrayList<Action>)objectIn.readObject();	
			}
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
