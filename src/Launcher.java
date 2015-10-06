import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import framework.Config;
import framework.NetController;

public class Launcher {

	// A list of all thread handles.
	public static ArrayList<Thread> threads = new ArrayList<Thread>();
	
	// A list of all net controllers (corresponding to the same element in
	// the threads list).
	public static ArrayList<NetController> netControllers = new ArrayList<NetController>();
	
	//  Configure commands.
	private static final String ADD_CMD = "add";
	private static final String REMOVE_CMD = "remove";
	private static final String EDIT_CMD = "edit";
	private static final String CREATE_PROCESSES_CMD = "cp";
	private static final String KILL_CMD = "kill";
	private static final String KILL_ALL_CMD = "killAll";
	private static final String KILL_LEADER_CMD = "killLeader";
	private static final String REVIVE_CMD = "revive";
	private static final String REVIVE_LAST_CMD = "reviveLast";
	private static final String REVIVE_ALL_CMD = "reviveAll";
	private static final String PARTIAL_MESSAGE_CMD = "pm";
	private static final String RESUME_MESSAGES_CMD = "rm";
	private static final String ALL_CLEAR_CMD = "allClear";
	private static final String REJECT_NEXT_CHANGE_CMD = "rejectNextChange";
	private static final String EXIT_CMD = "e";
	
	// Number of processes we choose to create for this execution.
	public static int numProcesses;
	
	public static void main(String args[]) throws Exception
	{
		System.out.println("Input commands to control 3PC flow:");
		
		Scanner scanner = new Scanner(System.in);
		
		while (true)
		{
			String command = scanner.nextLine();
			
			// Break up this command into tokens.
			String[] tokens = command.split(" ");
			String firstToken = tokens[0];
			
			if (firstToken.equals(ADD_CMD)) {
				
				System.out.println("Adding <" + tokens[1] + ", " + tokens[2] + ">.");
				
			} else if (firstToken.equals(REMOVE_CMD)) {
				
				System.out.println("Removing <" + tokens[1] + ">.");
			
			} else if (firstToken.equals(EDIT_CMD)) {
				
				System.out.println("Editting <" + tokens[1] + ", ?> to  <" + tokens[2] + ", " + tokens[3] + ">.");
				
			} else if (firstToken.equals(CREATE_PROCESSES_CMD)) {
				
				System.out.println("Creating " + tokens[1] + " \"processes.\"");
				createProcesses(Integer.valueOf(tokens[1]));
				
			} else if (firstToken.equals(KILL_CMD)) {
				
				// Note: kill should spin-wait on the status of the thread to be
				// terminated
				
				System.out.println("Killing process " + tokens[1] + ".");
				
			} else if (firstToken.equals(KILL_ALL_CMD)) {
				
				// This would be a total failure? this isn't allowed?
				
				System.out.println("Killing all processes.");
				
			} else if (firstToken.equals(KILL_LEADER_CMD)) {
				
				System.out.println("Killing the leader.");
				
			} else if (firstToken.equals(REVIVE_CMD)) {
				
				System.out.println("Reviving process " + tokens[1] + ".");
				
			} else if (firstToken.equals(REVIVE_LAST_CMD)) {
				
				System.out.println("Reviving last killed process.");
				
			} else if (firstToken.equals(REVIVE_ALL_CMD)) {
				
				System.out.println("Reviving all processes.");
				
			} else if (firstToken.equals(PARTIAL_MESSAGE_CMD)) {
				
				System.out.println("Process " + tokens[1] + " will send " + tokens[2] + " messages then stop.");
				
			} else if (firstToken.equals(RESUME_MESSAGES_CMD)) {
				
				System.out.println("Process " + tokens[1] + " will resume sending messages.");
				
			} else if (firstToken.equals(ALL_CLEAR_CMD)) {
				
				System.out.println("allClear was called.");
				
			} else if (firstToken.equals(REJECT_NEXT_CHANGE_CMD)) {
				
				System.out.println("Process " + tokens[1] + " will reject next change.");
				
			} else if (firstToken.equals(EXIT_CMD)) {
				
				System.out.println("Exting after closing all net controllers.");
				
				for (int i = 0; i < netControllers.size(); i++) {
					netControllers.get(i).shutdown();
				}
				
				System.exit(-1);
			}
			
			else {
				
				System.out.println("Unrecognized command. Closing all net controllers. Program terminating.");
				
				for (int i = 0; i < netControllers.size(); i++) {
					netControllers.get(i).shutdown();
				}
				
				System.exit(-1);
			}
		}
	}
	
	
	/**
	 * Creates the specified number of "processes" (threads). For each
	 * thread created, add its handle to a list.
	 * 
	 * @param numProcesses, the specified number of "processes" to create.
	 */
	private static void createProcesses(int numProcesses) {
		
		// Store the number of processes to a global variable.
		Launcher.numProcesses = numProcesses;
		
		for (int i = 0; i < numProcesses; i++) {
			
			NetController nc = createNetController(i);
			
			// Pass in "i" as the process number for this process.
			Runnable r = new Process(i, nc);
			Thread d = new Thread(r);
			d.start();
			
			// Add to our list of threads.
			threads.add(d);
		}
	}
	
	
	/**
	 * Creates a NetController for the given process, described by its process
	 * number.
	 * 
	 * @param processNumber, this process' process number.
	 * 
	 * @return a NetController for the given process, described by its process
	 * number.
	 */
	private static NetController createNetController(int processNumber) {
		
		//**********************************************************************
		//* Setup communication for this process with all other processes.
		//**********************************************************************
		NetController nc = null;
		
		// Dynamically create a config file for this process.
		// Reuse the same file for all processes.
		String fileName = "config.txt";
		File file = new File(fileName);
		
		PrintWriter out = null;
		try {
			out = new PrintWriter(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		// NumProcesses field.
		out.println("NumProcesses=" + Launcher.numProcesses);
		
		// ProcNum field.
		out.println("ProcNum=" + processNumber);
		
		// host fields.
		for (int i = 0; i < Launcher.numProcesses; i++) {
			out.println("host" + i + "=localhost");
		}
		
		// port fields.
		for (int i = 0; i < Launcher.numProcesses; i++) {
			out.println("port" + i + "=" + (6100 + i));
		}
		
		out.flush();
		out.close();
		
		try {
			
			Config config = new Config(fileName);
			nc = new NetController(config);
			
		} catch (Exception e) {	
			e.printStackTrace();
		}
		
		// Add this to the list of net controllers in the entire system.
		netControllers.add(nc);
		
		return nc;
	}
}


