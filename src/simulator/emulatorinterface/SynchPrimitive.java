package emulatorinterface;

import java.util.Hashtable;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.shm.Encoding;

class synchTypes {

	public synchTypes(int thread, long time, int encoding) {
		super();
		this.thread = thread;
		this.time = time;
		this.encoding = encoding;
	}

	int thread;
	long time;
	int encoding; // same as in Encoding.java
}

public class SynchPrimitive implements Encoding {

	private static final Logger logger = Logger.getLogger(SynchPrimitive.class);
	
	LinkedList<synchTypes> entries;
	long address;

	public SynchPrimitive(long addressSynchItem, int thread, long time,
			int encoding, IpcBase ipcType) {
		this.address = addressSynchItem;
		this.entries = new LinkedList<synchTypes>();
		entries.add(new synchTypes(thread, time, encoding));
	}

	private int putOnTimedWait(int thread, long time, int encoding) {
		Hashtable<Integer, ThreadState> stateTable = IpcBase.glTable
		.getStateTable();
		ThreadState ts = stateTable.get(thread);
		LinkedList<Integer> others = new LinkedList<Integer>();
		// add dependencies bothways
		for (ThreadState otherThreads : stateTable.values()) {
			if (otherThreads.lastTimerseen < time && otherThreads.threadIndex!=thread) {
				otherThreads.addDep(address, time, thread);
				others.add(otherThreads.threadIndex);
			}
		}
		if (others.size()!=0) {
			System.out.println(this.address+"  "+thread+" `"+encoding+"`, going on a timedWait on "+others.size()+" threads");
			IpcBase.glTable.getStateTable().get((Integer)thread).countTimedSleep++;
			//ts.timedWait = true;
			ts.addressMap.put(address, new PerAddressInfo(others, time, address,true));
			entries.add(new synchTypes(thread, time, encoding));
		}
		else {
			System.out.println(this.address+"  "+thread+" `"+encoding+"`, no TimedWait ");
			ts.addressMap.remove(address);
		}
		return others.size();
	}
	
	int sigEnter(int thread, long time, int encoding) {
		boolean done = false;
		int interactingThread = -1;
		synchTypes toBeRemoved1 = null, toBeRemoved2 = null;
		for (synchTypes entry : entries) {
			// if a wait enter before
			if (entry.encoding == CONDWAIT && entry.time < time  && entry.thread != thread) {
				for (synchTypes exit : entries) {
					// if a wait exit after
					if (exit.encoding == CONDWAIT + 1 && exit.time > time
							&& exit.thread == entry.thread) {
						System.out.println(this.address+" "+thread+" sigenter, got waitenter & exit from "+exit.thread);
						if (done)
							misc.Error.shutDown("Duplicate entry in sigEnter");
						interactingThread = exit.thread;
						done = true;
						toBeRemoved1 = entry;
						toBeRemoved2 = exit;
						Hashtable<Integer, ThreadState> stateTable = IpcBase.glTable.getStateTable();
						stateTable.get(interactingThread).addressMap.remove(address);
						stateTable.get(thread).addressMap.remove(address);
						break;
					}
				}
			}
			if (done)
				break;
		}

		if (!done) {
			int otherThreads = putOnTimedWait(thread, time, encoding);
			if (otherThreads==0) {
				System.out.println("Spurious signal received. Too Bad !!");
				interactingThread = -2; // means nobody sleeps/resumes
			}
		} else {
			entries.remove(toBeRemoved1);
			entries.remove(toBeRemoved2);
		}

		return interactingThread;
	}

	int waitEnter(int thread, long time, int encoding) {
		System.out.println(this.address+" "+" waitEnter");
		entries.add(new synchTypes(thread, time, encoding));
		return -1;
	}

	int waitExit(int thread, long time, int encoding) {
		boolean done = false;
		int interactingThread = -1;
		synchTypes toBeRemoved1 = null, toBeRemoved2 = null;
		for (synchTypes entry : entries) {
			// if this thread entered
			if (entry.encoding == CONDWAIT && entry.time < time
					&& entry.thread == thread) {
				for (synchTypes sig : entries) {
					// if a signal by some other thread found
					if (sig.encoding == SIGNAL && sig.time < time
							&& sig.time > entry.time && sig.thread!=thread) {
						if (done)
							misc.Error.shutDown("Duplicate entry in wEx");
						System.out.println(this.address+"  "+thread+" waitexit, got signal dep on "+sig.thread);
						interactingThread = sig.thread;
						done = true;
						toBeRemoved1 = entry;
						toBeRemoved2 = sig;
						
						Hashtable<Integer, ThreadState> stateTable = IpcBase.glTable
						.getStateTable();
						stateTable.get(interactingThread).addressMap
						.remove(address);
						stateTable.get(thread).addressMap.remove(address);
						break;
					}
				}
			}
			if (done)
				break;
		}

		if (!done) {
			// XXX the only difference between lock/unlock and wait/signal is here.
			// as we are not going for a timedWait but original wait.
			entries.add(new synchTypes(thread, time, encoding));
			interactingThread = -2;
		} else {
			entries.remove(toBeRemoved1);
			if (toBeRemoved2!=null) entries.remove(toBeRemoved2);
		}
		return interactingThread;
	}

	int unlockEnter(int thread, long time, int encoding) {
		boolean done = false;
		int interactingThread = -1;
		synchTypes toBeRemoved1 = null, toBeRemoved2 = null;
		for (synchTypes entry : entries) {
			// if a lock enter before
			if (entry.encoding == LOCK && entry.time < time) {
				for (synchTypes exit : entries) {
					// if a lock exit after
					if (exit.encoding == LOCK + 1 && exit.time > time
							&& exit.thread == entry.thread) {
						System.out.println(this.address+"  "+thread+" unlockenter, got lockenter and lockexit from "+exit.thread);
						if (done)
							misc.Error.shutDown("Duplicate entry in unlockEnter");
						interactingThread = exit.thread;
						done = true;
						toBeRemoved1 = entry;
						toBeRemoved2 = exit;

						Hashtable<Integer, ThreadState> stateTable = IpcBase.glTable
						.getStateTable();
						stateTable.get(interactingThread).addressMap
						.remove(address);
						stateTable.get(thread).addressMap.remove(address);
						break;
					}
				}
			}
			if (done)
				break;
		}

		if (!done) {
			//May never get a corresponding lockenter lockexit
			//so do a timed wait.
			int otherThreads = putOnTimedWait(thread, time, encoding);
			if (otherThreads==0) interactingThread = -2;// means nobody sleeps/resumes
		
		} else {
			entries.remove(toBeRemoved1);
			entries.remove(toBeRemoved2);
		}
		return interactingThread;
	}

	int lockEnter(int thread, long time, int encoding) {
		System.out.println(this.address+"  "+thread+" lockenter");
		entries.add(new synchTypes(thread, time, encoding));
		return -1;
	}

	int lockExit(int thread, long time, int encoding) {
		boolean done = false;
		int interactingThread = -1;
		synchTypes toBeRemoved1 = null, toBeRemoved2 = null;
		for (synchTypes entry : entries) {
			// if this thread entered
			if (entry.encoding == LOCK && entry.time < time
					&& entry.thread == thread) {
				for (synchTypes unlock : entries) {
					// if an unlock by some other thread found
					if (unlock.encoding == UNLOCK && unlock.time < time
							&& unlock.time > entry.time && unlock.thread!=thread) {
						System.out.println(this.address+"  "+thread+" lockexit, got unlock dep on "+unlock.thread);
						interactingThread = unlock.thread;
						done = true;
						toBeRemoved1 = entry;
						toBeRemoved2 = unlock;

						Hashtable<Integer, ThreadState> stateTable = IpcBase.glTable
						.getStateTable();
						stateTable.get(interactingThread).addressMap
						.remove(address);
						stateTable.get(thread).addressMap.remove(address);
						break;
					}
				}
			}
			if (done)
				break;
		}

		if (!done) {
			// lock enter and lock exit seen but no unlock enter. so
			// wait till others pass its time
			int otherThreads = putOnTimedWait(thread, time, encoding);
			if (otherThreads == 0) interactingThread = thread;
			else interactingThread = -2;
		} else {
			entries.remove(toBeRemoved1);
			if(toBeRemoved2!=null) entries.remove(toBeRemoved2);
		}
		return interactingThread;
	}

}
