/*
 * Implementation for the shared memory IPC between the simulator and emulator. Implements the
 * functions declared in IPCBase.java. It declares the native functions which are implemented
 * in JNIShm.c
 * 
 *  TODO: speedup can be achieved by calling a native init function and initialising some variables
 *  in the jni file which are generated again and again. 
 * */

package emulatorinterface.communication.shm;

import java.lang.System;
import java.util.concurrent.Semaphore;
import config.SimulationConfig;
import emulatorinterface.communication.*;
import emulatorinterface.*;
import generic.Core;
import generic.InstructionTable;
import generic.NewEventQueue;


public class SharedMem extends  IPCBase
{

	// Must ensure that this is same as COUNT in IPCBase.h
	public static final int COUNT = 1000;
	public static InstructionTable insTable;
	
	public SharedMem(InstructionTable instructionTable, NewEventQueue[] eventQ, Core[] cores) {
		// MAXNUMTHREADS is the max number of java threads while EMUTHREADS is the number of 
		// emulator(PIN) threads it is reading from. For each emulator threads 5 packets are
		// needed for lock management, queue size etc. For details look common.h
		insTable = instructionTable;
		this.eventQ = eventQ;
		this.cores = cores;
		System.out.println("coremap "+SimulationConfig.MapJavaCores);
		shmid = shmget((COUNT+5)*MAXNUMTHREADS*EMUTHREADS, SimulationConfig.MapJavaCores);
		ibuf = shmat(shmid);
	}
	
	public Process startPIN(String cmd) throws Exception {
		Runtime rt = Runtime.getRuntime();
		System.out.println("starting PIN");
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
	
	// Creates the reader threads. Takes DynamicInstructionBuffer as the argument  and fills it
	// with the profiling information of the instruction.
	// TODO should pass an array of DynamicInstructionBuffer if multiple such buffers are needed
	// for multiple threads. This side of the code is generic and can handle the case very easily.
	public void createReaders(DynamicInstructionBuffer passPackets) {
		String name;
		for (int i=0; i<MAXNUMTHREADS; i++){
			name = "thread"+Integer.toString(i);
			termination[i]=false;
			started[i]=false;
			numInstructions[i]=0;
			readerThreads[i] = new RunnableThread(name,i,passPackets, eventQ[i], cores);
			//TODO not all cores are assigned to each thread
			//when the mechanism to tie threads to cores is in place
			//this has to be changed
		}
	}

	public long doExpectedWaitForSelf() throws InterruptedException{
		
		// this takes care if no thread started yet.
		free.acquire();	
		
		// if any thread has started and not finished then wait.
		for (int i=0; i<MAXNUMTHREADS; i++) {
			if (started[i] && !termination[i]) {
				free.acquire();
			}
		}
		
		long totalInstructions = 0;
		
		//inform threads which have not started about finish
		for (int i=0; i<MAXNUMTHREADS; i++) {
			if (started[i]==false) termination[i]=true;
			totalInstructions += numInstructions[i];
		}

		return totalInstructions;
	}
	
	
	public void doWaitForPIN(Process p) throws Exception{
		try {
			p.waitFor();
		} catch (Exception e) {

		}
	}

	public void finish(){
		shmd(ibuf);
		shmdel(shmid);
	}

	// calls shmget function and returns the shmid. Only 1 big segment is created and is indexed
	// by the threads id. Also pass the core mapping read from config.xml
	native int shmget(int size, long coremap);
	
	// attaches to the shared memory segment identified by shmid and returns the pointer to 
	// the memory attached. 
	native  long shmat(int shmid);
	
	// returns the class corresponding to the packet struct in common.h. Takes as argument the
	// emulator thread id, the pointer corresponding to that thread, the index where we want to
	// read and COUNT
	native static Packet shmread(int tid,long pointer, int index,int COUNT);
	
	// reads only the "value" from the packet struct. could be done using shmread() as well,
	// but if we only need to read value this saves from the heavy JNI callback and thus saves
	// on time.
	native static int shmreadvalue(int tid, long pointer, int index, int COUNT);
	
	// write in the shared memory. needed in peterson locks.
	native static int shmwrite(int tid,long pointer, int index, int val,int COUNT);
	
	// deatches the shared memory segment
	native static int shmd(long pointer);
	
	// deletes the shared memory segment
	native static int shmdel(int shmid);
	
	// inserts compiler barriers to avoid reordering. Needed for correct implementation of 
	// Petersons lock.
	native static void asmmfence();

	// get a lock to access a resource shared between PIN and java. For an explanation of the 
	// shared memory segment structure which explains the parameters passed to the shmwrite 
	// and shmreadvalue functions here take a look in common.h
	public static void get_lock(int tid,long pointer, int COUNT) {
		shmwrite(tid,pointer,COUNT+2,1,COUNT);
		asmmfence();
		shmwrite(tid,pointer,COUNT+3,0,COUNT);
		asmmfence();
		while( (shmreadvalue(tid,pointer,COUNT+1,COUNT) == 1) && (shmreadvalue(tid,pointer,COUNT+3,COUNT) == 0)) {
		}
	}

	public static void release_lock(int tid,long pointer, int NUMINTS) {
		shmwrite(tid,pointer, NUMINTS+2,0,NUMINTS);
	}

	// loads the library which contains the implementation of these native functions. The name of
	// the library should match in the makefile.
	static { System.loadLibrary("shmlib"); }

	// to maintain synchronization between main thread and the reader threads
	static final Semaphore free = new Semaphore(0, true);
	
	// the reader threads. Each thread reads from EMUTHREADS
	RunnableThread [] readerThreads = new RunnableThread[MAXNUMTHREADS];
	
	// event queues - one event queue for each java thread
	NewEventQueue[] eventQ;
	
	// cores associated with this java thread
	Core[] cores;

	// state management for reader threads
	static boolean[] termination=new boolean[MAXNUMTHREADS];
	static boolean[] started=new boolean[MAXNUMTHREADS];
	
	// address of shared memory segment attached. should be of type 'long' to ensure for 64bit
	static long ibuf;
	static int shmid;
	
	// number of instructions read by each of the threads
	static long[] numInstructions = new long[MAXNUMTHREADS];
	
	public RunnableThread[] getReaderThreads() {
		return readerThreads;
	}

}
