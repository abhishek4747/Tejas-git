package emulatorinterface;

import java.util.ArrayList;
import java.util.Hashtable;

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

	ArrayList<synchTypes> entries;
	long address;

	public SynchPrimitive(long addressSynchItem, int thread, long time,
			int encoding, IpcBase ipcType) {
		this.address = addressSynchItem;
		this.entries = new ArrayList<synchTypes>();
		entries.add(new synchTypes(thread, time, encoding));
	}

	int sigEnter(int thread, long time, int encoding) {
		boolean done = false;
		int interactingThread = -1;
		synchTypes toBeRemoved1 = null, toBeRemoved2 = null;
		for (synchTypes entry : entries) {
			// if a wait enter before
			if (entry.encoding == CONDWAIT && entry.time < time) {
				for (synchTypes exit : entries) {
					// if a wait exit after
					if (exit.encoding == CONDWAIT + 1 && exit.time > time
							&& exit.thread == entry.thread) {
						if (done)
							misc.Error.shutDown("Duplicate entry in sigEnter");
						interactingThread = exit.thread;
						// IpcBase.glTable.updateThreadState(thread,
						// interactingThread, address);
						done = true;
						toBeRemoved1 = entry;
						toBeRemoved2 = exit;
						break;
					}
				}
			}
			if (done)
				break;
		}

		if (!done) {
			entries.add(new synchTypes(thread, time, encoding));
			// IpcBase.glTable.updateThreadState(thread, -1, address);
		} else {
			entries.remove(toBeRemoved1);
			entries.remove(toBeRemoved2);
		}

		return interactingThread;
	}

	int waitEnter(int thread, long time, int encoding) {
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
							&& sig.time > entry.time) {
						if (done)
							misc.Error.shutDown("Duplicate entry in sigEnter");
						interactingThread = sig.thread;
						// IpcBase.glTable.updateThreadState(thread,
						// interactingThread, address);
						done = true;
						toBeRemoved1 = entry;
						toBeRemoved2 = sig;
						break;
					}
				}
			}
			if (done)
				break;
		}

		if (!done) {
			entries.add(new synchTypes(thread, time, encoding));
			// IpcBase.glTable.updateThreadState(thread, -1, address);
		} else {
			entries.remove(toBeRemoved1);
			entries.remove(toBeRemoved2);
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
						if (done)
							misc.Error.shutDown("Duplicate entry in sigEnter");
						interactingThread = exit.thread;
						// IpcBase.glTable.updateThreadState(thread,
						// interactingThread, address);
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
			entries.add(new synchTypes(thread, time, encoding));
		} else {
			entries.remove(toBeRemoved1);
			entries.remove(toBeRemoved2);
		}
		return interactingThread;
	}

	int lockEnter(int thread, long time, int encoding) {
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
							&& unlock.time > entry.time) {
						interactingThread = unlock.thread;
						// IpcBase.glTable.updateThreadState(thread,
						// interactingThread, address);
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

				if (!done) {
					// lock enter and lock exit seen but no unlock enter. so
					// wait till others pass its time
					Hashtable<Integer, ThreadState> stateTable = IpcBase.glTable
							.getStateTable();
					ThreadState ts = stateTable.get(thread);
					ts.timedWait = true;
					ArrayList<Integer> others = new ArrayList<Integer>();
					// add dependencies bothways
					for (ThreadState otherThreads : stateTable.values()) {
						if (otherThreads.lastTimerseen < time) {
							otherThreads.addDep(address, time, thread);
							others.add(otherThreads.threadIndex);
						}
					}
					ts.addressMap.put(address, new PerAddressInfo(others, time, address));
				}
			}
			if (done)
				break;
		}

		if (!done) {
			entries.add(new synchTypes(thread, time, encoding));
		} else {
			entries.remove(toBeRemoved1);
			entries.remove(toBeRemoved2);
		}
		return interactingThread;
	}

}
