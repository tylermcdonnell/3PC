
public class MikeProcess implements Runnable {
	
	
	// This process' ID.
	private long processID;
	
	public MikeProcess(Object args) {
	}
	
	public void run() {
		
		this.processID = Thread.currentThread().getId();
		
		System.out.println("Process " + this.processID + " is up!");
	
		// Wait for message from coord.
		// How to handle timeout?
	}
}
