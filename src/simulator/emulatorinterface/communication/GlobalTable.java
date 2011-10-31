package emulatorinterface.communication;

import java.util.HashMap;

import emulatorinterface.communication.shm.Encoding;

public final class GlobalTable implements Encoding {
	
	private HashMap<Long, SynchPrimitive> synchTable;
	private HashMap<Integer, ThreadState> stateTable;
	private IPCBase ipcType;
	
	public GlobalTable(IPCBase ipcType) {
		this.ipcType = ipcType;
		this.synchTable = new HashMap<Long, SynchPrimitive>();
		this.stateTable = new HashMap<Integer, ThreadState>();
	}

	// Updates the thread states for both the threads
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
	}


	public void update(long addressSynchItem, int thread, long time,
			int encoding) {
		SynchPrimitive s;
		if (synchTable.containsKey(addressSynchItem))
			s = synchTable.get(addressSynchItem);
		else
			s = synchTable.put(addressSynchItem, new SynchPrimitive(
					addressSynchItem, thread, time, encoding,ipcType));
		
		switch (encoding) {
		case (BCAST):
			break;
		case (SIGNAL):
			s.sigEnter(thread, time, encoding);
			break;
		case (LOCK):
			s.lockEnter(thread, time, encoding);
			break;
		case (UNLOCK):
			s.unlockEnter(thread, time, encoding);
			break;
		case (JOIN):
			break;
		case (CONDWAIT):
			s.waitEnter(thread, time, encoding);
			break;
		case (BARRIERWAIT):
			break;
		case (BCAST + 1):
			break;
		case (SIGNAL + 1):
			// ignore
			break;
		case (LOCK + 1):
			s.lockExit(thread, time, encoding);
			break;
		case (UNLOCK + 1):
			// ignore
			break;
		case (JOIN + 1):
			break;
		case (CONDWAIT + 1):
			s.waitExit(thread, time, encoding);
			break;
		case (BARRIERWAIT + 1):
			break;
		}
	}

}
