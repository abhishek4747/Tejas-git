package emulatorinterface;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import sun.java2d.StateTrackable;

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
	private static final boolean debugMode = false; 
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
			if(debugMode)	
				System.out.println(this.address+"  "+thread+" `"+encoding+"`, going on a timedWait on "+others.size()+" threads");
			stateTable.get((Integer)thread).countTimedSleep++;
			ts.addressMap.put(address, new PerAddressInfo(others, time, address,true));
			entries.add(new synchTypes(thread, time, encoding));
		}
		else {
			if(debugMode)
				System.out.println(this.address+"  "+thread+" `"+encoding+"`, no TimedWait ");
			ts.addressMap.remove(address);
		}
		return others.size();
	}

	ResumeSleep sigEnter(int thread, long time, int encoding) {
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
						if(debugMode)
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
				System.out.println("SynchPrimitive: Spurious signal received");
				interactingThread = -2; // means nobody sleeps/resumes
			}
		} else {
			entries.remove(toBeRemoved1);
			entries.remove(toBeRemoved2);
		}

		ResumeSleep ret = new ResumeSleep();
		if (interactingThread==-1) ret.addSleeper(thread);
		else if (interactingThread==-2) {}
		else {
			ret.addResumer(interactingThread);
		}
		return ret;
	}

	ResumeSleep waitEnter(int thread, long time, int encoding) {
		//System.out.println(this.address+" "+" waitEnter");
		entries.add(new synchTypes(thread, time, encoding));
		ResumeSleep ret = new ResumeSleep();
		ret.addSleeper(thread);
		return ret;
	}

	ResumeSleep waitExit(int thread, long time, int encoding) {
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
						if(debugMode)
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
			//if (entry.encoding == BCAST && entry.time < time)
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

		ResumeSleep ret = new ResumeSleep();
		if (interactingThread==-1) ret.addSleeper(thread);
		else if (interactingThread==-2) {}
		else {
			ret.addResumer(interactingThread);
			ret.addResumer(thread);
		}
		return ret;
	}

	ResumeSleep unlockEnter(int thread, long time, int encoding) {
		boolean done = false;
		int interactingThread = -1;
		synchTypes toBeRemoved1 = null, toBeRemoved2 = null;
		for (synchTypes entry : entries) {
			// if a lock enter before
			if (entry.encoding == LOCK && entry.time < time && entry.thread!=thread) {
				for (synchTypes exit : entries) {
					// if a lock exit after
					if (exit.encoding == LOCK + 1 && exit.time > time
							&& exit.thread == entry.thread) {
						if(debugMode)
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

		ResumeSleep ret = new ResumeSleep();
		if (interactingThread==-1) ret.addSleeper(thread);
		else if (interactingThread==-2) {}
		else {
			ret.addResumer(interactingThread);
		}
		return ret;
	}

	ResumeSleep lockEnter(int thread, long time, int encoding) {
		if(debugMode) System.out.println(this.address+"  "+thread+" lockenter");
		entries.add(new synchTypes(thread, time, encoding));
		ResumeSleep ret = new ResumeSleep();
		ret.addSleeper(thread);
		return ret;
	}

	ResumeSleep lockExit(int thread, long time, int encoding) {
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
						if(debugMode) 
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

		ResumeSleep ret = new ResumeSleep();
		if (interactingThread==-1) ret.addSleeper(thread);
		else if (interactingThread==-2) {}
		else if (interactingThread==thread){
			ret.addResumer(thread);
		}
		else {
			ret.addResumer(interactingThread);
			ret.addResumer(thread);
		}
		return ret;
	}

	//check if "waitenter before" and "waitexit after/or not available"
	ResumeSleep broadcastResume(long broadcastTime, int thread) {
		ResumeSleep ret = new ResumeSleep();
		ArrayList<synchTypes> toBeRemoved = new ArrayList<synchTypes>();
		for (synchTypes entry : entries) {
			if (entry.encoding == BCAST && entry.thread==thread) {
				ret.addResumer(entry.thread);
				toBeRemoved.add(entry);
			}
			boolean exitPresent = false;
			if (entry.encoding == CONDWAIT && entry.time<broadcastTime && entry.thread!=thread) {
				for (synchTypes exit : entries) {
					if (exit.encoding == CONDWAIT+1 && exit.time>broadcastTime && exit.thread==entry.thread) {
						// resume these thread
						ret.addResumer(exit.thread);
						toBeRemoved.add(entry);
					}
					if (exit.encoding == CONDWAIT+1 && exit.time>entry.time) {
						exitPresent = true;
						// this means it is a stale entry
					}
				}
				if (!exitPresent) {
					//no exit, ONLY enter, resume these as well
					ret.addResumer(entry.thread);
					toBeRemoved.add(entry);
				}
			}
		}
		for(synchTypes rem : toBeRemoved) {
			entries.remove(rem);
		}
		return ret;
	}

	ResumeSleep broadcastEnter(int thread, long time, int encoding) {
		if(debugMode)
			System.out.println(this.address+"  "+thread+" broadcastenter");
		ResumeSleep ret = new ResumeSleep(); 
		Hashtable<Integer, ThreadState> stateTable = IpcBase.glTable.getStateTable();
		if (putOnTimedWait(thread, time, encoding)==0) {
			//return all threads which were waiting to resume
			ArrayList<synchTypes> toBeRemoved = new ArrayList<synchTypes>();
			for (synchTypes entry : entries ) {
				if (entry.encoding == CONDWAIT && entry.time < time && entry.thread != thread) {
					for (synchTypes exit : entries) {
						if (exit.encoding == CONDWAIT+1 && exit.time > time && exit.thread==entry.thread) {
							ret.addResumer(exit.thread);
							stateTable.get(exit.thread).addressMap.remove(address);
							toBeRemoved.add(entry);
							toBeRemoved.add(exit);
						}
					}
				}
			}

			stateTable.get(thread).addressMap.remove(address);
			for (synchTypes t : toBeRemoved) {
				entries.remove(t);
			}
		}
		else {
			PerAddressInfo p = stateTable.get(thread).addressMap.get(address);
			p.on_broadcast = true;
			p.broadcastTime = time;
			// Not all threads have passed in time.
			entries.add(new synchTypes(thread, time, encoding));
			ret.addSleeper(thread);
		}
		return ret;
	}

	public ResumeSleep barrierEnter(int thread, long time, int encoding) {
		if(debugMode)
			System.out.println(this.address+"  "+thread+" barrierenter");
		entries.add(new synchTypes(thread, time, encoding));
		ResumeSleep ret = new ResumeSleep();
		ret.addSleeper(thread);
		return ret;
	}

	public ResumeSleep barrierExit(int thread, long time, int encoding) {
		if(debugMode)
			System.out.println(this.address+"  "+thread+" barrierexit");
		Hashtable<Integer, ThreadState> stateTable = IpcBase.glTable.getStateTable();
		ResumeSleep ret = new ResumeSleep();
		if (putOnTimedWait(thread, time, encoding)==0) {
			ArrayList<synchTypes> toBeRemoved = new ArrayList<synchTypes>();
			for (synchTypes entry : entries ) {
				if (entry.encoding==BARRIERWAIT) {
					ret.addResumer(entry.thread);
					stateTable.get(entry.thread).addressMap.remove(address);
					toBeRemoved.add(entry);
				}
			}

			stateTable.get(thread).addressMap.remove(address);
			for (synchTypes t : toBeRemoved) {
				entries.remove(t);
			}
		}
		else {
			PerAddressInfo p = stateTable.get(thread).addressMap.get(address);
			p.on_barrier = true;
		}
		return ret;
	}

	public ResumeSleep barrierResume() {
		ResumeSleep ret = new ResumeSleep();
		ArrayList<synchTypes> toBeRemoved = new ArrayList<synchTypes>();
		for (synchTypes entry : entries) {
			if (entry.encoding == BARRIERWAIT) {
				ret.addResumer(entry.thread);
				toBeRemoved.add(entry);
			}
		}

		for(synchTypes t : toBeRemoved) {
			entries.remove(t);
		}
		return ret;
	}

}
