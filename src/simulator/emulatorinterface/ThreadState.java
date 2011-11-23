package emulatorinterface;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

class PerAddressInfo {

	LinkedList<Integer> probableInteractors;
	long timeSinceSlept;
	long address;

	public PerAddressInfo(LinkedList<Integer> tentativeInteractors,
			long time,long address) {
		super();
		this.probableInteractors = tentativeInteractors;
		this.timeSinceSlept = time;
		this.address = address;
	}

	public LinkedList<Integer> getTentativeInteractors() {
		return probableInteractors;
	}

	public void setTentativeInteractors(LinkedList<Integer> tentativeInteractors) {
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
	long lastTimerseen=(long)-1>>>1;
	boolean timedWait=false;
	HashMap <Long,PerAddressInfo> addressMap = new HashMap<Long,PerAddressInfo>();
	
	public ThreadState(int tid){
		this.threadIndex = tid;
	}
	
	public void removeDep(int tidApp) {
		//for (PerAddressInfo pai : addressMap.values()) {
		for (Iterator<PerAddressInfo> iter = addressMap.values().iterator(); iter.hasNext();) {
			PerAddressInfo pai = (PerAddressInfo) iter.next();
			pai.probableInteractors.remove((Integer)tidApp);
			if (pai.probableInteractors.size()==0) {
				iter.remove();
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
				LinkedList<Integer> th = new LinkedList<Integer>();
				th.add(thread);
				this.addressMap.put(address,
						new PerAddressInfo(th, time, address));
			}

	}
	public long timeSlept(long address) {
		return addressMap.get(address).timeSinceSlept;
	}
	
}
