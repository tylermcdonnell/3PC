import java.util.Scanner;

public class Launcher {

	public static void main(String args[])
	{
		// TODO: Mike
		// Parse config file (which will be in 3PC/config.txt) and
		// then loop while file hasNextLine() to execute all
		// commands.
		
		System.out.println("Hello world!");
		
		Scanner scanner = new Scanner(System.in);
		while(true)
		{
			String command = scanner.next();
			System.out.println(command);
		}
	}
}


