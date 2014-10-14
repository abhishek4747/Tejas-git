package memorysystem;

import java.util.LinkedList;

public class MSHR {
	private int mshrMaxSize;
	private LinkedList<LinkedList<AddressCarryingEvent>> missRegister;
	private int blockSizeBits;
	
	public MSHR(int mshrMaxSize, int blockSizeBits) {
		this.mshrMaxSize = mshrMaxSize;
		missRegister = new LinkedList<LinkedList<AddressCarryingEvent>>();
		this.blockSizeBits = blockSizeBits;
	}

	public void addToMSHR(AddressCarryingEvent event) {
		long addr = event.getAddress();

		// First try to add it to the linked list of addr
		// If the linked list is not there, create a new MSHR entry
		LinkedList<AddressCarryingEvent> missList = getWaitingEventsInMSHR(addr);
		if (missList != null) {
			missList.add(event);
		} else {
			createMSHREntry(event);
		}
	}

	public LinkedList<AddressCarryingEvent> removeEventsFromMSHR(long addr) {
		LinkedList<AddressCarryingEvent> missList = getWaitingEventsInMSHR(addr);
		if (missList == null) {
			misc.Error.showErrorAndExit("No pending event in MSHR for addr : "
					+ addr + ". Cache : " + this);
		}

		missRegister.remove(missList);

		return missList;
	}

	public boolean isAddrInMSHR(long addr) {
		return (getWaitingEventsInMSHR(addr) != null);
	}

	public int sizeMSHR() {
		int size = 0;
		for (LinkedList<AddressCarryingEvent> missList : missRegister) {
			size += missList.size();
		}
		return size;
	}

	public boolean isMSHRFull() {
		return (sizeMSHR() >= mshrMaxSize);
	}

	public void printMSHR() {
		System.out.println("\nMSHR of " + this + "\n");
		for (LinkedList<AddressCarryingEvent> missList : missRegister) {
			System.out.println("\nLineAddr : " + getLineAddr(missList.peek().getAddress()));
			for (AddressCarryingEvent event : missList) {
				System.out.println(event);
			}
		}
	}

	public void createMSHREntry(AddressCarryingEvent event) {
		LinkedList<AddressCarryingEvent> missList = new LinkedList<AddressCarryingEvent>();
		missList.add(event);
		missRegister.add(missList);
	}
	
	private long getLineAddr(long addr) {
		return (addr>>blockSizeBits);
	}

	public LinkedList<AddressCarryingEvent> getWaitingEventsInMSHR(long addr) {
		long lineAddr = getLineAddr(addr);
		for (LinkedList<AddressCarryingEvent> missList : missRegister) {
			if (getLineAddr(missList.peek().getAddress()) == lineAddr) {
				return missList;
			}
		}

		return null;
	}
	
	public AddressCarryingEvent getFirstWaitingEventInMSHR(long addr) {
		long lineAddr = getLineAddr(addr);
		for (LinkedList<AddressCarryingEvent> missList : missRegister) {
			if (getLineAddr(missList.peek().getAddress()) == lineAddr) {
				return missList.getFirst();
			}
		}

		misc.Error.showErrorAndExit("No event for addr : " + addr);
		return null;
	}

	public void removeFirstEventFromMSHR(long addr) {
		long lineAddr = getLineAddr(addr);
		for (LinkedList<AddressCarryingEvent> missList : missRegister) {
			if (getLineAddr(missList.peek().getAddress()) == lineAddr) {
				missList.remove(0);
				return;
			}
		}
		
		misc.Error.showErrorAndExit("No event for addr : " + addr);
	}
}
