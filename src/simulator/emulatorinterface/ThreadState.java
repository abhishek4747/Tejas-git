package emulatorinterface;

import java.util.ArrayList;

class StateType {
	public StateType(int interactingThread2, long address2) {
		this.interactingThread = interactingThread2;
		this.address = address2;
	}
	int interactingThread;
	long address; //  address of the synchronization primitive
}

public class ThreadState {

	public ThreadState(int interactingThread, long address) {
		state.add(new StateType(interactingThread, address));
	}

	ArrayList<StateType> state;
	
}
