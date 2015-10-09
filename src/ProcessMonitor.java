import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import action.KeepAlive;
import framework.NetController;

/**
 * Abstraction for monitoring the life of other processes in a 
 * distributed system via consistent keep-alive messages.
 */
public class ProcessMonitor {
	
	class ProcessStatus
	{
		boolean live;
		long lastReceived;
		
		public ProcessStatus(boolean live, long lastReceived)
		{
			this.live 			= live;
			this.lastReceived 	= lastReceived;
		}
	}
	
	private NetController network;
	
	// TODO
	// MIKE: Processes that have not failed yet in the protocol.
	private ArrayList<Integer> nonFailedProcesses;
	
	// ID of this process.
	private Integer processId;
	
	// Number of processes in network.
	private Integer numProcesses;
	
	// Live status of all processes being monitored.
	private ArrayList<ProcessStatus> statuses;
	 
	// Will consider a process dead if this monitor has not received
	// a Keep-Alive from it in timeout milliseconds
	private Long timeout;
	
	 // Will not send a keep-alive to all processes more often than
	 // every interval milliseconds.
	private Long interval;
	
	// System time when this monitor last sent Keep-Alive messages.
	private ArrayList<Long> lastSent;
	
	/**
	 * 
	 * @param network	network to communicate with other processes
	 * @param timeout	monitor will consider a process dead if it 
	 * 					has not received a keep-alive from him in 
	 * 					timeout milliseconds
	 * @param interval 	monitor will send keep-alive no more often 
	 * 					than every interval milliseconds
	 */
	public ProcessMonitor(Integer processId, Integer numProcesses, NetController network, long timeout, long interval)
	{
		// Initialize all fields.
		this.processId 		= processId;
		this.numProcesses 	= numProcesses;
		this.network 		= network;
		this.timeout 		= timeout;
		this.interval 		= interval;
		this.lastSent 		= new ArrayList<Long>();
		this.statuses 		= new ArrayList<ProcessStatus>();
		Long time = System.currentTimeMillis();
		for(int i = 0; i < this.numProcesses; i++)
		{
			this.statuses.add(new ProcessStatus(true, time));
			this.lastSent.add(time);
		}
		
		// At the start of monitoring, assume all are alive. Once we
		// find a process has died, take it's ID away from this list
		// until the protocol terminates.
		this.nonFailedProcesses = new ArrayList<Integer>();
		for (int i = 0; i < this.numProcesses; i++)
		{
			this.nonFailedProcesses.add(i);
		}
	}
	
	public void setInterval(long interval)
	{
		this.interval = interval;
	}
	
	public void setTimeout(long timeout)
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
		// Process Keep-Alive list.
		for(Iterator<KeepAlive> i = keepAlives.iterator(); i.hasNext();)
		{
			KeepAlive ka = i.next();
			ProcessStatus senderStatus = this.statuses.get(ka.senderID);
			if (senderStatus.live == false)
			{
				System.out.println("Process " + this.processId + " believes process " + ka.senderID + " just came back to life.");
			}
			senderStatus.live = true;
			senderStatus.lastReceived = System.currentTimeMillis();
			i.remove();
		}
		
		// Send Keep-Alive (if applicable) and update status for every process.
		for(int i = 0; i < numProcesses; i++)
		{
			if (System.currentTimeMillis() - lastSent.get(i) > this.interval)
			{
				// MIKE: For keep alive messages, the PlaylistAction is not required, set it to
				// null.
				this.network.sendMsg(i, new KeepAlive(0, this.processId, i, null));
			}
			
			if (System.currentTimeMillis() - this.statuses.get(i).lastReceived > this.timeout)
			{
				if (this.statuses.get(i).live)
				{
					System.out.println("Process " + this.processId + " believes process " + i + " is dead.");
				}
				this.statuses.get(i).live = false;
				
				// Take this process ID off our list of non-failed
				// processes forever.
				for(Iterator<Integer> it = this.nonFailedProcesses.iterator(); it.hasNext();)
				{
					Integer processId = it.next();
					if (processId == i)
					{
						it.remove();
					}
				}
				// TYLER: This removes an index, not the object, which will cause problems.
				//this.nonFailedProcesses.remove(i);
			}
		}
		
		return getDead();
	}
	
	/**
	 * @return a list of processes currently considered to be live:
	 * 		   i.e., we have received a keep-alive from them.
	 */
	public Collection<Integer> getLive()
	{
		ArrayList<Integer> live = new ArrayList<Integer>();
		for(int i = 0; i < numProcesses; i++)
		{
			if (this.statuses.get(i).live == true)
			{
				live.add(i);
			}
		}
		return live;
	}
	
	/**
	 * @return  returns a list of processes currently considered 
	 * 			to be crashed: i.e., we have not received a keep-
	 * 			alive from them in more than timeout milliseconds.
	 */
	public Collection<Integer> getDead()
	{
		ArrayList<Integer> dead = new ArrayList<Integer>();
		for(int i = 0; i < numProcesses; i++)
		{
			if (this.statuses.get(i).live == false)
			{
				dead.add(i);
			}
		}
		return dead;
	}
	
	
	/**
	 * @return the list of processes who have not failed during this run
	 * of the protocol.
	 */
	public ArrayList<Integer> getNonFailedProcesses()
	{
		return this.nonFailedProcesses;
	}
	
	/**
	 * It will take this long after instantiation for values to stabilize.
	 */
	public long getStartupDelay()
	{
		return this.timeout * 2;
	}
}
