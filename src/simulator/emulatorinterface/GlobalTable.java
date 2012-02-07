package emulatorinterface;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.shm.Encoding;

class ResumeSleep {
	ArrayList<Integer> resume=new ArrayList<Integer>();
	ArrayList<Integer> sleep=new ArrayList<Integer>();
	
	void addSleeper(int t){
		this.sleep.add(t);
	}
	
	void addResumer(int t){
		this.resume.add(t);
	}
	
	int getNumResumers() {
		return this.resume.size();
	}
	
	int getNumSleepers() {
		return this.sleep.size();
	}
}

public final class GlobalTable implements Encoding {

	private Hashtable<Long, SynchPrimitive> synchTable;
	private Hashtable<Integer, ThreadState> stateTable;
	private IpcBase ipcType;

	public GlobalTable(IpcBase ipcType) {
		this.ipcType = ipcType;
		this.synchTable = new Hashtable<Long, SynchPrimitive>();
		this.stateTable = new Hashtable<Integer, ThreadState>();
	}

	public Hashtable<Long, SynchPrimitive> getSynchTable() {
		return synchTable;
	}
	public Hashtable<Integer, ThreadState> getStateTable() {
		return stateTable;
	}

	public void setStateTable(Hashtable<Integer, ThreadState> stateTable) {
		this.stateTable = stateTable;
	}


	// returns -2 if no thread needs to be slept/resumed.
	// returns -1 if 'this' thread needs to sleep
	// o/w returns otherThreadsId which will now resume
	public ResumeSleep update(long addressSynchItem, int thread, long time,
			int encoding) {
		SynchPrimitive s;
		if (synchTable.containsKey(addressSynchItem))
			s = (SynchPrimitive)synchTable.get(addressSynchItem);
		else {
			s = new SynchPrimitive(
					addressSynchItem, thread, time, encoding, ipcType);
				synchTable.put(addressSynchItem, s);
		}

		ResumeSleep ret = new ResumeSleep();
		switch (encoding) {
		case (BCAST):
			ret = s.broadcastEnter(thread,time,encoding);
			break;
		case (SIGNAL):
			ret = s.sigEnter(thread, time, encoding);
			break;
		case (LOCK):
			ret = s.lockEnter(thread, time, encoding);
			break;
		case (UNLOCK):
			ret = s.unlockEnter(thread, time, encoding);
			break;
		case (JOIN):
			//TODO
			break;
		case (CONDWAIT):
			ret = s.waitEnter(thread, time, encoding);
			break;
		case (BARRIERWAIT):
			ret = s.barrierEnter(thread, time, encoding);
			System.out.println(thread+"  barrier enter");
			break;
		case (BCAST + 1):
			// TODO
			break;
		case (SIGNAL + 1):
			// ignore
			break;
		case (LOCK + 1):
			ret = s.lockExit(thread, time, encoding);
			break;
		case (UNLOCK + 1):
			// ignore
			break;
		case (JOIN + 1):
			break;
		case (CONDWAIT + 1):
			ret = s.waitExit(thread, time, encoding);
			break;
		case (BARRIERWAIT + 1):
			ret = s.barrierExit(thread, time, encoding);
			System.out.println(thread+"  barrier exit");
			break;
		}
		
		
		return ret;
	}

}
