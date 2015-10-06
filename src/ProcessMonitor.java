import java.util.ArrayList;
import java.util.Collection;

import action.KeepAlive;
import framework.NetController;

/**
 * Abstraction for monitoring the life of other processes in a 
 * distributed system via consistent keep-alive messages.
 */
public class ProcessMonitor {
	
	private NetController network;
	
	private Integer processId;
	
	/**
	 * Number of processes in network.
	 */
	private Integer numProcesses;
	
	/** 
	 * Will consider a process dead if this heartbeat monitor has
	 * not received a keep-alive from him in timeout milliseconds
	 */
	private Integer timeout;
	
	/**
	 * Will not send a keep-alive to all processes more often than
	 * every interval milliseconds.
	 */
	private Integer interval;
	
	/**
	 * 
	 * @param network	network to communicate with other processes
	 * @param timeout	monitor will consider a process dead if it 
	 * 					has not received a keep-alive from him in 
	 * 					timeout milliseconds
	 * @param interval 	monitor will send keep-alive no more often 
	 * 					than every interval milliseconds
	 */
	public ProcessMonitor(Integer processId, Integer numProcesses, NetController network, Integer timeout, Integer interval)
	{
		this.processId 		= processId;
		this.numProcesses 	= numProcesses;
		this.network 		= network;
		this.timeout 		= timeout;
		this.interval 		= interval;
	}
	
	public void setInterval(Integer interval)
	{
		this.interval = interval;
	}
	
	public void setTimeout(Integer timeout)
	{
		this.timeout = timeout;
	}
	
	/**
	 * Processes provided keep alive messages (to determine life
	 * of other processes) and sends a keep-alive message, if 
	 * allowable by interval to all other processes. Returns a 
	 * list of currently "dead" processes.
	 * 
	 * Note: This method takes an input list of keep alive messages
	 * already received by the parent process. This monitor NEVER
	 * reads from the network. It only uses the network to send.
	 * 
	 * @param keepAlives	received keep alive messages
	 * @return				list of "dead" processes
	 */
	public Collection<Integer> monitor(Collection<KeepAlive> keepAlives)
	{
		for(int i = 0; i < numProcesses; i++)
		{
			// TODO: Send Keep-Alive to process i
			
			// TODO: Evaluate live status of process i
		}
		
		return new ArrayList<Integer>();
	}
	
	/**
	 * @return  returns a list of processes currently considered 
	 * 			to be crashed: i.e., we have not received a keep-
	 * 			alive from them in more than timeout milliseconds.
	 */
	public Collection<Integer> getCrashed()
	{
		//TODO: Implement
		return new ArrayList<Integer>();
	}
}
