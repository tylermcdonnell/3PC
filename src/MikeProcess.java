import java.util.ArrayList;

import framework.NetController;

/**
 * Each "process" will continuously run code in the "run" method to implement
 * 3PC.
 * 
 * @author Mike Feilbach
 * 
 */
public class MikeProcess implements Runnable {
	
	// This process' number, as assigned in the code -- 0 for the first
	// created thread, 1 for the second created thread, etc.
	private int processNumber;
	
	// This process' ID, as assigned by the scheduler -- this may mean
	// that the first created process is 11, the second created process
	// is 12, etc.
	private long processID;
	
	// This process' net controller.
	private NetController nc;
	
	/**
	 * Constructor.
	 * 
	 * @param processNumber, the assigned number for this "process."
	 */
	public MikeProcess(int processNumber, NetController nc) {
		
		this.processNumber = processNumber;
		this.nc = nc;
	}
	
	/**
	 * Each "process" will run this code for the span of its life.
	 */
	public void run() {
		
		// Store this process' ID.
		this.processID = Thread.currentThread().getId();
		System.out.println("Process " + this.processID + " is up, processNumber: " + this.processNumber);
		
		
		// Testing. This can be used to kill threads.
		if (Thread.interrupted()) {
	        System.out.println("INTERRUPTED");
	        return;
	    }
		
		// Send a message from p0 to p1.
		if (this.processNumber == 0) {
			nc.sendMsg(1, "HI THERE from p_0");
		} else if (this.processNumber == 1) {
			nc.sendMsg(0,  "HI THERE FROM p_1");
		}
		
		
		// Wait for a message.
		ArrayList<String> messages = new ArrayList<String>();
			
		while (messages.size() == 0) {
			messages = (ArrayList<String>)nc.getReceivedMsgs();
		}
			
		 // Got a message.
		for (int i = 0; i < messages.size(); i++) {
			System.out.println("Message: " + messages.get(i));
		}
		
		nc.shutdown();
	}
}
