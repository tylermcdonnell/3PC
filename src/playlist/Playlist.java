package playlist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Each process in the network will have a local copy of a playlist (i.e.,
 * an instance of this class).  Upon a successful commit, each process
 * in the network will update their playlist.
 * 
 * @author Mike Feilbach
 *
 */
public class Playlist implements Serializable {

	// Necessary for implementing Serializable correctly.
	private static final long serialVersionUID = 1L;

	// Each song hashes to its URL. This embodies the entire contents
	// of a playlist.
	private HashMap<String, String> playlistMap;
	
	// A list of transaction IDs corresponding to add/edit/delete events
	// that were committed to this playlist.
	ArrayList<Integer> transactionsCompleted;
	
	/**
	 * Default constructor.
	 */
	public Playlist() {
		
		this.playlistMap = new HashMap<String, String>();
		this.transactionsCompleted = new ArrayList<Integer>();
	}
	
	
	/**
	 * Add a new <songName, URL> pair to the playlist.
	 * 
	 * @param songName, the name of the song.
	 * @param URL, the corresponding URL of the song.
	 * @param transID, the transaction ID corresponding to this commit event.
	 * @throws Exception if the song being added already exists in the
	 * playlist.
	 */
	public void add(String songName, String URL, Integer transID) throws Exception {
		
		// Make sure we aren't adding a duplicate.
		if (this.playlistMap.containsKey(songName)) {
			throw new Exception("The song name being add: " + 
					songName + " is already in the playlist.");
		}
		
		this.playlistMap.put(songName, URL);
		
		// Add the transaction ID to the list of completed transactions.
		if (this.transactionsCompleted.contains(transID))
		{
			System.out.println("This transaction ID is already in the Playlist! Terminating.");
			System.exit(-1);
		}
		
		this.transactionsCompleted.add(transID);
	}
	
	
	/**
	 * Deletes an existing song from the playlist.
	 * 
	 * @param songName, the name of the song.
	 * @param transID, the transaction ID corresponding to this commit event.
	 * @throws Exception if the name of the song is not within
	 * the playlist.
	 */
	public void remove(String songName, Integer transID) throws Exception {
		
		// Make sure this song is in our playlist (i.e., it is a key
		// in our hash map).
		if (!this.playlistMap.containsKey(songName)) {
			throw new Exception("The song name being removed: " + 
					songName + " was not in the playlist.");
		}
		
		// The song name (and it's value) is okay to remove.
		this.playlistMap.remove(songName);
		
		// Add the transaction ID to the list of completed transactions.
		if (this.transactionsCompleted.contains(transID))
		{
			System.out.println("This transaction ID is already in the Playlist! Terminating.");
			System.exit(-1);
		}
		
		this.transactionsCompleted.add(transID);
	}
	
	
	/**
	 * Changes the key, value pair associated with songName to correspond to
	 * the new name and URL given.
	 * 
	 * @param songName, the name of the song corresponding to the key, value
	 * pair to be changed.
	 * 
	 * @param newSongName, the new name.
	 * @param newSongURL, the new URL.
	 * @param transID, the transaction ID corresponding to this commit event.
	 * @throws Exception if the playlist does not contain the song
	 * corresponding to songName.
	 */
	public void edit(String songName, String newSongName, String newSongURL, Integer transID) throws Exception {
		
		// Make sure this song is in our playlist (i.e., it is a key
		// in our hash map).
		if (!this.playlistMap.containsKey(songName)) {
			throw new Exception("The song name being editted: " + 
					songName + " was not in the playlist.");
		}
		
		// Remove the old key, value pair.
		this.playlistMap.remove(songName);
		
		// Add a new key, value pair.
		// NOTE: this method call with insert the given transID into
		// the transaction ID list -- no need to do it here.
		this.add(newSongName, newSongURL, transID);
	}
	
	
	/**
	 * Return this playlist.
	 * @return this playlist.
	 */
	public HashMap<String, String> getPlaylist() {
		
		return this.playlistMap;
	}
	
	/**
	 * Prints this playlist to stdout.
	 */
	public void printPlaylist() {
		
		System.out.println("--------------------------------------------------------------------------------");
		System.out.print("Playlist has completed " + this.transactionsCompleted.size() + " transaction(s): ");
		
		// Print all but the last transaction ID.
		for (int i = 0; i < (this.transactionsCompleted.size() - 1); i++) {
			System.out.print(this.transactionsCompleted.get(i) + ", ");
		}
		
		// Print last transaction ID.
		System.out.println(this.transactionsCompleted.get(this.transactionsCompleted.size() - 1));
		
		int count = 1;
		
		for (String key : this.playlistMap.keySet()) {
		    System.out.println(count + ") " + key + ", " + this.playlistMap.get(key));
		    count++;
		}
		
		System.out.println("--------------------------------------------------------------------------------\n");
	}
}
