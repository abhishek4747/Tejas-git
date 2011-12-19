package emulatorinterface;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

class PerAddressInfo {

	LinkedList<Integer> probableInteractors;
	long timeSinceSlept;
	long address;
	boolean timedWait=false;
	boolean on_broadcast = false;
	long broadcastTime = Long.MAX_VALUE;
	boolean on_barrier = false;

	public PerAddressInfo(LinkedList<Integer> tentativeInteractors,
			long timeSinceSlept,long address,boolean timedWait) {
		super();
		this.probableInteractors = tentativeInteractors;
		this.timeSinceSlept = timeSinceSlept;
		this.address = address;
		this.timedWait = timedWait;
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
	int countTimedSleep=0;
	long lastTimerseen=(long)-1>>>1;
	//boolean timedWait=false;
	HashMap <Long,PerAddressInfo> addressMap = new HashMap<Long,PerAddressInfo>();

	public ThreadState(int tid){
		this.threadIndex = tid;
	}

	public void removeDep(long address) {
		addressMap.remove((Long)address);
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
	}

	public void addDep(long address, long time, int thread) {
		PerAddressInfo opai;
		if ((opai = this.addressMap.get(address)) != null) {
			opai.probableInteractors.add(thread);
			//opai.timeSinceSlept = time;
		} else {
			LinkedList<Integer> th = new LinkedList<Integer>();
			th.add(thread);
			this.addressMap.put(address,
					new PerAddressInfo(th, -1, address, false));
		}

	}
	public long timeSlept(long address) {
		return addressMap.get(address).timeSinceSlept;
	}

	public boolean isOntimedWait() {
		boolean ret = false;
		for (PerAddressInfo pai : addressMap.values()) {
			ret = ret || pai.timedWait;
		}
		return ret;
	}

	public boolean isOntimedWaitAt(long address) {
		if (addressMap.get(address) == null) return false;
		else return addressMap.get(address).timedWait;
	}

	}
