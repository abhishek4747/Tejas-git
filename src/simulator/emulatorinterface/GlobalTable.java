package emulatorinterface;

import java.util.Hashtable;

import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.shm.Encoding;

public final class GlobalTable implements Encoding {

	private Hashtable<Long, SynchPrimitive> synchTable;
	private Hashtable<Integer, ThreadState> stateTable;
	private IpcBase ipcType;

	public GlobalTable(IpcBase ipcType) {
		this.ipcType = ipcType;
		this.synchTable = new Hashtable<Long, SynchPrimitive>();
		this.stateTable = new Hashtable<Integer, ThreadState>();
	}

	public Hashtable<Integer, ThreadState> getStateTable() {
		return stateTable;
	}


	public void setStateTable(Hashtable<Integer, ThreadState> stateTable) {
		this.stateTable = stateTable;
	}


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

		int threadToResume=-2;
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
