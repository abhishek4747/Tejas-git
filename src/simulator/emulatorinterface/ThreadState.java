package emulatorinterface;

import java.util.ArrayList;


public class ThreadState {

	ArrayList<Integer> tentativeInteractors;
	long timeSinceSlept;
	long address;
	
	public ThreadState(ArrayList<Integer> tentativeInteractors,
			long time, long address) {
		super();
		this.tentativeInteractors = tentativeInteractors;
		this.timeSinceSlept = time;
		this.address = address;
	}

	public ArrayList<Integer> getTentativeInteractors() {
		return tentativeInteractors;
	}

	public void setTentativeInteractors(ArrayList<Integer> tentativeInteractors) {
		this.tentativeInteractors = tentativeInteractors;
	}

	public long getTime() {
		return timeSinceSlept;
	}

	public void setTime(long time) {
		this.timeSinceSlept = time;
	}
	
	

	
}
