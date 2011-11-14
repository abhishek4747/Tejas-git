package emulatorinterface;

import java.util.ArrayList;

import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.shm.Encoding;
import emulatorinterface.communication.shm.SharedMem;

class synchTypes {
	
	
	public synchTypes(int thread, long time, int encoding) {
		super();
		this.thread = thread;
		this.time = time;
		this.encoding = encoding;
	}
	int thread;
	long time;
	int encoding;  // same as in Encoding.java
}
public class SynchPrimitive implements Encoding{

	ArrayList<synchTypes> entries;
	long address;
	IpcBase ipcType;

 	public SynchPrimitive(long addressSynchItem, int thread, long time, int encoding,IpcBase ipcType) {
		this.address = addressSynchItem;
		this.entries = new ArrayList<synchTypes>();
		this.ipcType = ipcType;
		entries.add(new synchTypes(thread, time, encoding));
	}

	// FIXME What if the SYNCH instruction corresponding to this signal reaches to pipeline and its corresponding wait_exit has not yet been seen in the runnable
 	// thread. This SYNCH will then get ignored (cant be avoided as there could be an arbitrary signal without any corresponding wait). Even if I stop ignoring
 	// here the problem is still there in LOCK & UNLOCK.
 	void sigEnter(int thread, long time, int encoding) {
 		boolean done=false;
 		int interactingThread = -1;
 		for (synchTypes entry : entries) {
 			// if a wait enter before
			if (entry.encoding==CONDWAIT && entry.time<time) {
				for (synchTypes exit : entries) {
					// if a wait exit after
					if (exit.encoding==CONDWAIT+1 && exit.time>time && exit.thread==entry.thread) {
						if (done) misc.Error.shutDown("Duplicate entry in sigEnter",ipcType);
						interactingThread = exit.thread;
						SharedMem.glTable.updateThreadState(thread, interactingThread, address);
						done = true;
						entries.remove(entry);
						entries.remove(exit);
						break;
					}
				}
			}
		}
 		
 		if (!done) {
 			entries.add(new synchTypes(thread, time, encoding));
 			SharedMem.glTable.updateThreadState(thread, -1, address);
 		}
 	}
 	
 	void waitEnter(int thread, long time, int encoding) {
 			entries.add(new synchTypes(thread, time, encoding));
 	}
 	
 	void waitExit(int thread, long time, int encoding) {
 		boolean done=false;
 		int interactingThread = -1;
 		for (synchTypes entry : entries) {
 			// if this thread entered
			if (entry.encoding==CONDWAIT && entry.time<time && entry.thread == thread) {
				for (synchTypes sig : entries) {
					// if a signal by some other thread found
					if (sig.encoding==SIGNAL && sig.time<time && sig.time>entry.time) {
						if (done) misc.Error.shutDown("Duplicate entry in sigEnter",ipcType);
						interactingThread = sig.thread;
						SharedMem.glTable.updateThreadState(thread, interactingThread, address);
						done = true;
						entries.remove(entry);
						entries.remove(sig);
						break;
					}
				}
			}
		}
 		
 		if (!done) {
 			entries.add(new synchTypes(thread, time, encoding));
 			SharedMem.glTable.updateThreadState(thread, -1, address);
 		}
 	}

 	//TODO this turned out to be exact same code after the modification
 	// so instead remove this function and make a generic function for both
 	// sigEnter and unlockEnter
 	void unlockEnter (int thread, long time, int encoding) {
 		boolean done=false;
 		int interactingThread = -1;
 		for (synchTypes entry : entries) {
 			// if a lock enter before
			if (entry.encoding==LOCK && entry.time<time) {
				for (synchTypes exit : entries) {
					// if a lock exit after
					if (exit.encoding==LOCK+1 && exit.time>time && exit.thread==entry.thread) {
						if (done) misc.Error.shutDown("Duplicate entry in sigEnter",ipcType);
						interactingThread = exit.thread;
						SharedMem.glTable.updateThreadState(thread, interactingThread, address);
						done = true;
						entries.remove(entry);
						entries.remove(exit);
						break;
					}
				}
			}
		}
 		
 		if (!done) {
 			entries.add(new synchTypes(thread, time, encoding));
 			SharedMem.glTable.updateThreadState(thread, -1, address);
 		} 		
 	}

 	void lockEnter (int thread, long time, int encoding) {
 		entries.add(new synchTypes(thread, time, encoding));
 	}
 	
 	void lockExit (int thread, long time, int encoding) {
 		boolean done=false;
 		int interactingThread = -1;
 		for (synchTypes entry : entries) {
 			// if this thread entered
			if (entry.encoding==LOCK && entry.time<time && entry.thread == thread) {
				for (synchTypes unlock : entries) {
					// if an unlock by some other thread found
					if (unlock.encoding==UNLOCK && unlock.time<time && unlock.time>entry.time) {
						if (done) misc.Error.shutDown("Duplicate entry in sigEnter",ipcType);
						interactingThread = unlock.thread;
						SharedMem.glTable.updateThreadState(thread, interactingThread, address);
						done = true;
						entries.remove(entry);
						entries.remove(unlock);
						break;
					}
				}
			}
		}
 		
 		if (!done) {
 			entries.add(new synchTypes(thread, time, encoding));
 			SharedMem.glTable.updateThreadState(thread, -1, address);
 		}
 	}
 	
}
