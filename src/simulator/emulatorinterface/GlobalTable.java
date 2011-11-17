package emulatorinterface;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.shm.Encoding;

public final class GlobalTable implements Encoding {

	private Hashtable<Long, SynchPrimitive> synchTable;
//	private Hashtable<Integer, ThreadState> stateTable;
	private IpcBase ipcType;

	public GlobalTable(IpcBase ipcType) {
		this.ipcType = ipcType;
		this.synchTable = new Hashtable();
		/*this.stateTable = (HashMap<Integer, ThreadState>) Collections
				.synchronizedMap(new HashMap<Integer, ThreadState>());
*/	}

/*	// Updates the thread states for both the threads
	public void updateThreadState(int thread, int interactingThread,
			long address) {
		helperupdateThreadState(thread, interactingThread, address);
		if (interactingThread != -1)
			helperupdateThreadState(interactingThread, thread, address);
	}

	private void helperupdateThreadState(int thread, int interactingThread,
			long address) {
		if (!stateTable.containsKey(thread)) {
			stateTable.put(thread, new ThreadState(interactingThread, address));
			return;
		} else {
			ThreadState curr = stateTable.get(thread);
			// if already exists update else insert
			for (StateType state : curr.state) {
				if (state.address == address) {
					state.interactingThread = interactingThread;
					System.out.println("Updated");
					return;
				}
			}

			curr.state.add(new StateType(interactingThread, address));
			System.out.println("Inserted");
		}
	}*/

	public int update(long addressSynchItem, int thread, long time,
			int encoding) {
		SynchPrimitive s;
		if (synchTable.containsKey(addressSynchItem))
			s = (SynchPrimitive)synchTable.get(addressSynchItem);
		else {
			s = new SynchPrimitive(
					addressSynchItem, thread, time, encoding, ipcType);
				synchTable.put(addressSynchItem, s);
		}

		int threadToResume=-1;
		switch (encoding) {
		case (BCAST):
			//TODO
			break;
		case (SIGNAL):
			threadToResume = s.sigEnter(thread, time, encoding);
			break;
		case (LOCK):
			threadToResume = s.lockEnter(thread, time, encoding);
			break;
		case (UNLOCK):
			threadToResume = s.unlockEnter(thread, time, encoding);
			break;
		case (JOIN):
			//TODO
			break;
		case (CONDWAIT):
			threadToResume = s.waitEnter(thread, time, encoding);
			break;
		case (BARRIERWAIT):
			//TODO
			break;
		case (BCAST + 1):
			// TODO
			break;
		case (SIGNAL + 1):
			// ignore
			break;
		case (LOCK + 1):
			threadToResume = s.lockExit(thread, time, encoding);
			break;
		case (UNLOCK + 1):
			// ignore
			break;
		case (JOIN + 1):
			break;
		case (CONDWAIT + 1):
			threadToResume = s.waitExit(thread, time, encoding);
			break;
		case (BARRIERWAIT + 1):
			break;
		}
		
		return threadToResume;
	}

}
