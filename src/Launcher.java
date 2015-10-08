import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

import playlist.PlaylistAction;
import framework.Config;
import framework.NetController;

public class Launcher {

	// Semaphore for using stdout in process code.
	public static Semaphore printSem = new Semaphore(1);
	
	// A list of all thread handles.
	public static ArrayList<Thread> threads = new ArrayList<Thread>();
	
	// A list of the process objects underlying the thread handles.
	public static ArrayList<Process3PC> processes = new ArrayList<Process3PC>();
	
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
	private static final String SLEEP_CMD = "s";
	private static final String USE_SCRIPT = "script";
	private static final String TPC = "3pc";
	private static final String PRINT_PLAYLISTS_CMD = "p";
	
	// Number of processes we choose to create for this execution.
	public static int numProcesses;
	
	public static void main(String args[]) throws Exception
	{
		/**********************************************
		 * You can use this to test 3PC configurations.
		 *********************************************/
		//test3PC(3);
		
		/**********************************************
		 * You can use this to test Keep Alive functionality.
		 *********************************************/
		//testKeepAlive(3);
		
		/**********************************************
		 * You can use this to test Playlist printing functionality.
		 *********************************************/
		testPlaylistPrint(4);
		
		System.out.println("Input commands to control 3PC flow:");
		
		Scanner scanner = new Scanner(System.in);
		
		while (true)
		{
			String input = scanner.nextLine();
			
			// Break up this command into tokens.
			String[] tokens 	= input.split(" ");
			String command 		= tokens[0];
			String[] parameters = Arrays.copyOfRange(tokens, 1, tokens.length);
			
			execute(command, parameters);
		}
	}
	
	private static void execute(String command, String[] parameters) throws InterruptedException
	{
		if (command.equals(ADD_CMD)) 
		{
			System.out.println("Adding <" + parameters[0] + ", " + parameters[1] + ">.");
		} 
		else if (command.equals(REMOVE_CMD)) 
		{
			System.out.println("Removing <" + parameters[0] + ">.");
		}
		else if (command.equals(EDIT_CMD)) 
		{
			System.out.println("Editting <" + parameters[0] + ", ?> to  <" + parameters[1] + ", " + parameters[2] + ">.");
		}
		else if (command.equals(CREATE_PROCESSES_CMD)) 
		{		
			System.out.println("Creating " + parameters[0] + " \"processes.\"");
			createProcesses(Integer.valueOf(parameters[0]));
		} 
		else if (command.equals(KILL_CMD))
		{	
			System.out.println("Killing process " + parameters[0] + ".");
			kill(Integer.valueOf(parameters[0]));
		} 
		else if (command.equals(KILL_ALL_CMD)) 
		{
			System.out.println("Killing all processes.");
			killAll();
		} 
		else if (command.equals(KILL_LEADER_CMD)) 
		{
			System.out.println("Killing the leader.");		
		} 
		else if (command.equals(REVIVE_CMD)) 
		{
			System.out.println("Reviving process " + parameters[0] + ".");
			revive(Integer.valueOf(parameters[0]));
		} 
		else if (command.equals(REVIVE_LAST_CMD)) 
		{		
			System.out.println("Reviving last killed process.");		
		} 
		else if (command.equals(REVIVE_ALL_CMD)) 
		{		
			System.out.println("Reviving all processes.");		
		} 
		else if (command.equals(PARTIAL_MESSAGE_CMD)) 
		{	
			Integer process = Integer.valueOf(parameters[0]);
			Integer messages = Integer.valueOf(parameters[1]);
			processes.get(process).haltAfter(messages);
			System.out.println("Process " + process + " will send " + messages + " messages then stop.");	
		} 
		else if (command.equals(RESUME_MESSAGES_CMD))
		{
			Integer process = Integer.valueOf(parameters[0]);
			processes.get(process).resumeMessages();
			System.out.println("Process " + process + " will resume sending messages.");	
		} 
		else if (command.equals(ALL_CLEAR_CMD)) 
		{	
			System.out.println("allClear was called.");	
		} 
		else if (command.equals(REJECT_NEXT_CHANGE_CMD)) 
		{
			System.out.println("Process " + parameters[0] + " will reject next change.");
		} 
		else if (command.equals(EXIT_CMD)) 
		{
			System.out.println("Exting after closing all net controllers.");
			
			for (int i = 0; i < netControllers.size(); i++) {
				netControllers.get(i).shutdown();
			}
			
			System.exit(-1);
		}
		else if (command.equals(SLEEP_CMD)) 
		{	
			System.out.print("Sleeping for " + parameters[0] + " seconds.");
			sleep(Integer.parseInt(parameters[0]));
		}
		else if (command.equals(USE_SCRIPT))
		{
			runScript(parameters[0]);
		}
		else if (command.equals(TPC))
		{
			// Command to help test 3PC until we get playlist commands up.
			ArrayList<String> action = new ArrayList<String>();
			action.add("Add");
			action.add("SongName");
			action.add("SongURL");
			PlaylistAction pa = new PlaylistAction(action);
			processes.get(0).start(0, pa);
		}
		else if (command.equals(PRINT_PLAYLISTS_CMD))
		{
			printPlaylists();
		}
		else {
			System.out.println("Unrecognized command. Closing all net controllers. Program terminating.");
			
			for (int i = 0; i < netControllers.size(); i++) 
			{
				netControllers.get(i).shutdown();
			}
	
			System.exit(-1);
		}
	}

	private static void runScript(String filename)
	{
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) 
		{
		    String line;
		    while ((line = br.readLine()) != null) 
		    {
		    	if (line.startsWith("/"))
		    	{
		    		continue;
		    	}
		    	
				String[] tokens 	= line.split(" ");
				String command 		= tokens[0];
				String[] parameters = Arrays.copyOfRange(tokens, 1, tokens.length);
				
				execute(command, parameters);
		    }
		}
		catch (Exception exc)
		{
			System.out.println("Error while running script.");
			exc.printStackTrace();
		}
	}
	
	
	/**
	 * Kill the thread with the given id.
	 * 
	 * @param id, the given id.
	 */
	private static void kill(Integer id)
	{
		threads.get(id).stop();
		
		// Spin-wait for process to become "TERMINATED."
		//while (!threads.get(id).getState().equals("TERMINATED")) {
		//}
		
		System.out.println("CONTROLLER: killed process " + id);
	}
	
	
	/**
	 * Kill all threads.
	 */
	private static void killAll()
	{	
		for (int i = 0; i < threads.size(); i++) {
			kill(i);
		}
	}
	
	
	/**
	 * Revive the thread with the given id.
	 * 
	 * @param id, the given id.
	 */
	private static void revive(Integer id)
	{
		// Note: use same NetController object as the previously killed thread.
		Process3PC r = new Process3PC(id, netControllers.get(id), numProcesses, false);
		
		Thread d = new Thread(r);
		d.start();
		
		threads.set(id, d);
		processes.set(id, r);
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
			Process3PC r = new Process3PC(i, nc, numProcesses, true);
			Thread d = new Thread(r);
			d.start();
			
			// Add to our list of threads.
			threads.add(d);
			
			// Add to our list of processes.
			processes.add(r);
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
	
	
	/**
	 * Sleep the main thread for the specified number of seconds.
	 * 
	 * @param numSeconds, the specified number of seconds.
	 * 
	 * @throws InterruptedException if interrupted while sleeping.
	 */
	private static void sleep(int numSeconds) throws InterruptedException {
		
		// Note: sleep takes an argument in milliseconds.
		Thread.sleep(numSeconds * 1000);
	}
	
	/**
	 * Prints all process' Playlists in a nice format.
	 */
	private static void printPlaylists() {
		
		// Print all Playlist logs to compare.
		System.out.println("\n");
		for (int i = 0; i < processes.size(); i++) 
		{
			processes.get(i).printPlaylist();
		}
	}
	
	
	/**********************************************************************
	 * TEST METHODS             
	 **********************************************************************/
	
	private static void test3PC(Integer numProcesses) throws InterruptedException
	{
		createProcesses(numProcesses);
		
		ArrayList<String> action = new ArrayList<String>();
		action.add("Add");
		action.add("SongName");
		action.add("SongURL");
		PlaylistAction pa = new PlaylistAction(action);
		processes.get(0).start(0, pa);
	}
	
	private static void testPlaylistPrint(Integer numProcesses) throws InterruptedException
	{
		createProcesses(numProcesses);
		
		ArrayList<String> action0 = new ArrayList<String>();
		action0.add("Add");
		action0.add("Steal My Girl");
		action0.add("www.youtube.com/SMG");
		PlaylistAction pa0 = new PlaylistAction(action0);
		processes.get(0).start(0, pa0);
		
		// Wait for commits to be done.
		Thread.sleep(3000);
		printPlaylists();
		
		ArrayList<String> action1 = new ArrayList<String>();
		action1.add("Add");
		action1.add("Where Do Broken Hearts Go?");
		action1.add("www.youtube.com/WDBHG");
		PlaylistAction pa1 = new PlaylistAction(action1);
		processes.get(1).start(1, pa1);
		
		// Wait for commits to be done.
		Thread.sleep(2000);
		printPlaylists();
		
		ArrayList<String> action2 = new ArrayList<String>();
		action2.add("Edit");
		action2.add("Steal My Girl");
		action2.add("Steal My Girl (EDITED)");
		action2.add("www.youtube.com/SMG (EDITED)");
		PlaylistAction pa2 = new PlaylistAction(action2);
		processes.get(3).start(4, pa2);
		
		// Wait for commits to be done.
		Thread.sleep(2000);
		printPlaylists();
		
		ArrayList<String> action3 = new ArrayList<String>();
		action3.add("Delete");
		action3.add("Steal My Girl (EDITED)");
		PlaylistAction pa3 = new PlaylistAction(action3);
		processes.get(3).start(134, pa3);
		
		// Wait for commits to be done.
		Thread.sleep(2000);
		printPlaylists();
		
		ArrayList<String> action4 = new ArrayList<String>();
		action4.add("Delete");
		action4.add("Where Do Broken Hearts Go?");
		PlaylistAction pa4 = new PlaylistAction(action4);
		processes.get(2).start(6, pa4);
		
		// Wait for commits to be done.
		Thread.sleep(2000);
		printPlaylists();
		
		System.out.println("\n\n$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
		System.out.println("PLAYLIST SHOULD BE EMPTY NOW");
		System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
	}

	private static void testKeepAlive(Integer numProcesses) throws InterruptedException
	{
		createProcesses(numProcesses);
		Thread.sleep(3000);
		threads.get(0).stop();
		Thread.sleep(1000);
		revive(0);
	}
}


