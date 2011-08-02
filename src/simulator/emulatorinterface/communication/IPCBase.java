/*
 * This file declares some parameters which is common to all IPC mechanisms. Every IPC mechanism
 * inherits this class and implements the functions declared. Since Java has runtime binding
 * so the corresponding methods will be called.
 * 
 * MAXNUMTHREADS - The maximum number of java threads running
 * EMUTHREADS - The number of emulator threads 1 java thread is reading from
 * COUNT - this many number of packets is allocated in the shared memory for each 
 * 		   application/emulator thread 
 * */

package emulatorinterface.communication;

import emulatorinterface.*;
import emulatorinterface.communication.shm.RunnableThread;

public class IPCBase {

	// Must ensure that MAXNUMTHREADS*EMUTHREADS == MaxNumThreads on the PIN side
	public static final int MAXNUMTHREADS = 1;
	public static final int EMUTHREADS = 32; 

	// Initialise structures and objects
	public IPCBase () {}

	// Start the PIN process. Parse the cmd accordingly
	public Process startPIN(String cmd) throws Exception{
		Runtime rt = Runtime.getRuntime();
		try {
			Process p = rt.exec(cmd);
			StreamGobbler s1 = new StreamGobbler ("stdin", p.getInputStream ());
			StreamGobbler s2 = new StreamGobbler ("stderr", p.getErrorStream ());
			s1.start ();
			s2.start ();
			return p;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// Create Reader threads in java. A queue for returning information about the instruction
	// is passed which is filled by the reader threads.
	public void createReaders(DynamicInstructionBuffer passPackets){}
	
	public RunnableThread[] getReaderThreads() {return null;}

	// The main thread waits for the finish of reader threads and returns total number of 
	// instructions read
	public long doExpectedWaitForSelf() throws Exception{ return 0; }

	// Should wait for PIN too before calling the finish function to deallocate stuff related to
	// the corresponding mechanism
	public void doWaitForPIN(Process p) throws Exception{}

	// Free buffers, free memory , deallocate any stuff.
	public void finish(){}
}