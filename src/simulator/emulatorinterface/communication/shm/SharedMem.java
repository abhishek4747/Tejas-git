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
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import config.SimulationConfig;
import emulatorinterface.communication.*;
import emulatorinterface.*;
import generic.Core;
import generic.CoreBcastBus;
import generic.InstructionTable;


public class SharedMem extends  IpcBase
{
	// Must ensure that this is same as COUNT in shmem.h
	public static final int COUNT = 1000;
	
	public SharedMem() 
	{
		super();
		// MAXNUMTHREADS is the max number of java threads while EMUTHREADS is the number of 
		// emulator(PIN) threads it is reading from. For each emulator threads 5 packets are
		// needed for lock management, queue size etc. For details look common.h
		System.out.println("coremap "+SimulationConfig.MapJavaCores);
		shmid = shmget(COUNT,MaxNumJavaThreads,getEmuThreadsPerJavaThread_Acutal(), SimulationConfig.MapJavaCores);
		shmAddress = shmat(shmid);
	}
		
	public long fetchManyPackets(int tidApp, long index, long numPackets,ArrayList<Packet> fromPIN){

		long[] ret  = new long[(int) (3*numPackets)]; 
		SharedMem.shmreadMult(tidApp, shmAddress, index, numPackets,ret);
			for (int i=0; i<numPackets; i++) {
				//fromPIN.add(i, new Packet(ret[3*i],ret[3*i+1],ret[3*i+2]));
				fromPIN.get(i).set(ret[3*i],ret[3*i+1],ret[3*i+2]);
				//System.out.println(fromPIN.get(i).toString());
			}
			
		return 0;
	}
	
	public long update(int tidApp, int numReads){
		get_lock(tidApp, shmAddress, COUNT);
		long queue_size = SharedMem.shmreadvalue(tidApp, shmAddress, COUNT);
		queue_size -= numReads;

		// update queue_size
		SharedMem
				.shmwrite(tidApp, shmAddress, COUNT, queue_size);
		SharedMem.release_lock(tidApp, shmAddress, COUNT);
		
		return queue_size;
	}
	
	public long totalProduced (int tidApp){
		return shmreadvalue(tidApp, shmAddress, COUNT + 4);
	}
	public void finish(){
		shmd(shmAddress);
		shmdel(shmid);
	}

	public void cleanup() {
		shmd(shmAddress);
		shmdel(shmid);
	}
	// calls shmget function and returns the shmid. Only 1 big segment is created and is indexed
	// by the threads id. Also pass the core mapping read from config.xml
	native int shmget(int COUNT,int MaxNumJavaThreads,int EmuThreadsPerJavaThread , long coremap);
	
	// attaches to the shared memory segment identified by shmid and returns the pointer to 
	// the memory attached. 
	native  long shmat(int shmid);
	
	// returns the class corresponding to the packet struct in common.h. Takes as argument the
	// emulator thread id, the pointer corresponding to that thread, the index where we want to
	// read and COUNT
	native static Packet shmread(int tid,long pointer, int index);
	
	// reads multiple packets into the arrays passed.
	native static void shmreadMult(int tid,long pointer, long index, long numPackets, long[] ret);
	
	// reads only the "value" from the packet struct. could be done using shmread() as well,
	// but if we only need to read value this saves from the heavy JNI callback and thus saves
	// on time.
	native static long shmreadvalue(int tid, long pointer, int index);
	
	// write in the shared memory. needed in peterson locks.
	native static int shmwrite(int tid,long pointer, int index, long val);
	
	// deatches the shared memory segment
	native static int shmd(long pointer);
	
	// deletes the shared memory segment
	native static int shmdel(int shmid);
	
	// inserts compiler barriers to avoid reordering. Needed for correct implementation of 
	// Petersons lock.
	native static void asmmfence();
	
	native static long numPacketsAlternate(int tidApp);

	// get a lock to access a resource shared between PIN and java. For an explanation of the 
	// shared memory segment structure which explains the parameters passed to the shmwrite 
	// and shmreadvalue functions here take a look in common.h
	public static void get_lock(int tid,long pointer, int COUNT) {
		shmwrite(tid,pointer,COUNT+2,1);
		asmmfence();
		shmwrite(tid,pointer,COUNT+3,0);
		asmmfence();
		while( (shmreadvalue(tid,pointer,COUNT+1) == 1) && (shmreadvalue(tid,pointer,COUNT+3) == 0)) {
		}
	}

	public static void release_lock(int tid,long pointer, int NUMINTS) {
		shmwrite(tid,pointer, NUMINTS+2,0);
	}

	public long numPackets(int tidApp) {
/*		get_lock(tidApp, shmAddress, COUNT);
		int size = SharedMem.shmreadvalue(tidApp, shmAddress, COUNT);
		release_lock(tidApp, shmAddress, COUNT);
		return size;
*/		return numPacketsAlternate(tidApp);
	}
	
	// loads the library which contains the implementation of these native functions. The name of
	// the library should match in the makefile.
	static { System.loadLibrary("shmlib"); }

	// cores associated with this java thread
	Core[] cores;

	// address of shared memory segment attached. should be of type 'long' to ensure for 64bit
	static long shmAddress;
	static int shmid;

	@Override
	public void initIpc() {
		if (SimulationConfig.debugMode) 
			System.out.println("-- SharedMem initialising");
		
		String name;
		for (int i=0; i<MaxNumJavaThreads; i++){
			name = "thread"+Integer.toString(i);
			termination[i]=false;
			started[i]=false;
			//TODO not all cores are assigned to each thread
			//when the mechanism to tie threads to cores is in place
			//this has to be changed
		}
		
	}


	@Override
	public Packet fetchOnePacket(int tidApp, int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long update(int tidApp, long numReads) {
		// TODO Auto-generated method stub
		return 0;
	}

	

	
}