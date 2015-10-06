package protocol;
/**
 * Container class for 3PC types, etc.
 */
public class ThreePC 
{
	// Possible 3PC roles.
	// Coordinator: Process "coordinating" the 3PC protocol.
	// Participant: Process voting in 3PC protocol.
	public enum Role
	{
		Coordinator, Participant;
	}
	
	// Possible 3PC states.
	// Aborted: 	The process has not voted, has voted NO, or has received an ABORT.
	// Uncertain:	The process has voted YES but not received a PRECOMMIT or ABORT.
	// Committable:	The process has received PRECOMMIT, but has not received COMMIT.
	// Committed:	The process has received and decided to COMMIT. 
	public enum State 
	{
		Aborted, Uncertain, Committable, Committed
	}
}
