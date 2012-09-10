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

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import emulatorinterface.*;
import generic.CoreBcastBus;
import generic.InstructionTable;

public abstract class IpcBase {

	// Must ensure that MAXNUMTHREADS*EMUTHREADS == MaxNumThreads on the PIN side
	// Do not move it to config file unless you can satisfy the first constraint
	public static final int MaxNumJavaThreads = 1;
	public static final int EmuThreadsPerJavaThread = 1000; 
//	public static int memMapping[] = new int[EmuThreadsPerJavaThread];

	// state management for reader threads
	public boolean[] termination=new boolean[MaxNumJavaThreads];
	public boolean[] started=new boolean[MaxNumJavaThreads];

	// number of instructions read by each of the threads
	public long[] numInstructions = new long[MaxNumJavaThreads];

	// to maintain synchronization between main thread and the reader threads
	public static final Semaphore free = new Semaphore(0, true);

	public static InstructionTable insTable;
	public static GlobalTable glTable;

	StreamGobbler s1;
	StreamGobbler s2;
	
	// Initialise structures and objects
	public IpcBase () {
		glTable = new GlobalTable(this);
	}

	// Start the PIN process. Parse the cmd accordingly
	public Process startPIN(String cmd) throws Exception{
		Runtime rt = Runtime.getRuntime();
		try {
			Process p = rt.exec(cmd);
			s1 = new StreamGobbler ("stdin", p.getInputStream ());
			s2 = new StreamGobbler ("stderr", p.getErrorStream ());
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

	public abstract long update(int tidApp, int numReads);
	// The main thread waits for the finish of reader threads and returns total number of 
	// instructions read

	// return the total packets produced by PIN till now
	public abstract long totalProduced(int tidApp);

	public long doExpectedWaitForSelf() throws InterruptedException{
		
		// this takes care if no thread started yet.
		free.acquire();	
		
		int j=0;
		// if any thread has started and not finished then wait.
		for (int i=0; i<MaxNumJavaThreads; i++) {
			if (started[i] && !termination[i]) {
				free.acquire();
				j++;
			}
		}
		
		long totalInstructions = 0;
		
		//inform threads which have not started about finish
		for (int i=0; i<MaxNumJavaThreads; i++) {
			if (started[i]==false) {
				termination[i]=true;
			}
			//totalInstructions += numInstructions[i];
		}
		for (; j<MaxNumJavaThreads-1; j++)
			free.acquire();
		
		s1.join();
		s2.join();
		//return totalInstructions;
		return 0;
	}

	// Should wait for PIN too before calling the finish function to deallocate stuff related to
	// the corresponding mechanism
	public void doWaitForPIN(Process p) throws Exception{
		try {
			p.waitFor();
		} catch (Exception e) {

		}
	}

	// Free buffers, free memory , deallocate any stuff.
	public void finish(){
		System.out.println("Implement finish in the IPC mechanism");
	}

	public static int getEmuThreadsPerJavaThread()
	{
		return IpcBase.EmuThreadsPerJavaThread/30;
	}
	
	public static int getEmuThreadsPerJavaThread_Acutal()
	{
		return IpcBase.EmuThreadsPerJavaThread;
	}
	
	public abstract int fetchManyPackets(int tidApp, int readerLocation, int numReads,ArrayList<Packet> fromPIN);
}