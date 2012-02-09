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

import java.util.concurrent.Semaphore;

import emulatorinterface.*;
import generic.InstructionTable;

public abstract class IpcBase {

	// Must ensure that MAXNUMTHREADS*EMUTHREADS == MaxNumThreads on the PIN side
	// Do not move it to config file unless you can satisfy the first constraint
	public static final int MaxNumJavaThreads = 1;
	public static final int EmuThreadsPerJavaThread = 128; 
	// Must ensure that this is same as COUNT in IPCBase.h
	public static final int COUNT = 1000;

	// state management for reader threads
	public boolean[] termination=new boolean[MaxNumJavaThreads];
	public boolean[] started=new boolean[MaxNumJavaThreads];
	
	// number of instructions read by each of the threads
	public long[] numInstructions = new long[MaxNumJavaThreads];
	
	// to maintain synchronization between main thread and the reader threads
	public static final Semaphore free = new Semaphore(0, true);
	
	public static InstructionTable insTable;
	public static GlobalTable glTable;
	
	// Initialise structures and objects
	public IpcBase () {}

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

	public abstract void initIpc();
	
	
	/*** start, finish, isEmpty, fetchPacket, isTerminated ****/
	public RunnableThread[] getRunnableThreads(){
		System.out.println("Implement getRunnableThreads() in the IPC mechanism");
		return null;
	}

	// returns the numberOfPackets which are currently there in the stream for tidApp
	public abstract int numPackets(int tidApp);
	
	// fetch one packet for tidApp from index
	public abstract Packet fetchOnePacket(int tidApp, int index );
	
	public abstract int update(int tidApp, int numReads);
	// The main thread waits for the finish of reader threads and returns total number of 
	// instructions read
	
	// return the total packets produced by PIN till now
	public abstract int totalProduced(int tidApp);
	
	public long doExpectedWaitForSelf() throws Exception {
		System.out.println("Implement doExpectedWaitForSelf() in the IPC mechanism");
		return -1;
	}

	// Should wait for PIN too before calling the finish function to deallocate stuff related to
	// the corresponding mechanism
	public void doWaitForPIN(Process p) throws Exception{
		System.out.println("Implement doWaitForPIN in the IPC mechanism");
	}

	// Free buffers, free memory , deallocate any stuff.
	public void finish(){
		System.out.println("Implement finish in the IPC mechanism");
	}
	
}