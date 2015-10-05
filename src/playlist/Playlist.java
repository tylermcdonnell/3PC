package playlist;

import java.util.HashMap;


/**
 * Each process in the network will have a local copy of a playlist (i.e.,
 * an instance of this class).  Upon a successful commit, each process
 * in the network will update their playlist.
 * 
 * @author Mike Feilbach
 *
 */
public class Playlist {

	// Each song hashes to its URL. This embodies the entire contents
	// of a playlist.
	HashMap<String, String> playlistMap;
	
	/**
	 * Default constructor.
	 */
	public Playlist() {
		
		this.playlistMap = new HashMap<String, String>();
	}
	
	
	/**
	 * Add a new <songName, URL> pair to the playlist.
	 * 
	 * @param songName, the name of the song.
	 * @param URL, the corresponding URL of the song.
	 * @throws Exception if the song being added already exists in the
	 * playlist.
	 */
	public void add(String songName, String URL) throws Exception {
		
		// Make sure we aren't adding a duplicate.
		if (this.playlistMap.containsKey(songName)) {
			throw new Exception("The song name being add: " + 
					songName + " is already in the playlist.");
		}
		
		this.playlistMap.put(songName, URL);
	}
	
	
	/**
	 * Deletes an existing song from the playlist.
	 * 
	 * @param songName, the name of the song.
	 * @throws Exception if the name of the song is not within
	 * the playlist.
	 */
	public void remove(String songName) throws Exception {
		
		// Make sure this song is in our playlist (i.e., it is a key
		// in our hash map).
		if (!this.playlistMap.containsKey(songName)) {
			throw new Exception("The song name being removed: " + 
					songName + " was not in the playlist.");
		}
		
		// The song name (and it's value) is okay to remove.
		this.playlistMap.remove(songName);
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
	 * @throws Exception if the playlist does not contain the song
	 * corresponding to songName.
	 */
	public void edit(String songName, String newSongName, String newSongURL) throws Exception {
		
		// Make sure this song is in our playlist (i.e., it is a key
		// in our hash map).
		if (!this.playlistMap.containsKey(songName)) {
			throw new Exception("The song name being editted: " + 
					songName + " was not in the playlist.");
		}
		
		// Remove the old key, value pair.
		this.playlistMap.remove(songName);
		
		// Add a new key, value pair.
		this.playlistMap.put(newSongName, newSongURL);
	}
}
