package emulatorinterface;

import java.util.ArrayList;
import java.util.HashMap;

class PerAddressInfo {

	ArrayList<Integer> probableInteractors;
	long timeSinceSlept;
	long address;

	public PerAddressInfo(ArrayList<Integer> tentativeInteractors,
			long time,long address) {
		super();
		this.probableInteractors = tentativeInteractors;
		this.timeSinceSlept = time;
		this.address = address;
	}

	public ArrayList<Integer> getTentativeInteractors() {
		return probableInteractors;
	}

	public void setTentativeInteractors(ArrayList<Integer> tentativeInteractors) {
		this.probableInteractors = tentativeInteractors;
	}

	public long getTime() {
		return timeSinceSlept;
	}

	public void setTime(long time) {
		this.timeSinceSlept = time;
	}



}

public class ThreadState {
	int threadIndex;
	long lastTimerseen=0;
	boolean timedWait=false;
	HashMap <Long,PerAddressInfo> addressMap = new HashMap<Long,PerAddressInfo>();
	
	public ThreadState(int tid){
		this.threadIndex = tid;
	}
	
	public void removeDep(int tidApp) {
		for (PerAddressInfo pai : addressMap.values()) {
			pai.probableInteractors.remove((Integer)tidApp);
			if (pai.probableInteractors.size()==0) {
				addressMap.remove(pai);
			}
		}
		if (addressMap.size()==0) {
			timedWait=false;
		}
	}
	
	public void addDep(long address, long time, int thread) {
			PerAddressInfo opai;
			if ((opai = this.addressMap.get(address)) != null) {
				opai.probableInteractors.add(thread);
			} else {
				ArrayList<Integer> th = new ArrayList<Integer>();
				th.add(thread);
				this.addressMap.put(address,
						new PerAddressInfo(th, time, address));
			}

	}
	public long timeSlept(long address) {
		return addressMap.get(address).timeSinceSlept;
	}
	
}
