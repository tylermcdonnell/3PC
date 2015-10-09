/**
 * This code may be modified and used for non-commercial 
 * purposes as long as attribution is maintained.
 * 
 * @author: Isaac Levy
 */

/**
* The sendMsg method has been modified by Navid Yaghmazadeh to fix a bug regarding to send a message to a reconnected socket.
*/

package framework;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;

import action.Action;

/**
 * Public interface for managing network connections.
 * You should only need to use this and the Config class.
 * @author ilevy
 *
 */
public class NetController {
	private final Config config;
	private final List<IncomingSock> inSockets;
	private final OutgoingSock[] outSockets;
	private final ListenServer listener;
	
	public NetController(Config config) {
		this.config = config;
		inSockets = Collections.synchronizedList(new ArrayList<IncomingSock>());
		listener = new ListenServer(config, inSockets);
		outSockets = new OutgoingSock[config.numProcesses];
		listener.start();
	}
	
	// Establish outgoing connection to a process
	private synchronized void initOutgoingConn(int proc) throws IOException {
		
		// MIKE: Make sure this is only called once per outgoing connection.
		if (outSockets[proc] != null)
			throw new IllegalStateException("proc " + proc + " not null");
		
		// MIKE: config.addresses[proc] can just be localhost.
		// MIKE: pulls the port for this new connection from config.ports array.
		outSockets[proc] = new OutgoingSock(new Socket(config.addresses[proc], config.ports[proc]));
		//config.logger.info(String.format("Server %d: Socket to %d established", 
		//		config.procNum, proc));
	}
	
	/**
	 * Send a msg to another process.  This will establish a socket if one is not created yet.
	 * Will fail if recipient has not set up their own NetController (and its associated serverSocket)
	 * @param process int specified in the config file - 0 based
	 * @param msg Do not use the "&" character.  This is hardcoded as a message separator. 
	 *            Sends as ASCII.  Include the sending server ID in the message
	 * @return bool indicating success
	 */
	public synchronized boolean sendMsg(int process, String msg) {
		try {
			if (outSockets[process] == null)
				initOutgoingConn(process);
			outSockets[process].sendMsg(msg);
		} catch (IOException e) { 
			if (outSockets[process] != null) {
				outSockets[process].cleanShutdown();
				outSockets[process] = null;
				try{
					initOutgoingConn(process);
                        		outSockets[process].sendMsg(msg);	
				} catch(IOException e1){
					if (outSockets[process] != null) {
						outSockets[process].cleanShutdown();
	                	outSockets[process] = null;
					}
					config.logger.info(String.format("Server %d: Msg to %d failed.",
                        config.procNum, process));
        		    config.logger.log(Level.FINE, String.format("Server %d: Socket to %d error",
                        config.procNum, process), e);
                    return false;
				}
				return true;
			}
			config.logger.info(String.format("Server %d: Msg to %d failed.", 
				config.procNum, process));
			config.logger.log(Level.FINE, String.format("Server %d: Socket to %d error", 
				config.procNum, process), e);
			return false;
		}
		return true;
	}
	
	private static String toString(Serializable o) throws IOException 
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(o);
		oos.close();
		return Base64.getEncoder().encodeToString(out.toByteArray());
	}
	
	public boolean sendMsg(int process, Action msg)
	{
		try
		{
			return sendMsg(process, toString((Serializable)msg));
		}
		catch (Exception exc)
		{
			System.out.println("ERROR: IOException while sending message.");
			return false;
		}
	}
	
	/**
	 * Return a list of msgs received on established incoming sockets
	 * @return list of messages sorted by socket, in FIFO order. *not sorted by time received*
	 */
	public synchronized List<String> getReceivedMsgs() {
		List<String> objs = new ArrayList<String>();
		synchronized(inSockets) {
			ListIterator<IncomingSock> iter  = inSockets.listIterator();
			while (iter.hasNext()) {
				IncomingSock curSock = iter.next();
				try {
					objs.addAll(curSock.getMsgs());
				} catch (Exception e) {
					config.logger.log(Level.INFO, 
							"Server " + config.procNum + " received bad data on a socket", e);
					curSock.cleanShutdown();
					iter.remove();
				}
			}
		}
		
		return objs;
	}
	
	private static Object fromString(String s) throws IOException, ClassNotFoundException 
	{
		byte[] data = Base64.getDecoder().decode(s);
		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
		Object o = in.readObject();
		in.close();
		return o;
	}
	
	public List<Action> getReceived() 
	{
		List<String> msgs = getReceivedMsgs();
		List<Action> received = new ArrayList<Action>();
		for(Iterator<String> i = msgs.iterator(); i.hasNext();)
		{
			try
			{
				Object r = fromString(i.next());
				if (r instanceof Action)
				{
					received.add((Action)r);
				}
			}
			catch(IOException exc)
			{
				System.out.println("ERROR: I/O while receiving message.");
				exc.printStackTrace();
			}
			catch(ClassNotFoundException exc)
			{
				System.out.println("ERROR: Class not found while receiving message.");
			}
		}
		return received;
	}
	
	/**
	 * Shuts down threads and sockets.
	 */
	public synchronized void shutdown() {
		listener.cleanShutdown();
        if(inSockets != null) {
		    for (IncomingSock sock : inSockets)
			    if(sock != null)
                    sock.cleanShutdown();
        }
		if(outSockets != null) {
            for (OutgoingSock sock : outSockets)
			    if(sock != null)
                    sock.cleanShutdown();
        }
		
	}

}
