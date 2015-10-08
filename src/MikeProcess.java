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
	
	private boolean isCoordinator;
	
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
		
		if (processNumber == 0) {
			this.isCoordinator = true;
		} else {
			this.isCoordinator = false;
		}
	}
	
	/**
	 * Each "process" will run this code for the span of its life.
	 */
	public void run() {
		
		// Store this process' ID.
		this.processID = Thread.currentThread().getId();
		println("Process " + this.processID + " is up, processNumber: " + this.processNumber);
		
		//**********************************************************************
		//* Step 1: if coordinator, send VOTE-REQs to all participants.
		//*         if participant, receive one VOTE-REQ.
		//**********************************************************************
		if (this.isCoordinator) {
			
			// Wait for other threads to be created (coordinator is created first).
			sleep(1);
			
			println(this.processNumber + ": I am coord, sending VOTE-REQs to all participants");
			sendMessageToAllOthers("VOTE-REQ");
			
		} else {
			
			// If participant, receive a single VOTE-REQ message.
			ArrayList<String> messages = waitForNumMessages(1);
			
			println(this.processNumber + ": received: " + messages.get(0));
		}
		
		//**********************************************************************
		//* Step 2: if participant, send YES or NO vote to coordinator.
		//*         if coordinator, receive votes from all participants.
		//**********************************************************************
		boolean allVotesYes = false;
		
		if (this.isCoordinator) {
			
			// Wait for messages from all participants.
			ArrayList<String> messages = waitForNumMessages(Launcher.numProcesses - 1);
			System.out.println("Received all votes from parts");
			
			// Check on the votes -- see if they're all "YES."
			for (int i = 0; i < messages.size(); i++) {
				
				if (messages.get(i).equals("NO")) {
					allVotesYes = false;
				}
			}
		} else {
			
			// If participant, send vote (send YES for now).
			nc.sendMsg(0, "YES");
			println("Sent YES from p " + this.processNumber);
		}
		
		//***********************************************************************
		//* Step 3: if coordinator, if all votes are yes, send PRE-COMMMITS.
		//*         if participant and voted yes, wait for PRE-COMMIT.
		//**********************************************************************
		if (this.isCoordinator) {
			
			// Coordinator.
			if (allVotesYes) {
				
				println(this.processNumber + ": I am coord, sending PRE-COMMITs to all parts");
				for (int i = 0; i < Launcher.numProcesses; i++) {
					if (i == this.processNumber) {
						// Do not send message to yourself.
					} else {
						nc.sendMsg(i, "PRE-COMMIT");
					}
				}
			}
			
			
		} else {
			// Participant code.
		}
		
		if (this.isCoordinator) {
			println("Coord done");
		} else {
			println("Part done");
		}
		
		
		nc.shutdown();
		
		/*
		// Send a message from p0 to p1.
		if (this.processNumber == 0) {
			nc.sendMsg(1, "HI THERE from p_0");
		} else if (this.processNumber == 1) {
			nc.sendMsg(0,  "HI THERE FROM p_1");
		}
		
		
		// Wait for a message.
		
			
		
			
		 // Got a message.
		for (int i = 0; i < messages.size(); i++) {
			println("Message: " + messages.get(i));
		}
		*/
		
		//nc.shutdown();
		
		/*
		// Testing. This can be used to kill threads.
		if (Thread.interrupted()) {
	       println("INTERRUPTED");
	        return;
	    }
	    */
	}
	
	/**
	 * Sends a message to all other processes in the system.
	 * 
	 * @param s, the message to send.
	 */
	private void sendMessageToAllOthers(String s) {
		
		for (int i = 0; i < Launcher.numProcesses; i++) {
			if (i == this.processNumber) {
				
			} else {
				nc.sendMsg(i, s);
			}
		}
	}
	
	/**
	 * Wait for the specified number of messages to come in. When they do,
	 * return the messages. NOTE: this function is blocking, and will wait
	 * indefinitely for the correct number of messages to come in.
	 * 
	 * 
	 * @param numMessages, the specified number of messages.
	 * 
	 * @return the received messages.
	 */
	private ArrayList<String> waitForNumMessages(int numMessages) {
		
		ArrayList<String> messages = new ArrayList<String>();
		ArrayList<String> receivedMsgs = new ArrayList<String>();
		
		while (true) {
			
			// See how many messages are waiting right now.
			messages = (ArrayList<String>)nc.getReceivedMsgs();
			
			// Exhaust the messages that were received into the
			// return list.
			for (int i = 0; i < messages.size(); i++) {
				
				receivedMsgs.add(messages.get(i));
			}
			
			// If we've gotten all the messages we need, exit
			// this loop and return the messages
			if (receivedMsgs.size() == numMessages) {
				break;
			}
		}
		
		return receivedMsgs;
	}
	
	
	/**
	 * Implementation of System.out.println with mutual exclusion (only one
	 * thread can be printing at once, and will finish printing its contents
	 * before another thread can acquire the lock.
	 * 
	 * @param s, the String to print.
	 */
	private static void println(String s) {
		
		try {
			Launcher.printSem.acquire();
		} catch (InterruptedException e) {
			System.out.println("Could not acquire print semaphore:");
			e.printStackTrace();
		}
		
		System.out.println(s);
		
		Launcher.printSem.release();
	}
	
	/**
	 * Sleep for the given number of seconds.
	 * 
	 * @param numSeconds, the given number of seconds.
	 */
	private void sleep(int numSeconds) {
		
		try {
			Thread.sleep(numSeconds * 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
