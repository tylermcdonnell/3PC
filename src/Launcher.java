import java.util.ArrayList;
import java.util.Scanner;

public class Launcher {

	// A list of all thread handles.
	public static ArrayList<Thread> threads = new ArrayList<Thread>();
	
	
	public static void main(String args[]) throws Exception
	{
		// TODO: Mike
		// Parse config file (which will be in 3PC/config.txt) and
		// then loop while file hasNextLine() to execute all
		// commands.
		
		System.out.println("Input commands to control 3PC flow:");
		
		Scanner scanner = new Scanner(System.in);
		
		while(true)
		{
			String command = scanner.nextLine();
			
			// Break up this command into tokens.
			String[] tokens = command.split(" ");
			String firstToken = tokens[0];
			
			if (firstToken.equals("add")) {
				
				System.out.println("Adding <" + tokens[1] + ", " + tokens[2] + ">.");
				
			} else if (firstToken.equals("remove")) {
				
				System.out.println("Removing <" + tokens[1] + ">.");
			
			} else if (firstToken.equals("edit")) {
				
				System.out.println("Editting <" + tokens[1] + ", ?> to  <" + tokens[2] + ", " + tokens[3] + ">.");
				
			} else if (firstToken.equals("createProcesses")) {
				
				System.out.println("Creating " + tokens[1] + " \"processes.\"");
				createProcesses(tokens[1]);
				
			} else if (firstToken.equals("kill")) {
				
				System.out.println("Killing process " + tokens[1] + ".");
				
			} else if (firstToken.equals("killAll")) {
				
				System.out.println("Killing all processes.");
				
			} else if (firstToken.equals("killLeader")) {
				
				System.out.println("Killing the leader.");
				
			} else if (firstToken.equals("revive")) {
				
				System.out.println("Reviving process " + tokens[1] + ".");
				
			} else if (firstToken.equals("reviveLast")) {
				
				System.out.println("Reviving last killed process.");
				
			} else if (firstToken.equals("reviveAll")) {
				
				System.out.println("Reviving all processes.");
				
			} else if (firstToken.equals("partialMessage")) {
				
				System.out.println("Process " + tokens[1] + " will send " + tokens[2] + " messages then stop.");
				
			} else if (firstToken.equals("resumeMessages")) {
				
				System.out.println("Process " + tokens[1] + " will resume sending messages.");
				
			} else if (firstToken.equals("allClear")) {
				
				System.out.println("allClear was called.");
				
			} else if (firstToken.equals("rejectNextChange")) {
				
				System.out.println("Process " + tokens[1] + " will reject next change.");
				
			} else {
				
				System.out.println("Unrecognized command. Program terminating.");
				System.exit(-1);
			}
		}
	}
	
	private static void createProcesses(String numProcesses) {
		
		int numProcs = Integer.valueOf(numProcesses);
		
		for (int i = 0; i < numProcs; i++) {
			
			Runnable r = new MikeProcess(String.valueOf(i));
			Thread d = new Thread(r);
			d.start();
			
			threads.add(d);
		}
	}
}


