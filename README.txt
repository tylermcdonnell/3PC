README - P1

Mike Feilbach (mfeilbach@utexas.edu), mjf2628
Tyler Mcdonnel (EMAIL), UTEID                   **************** TYLER PLS FILL IN *****************
University of Texas at Austin
CS 380D -- Distributed Computing I
Professor Lorenzo Alvisi
Fall 2015

MIKE:
Slip days used (this project): 0
Slip days used (total)       : 2 (both were for H1)

TYLER:
Slip days used (this project): 0
Slip days used (total):        0

--------------------------------------------------------------------------------
- Notes:
--------------------------------------------------------------------------------

All code for the implementation of 3PC is within the 
3PC directory.

Scripts created to test our implementation are within 
the 3PC/scripts directory.

**NOTE: Please look at the scripts in 3PC/scripts to get an understanding
        of how to create a script given the below interface. A script is
        easily ran by starting the main method of Launcher.java, and running
        the "script" command (where you will provide the relative path of
        the script). At the top of each script file is the exact command
        you must enter to run that script, once starting the main method
        in Launcher. Simply copy and paste that command into the console
        and hit Enter.
        
**NOTE: The code itself is also documented in an appropriate manner.

--------------------------------------------------------------------------------
- Interface Provided: 
--------------------------------------------------------------------------------
add <songName> <URL>  - Attempt to add a new <songName, URL> pair to the 
                        Playlist of each process (atomically, using 3PC).
                        
remove <songName>     - Attempt to remove the specified song on the Playlist of
                        each process (atomically, using 3PC).
                        
edit <songName> <newSongName> <newURL>    - Attempt to edit the specified song
                                            on the Playlist of each process
                                            (atomically, using 3PC).

cp <numProcesses>     - Create the given number of processes.

kill <processID>      - Kill the process with the given ID.

killAll               - Kill all processes.

--killLeader

revive <processID>    - Revive the process corresponding to the given processID.

--reviveLast

reviveAll             - Revive all processes.

pm <processID> <numMessages>    - "Have specified process send only the number
                                   of messages then halt..."
                                   
rm <processID>        - "If a process' messaging has been paused, then resume
                        sending messages as normal."
                        
--allClear

rejectNextChange <process>  - "Specified process should vote no for the next update
                              suggested by the coordinator."
                              
e     - Exit out of program (and close all NetControllers).

s <numSeconds>  - Sleep numSeconds before executing the next command.

script <relativeScriptPath>   - Runs a script of pre-defined commands.

p     - Prints the Playlists of all processes to stdout in a nice format.

pl    - Prints the DT Logs of all processes to stdout in a nice format.

