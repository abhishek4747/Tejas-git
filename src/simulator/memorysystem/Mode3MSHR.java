package memorysystem;

import generic.Event;
import generic.EventQueue;
import generic.OMREntry;
import generic.PortType;
import generic.SimulationElement;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

public class Mode3MSHR extends SimulationElement implements MissStatusHoldingRegister{
	
	int offset;
	final int mshrStructSize;
	int currentSize;
	Hashtable<Long, OMREntry> mshr;
	
	/*
	 * offset, cacheLatency and numCachePorts are containing cache parameters
	 * mshrSize is the maximum legal size of the MSHR
	 * NOTE : an approximation is employed here -- a cache's requesting element
	 * checks if the MSHR is full before issuing a request. however, the request
	 * processing is done after 'latency' cycles from the time of the request.
	 * during this time, the cache's MSHR may fill up. we allow the MSHR to grow
	 * beyond its specified size in this scenario. 
	 */
	public Mode3MSHR(int offset, int mshrSize, EventQueue eventQ) {
		
		super(PortType.Unlimited, -1, -1, // Port parameter (type, numPorts, occupancy)
				eventQ, -1, -1);          // Simulation element (event queue, latency, frequency)
		
		this.offset = offset;
		this.mshrStructSize = mshrSize;
		this.currentSize = 0;
		mshr = new Hashtable<Long, OMREntry>(mshrSize);
	}
	
	public int getCurrentSize()
	{
		return currentSize;
	}
	
	@Override
	public boolean isFull()
	{
		if(getCurrentSize() < mshrStructSize)
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	/*
	 * return value signifies whether new omrentry created or not
	 * */
	@Override
	public boolean addOutstandingRequest(AddressCarryingEvent eventAdded)
	{
		currentSize++;
		
		AddressCarryingEvent event = (AddressCarryingEvent)eventAdded.clone();
		long addr = event.getAddress();
		long blockAddr = addr >>> offset;
		
		if (!/*NOT*/mshr.containsKey(blockAddr))
		{
			/*
			 * the higher level cache must check if mshr is full before requesting
			 */
			OMREntry newOMREntry = new OMREntry(new ArrayList<AddressCarryingEvent>());
			newOMREntry.outStandingEvents.add(event);
			mshr.put(blockAddr, newOMREntry);
			return true;
		}
		else
		{			
			ArrayList<AddressCarryingEvent> tempList = mshr.get(blockAddr).outStandingEvents;
			tempList.add(event);
			return false;
		}
	}
	
	@Override
	public ArrayList<AddressCarryingEvent> removeRequestsByAddress(long addr)
	{
		long blockAddr = addr >>> offset;
		
		OMREntry entry = this.mshr.remove(blockAddr);
		if(entry==null) {
			misc.Error.showErrorAndExit("event not in MSHR for address : " + addr);
			return null;
		} else {
			currentSize -= entry.outStandingEvents.size();
			return entry.outStandingEvents;
		}
	}
	
	boolean contains(long addr)
	{
		long blockaddr = addr >>> offset;
		if( mshr.containsKey(blockaddr) )
		{
			return true;
		}
		else 
		{
			return false;
		}
	}
	
	OMREntry getMshrEntry(long addr)
	{
		long blockaddr = addr >>> offset;
		if(mshr.containsKey(blockaddr))
		{
			return mshr.get(blockaddr);
		}
		else
		{
			return null;
		}
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
			
	}

	@Override
	public int getMSHRStructSize() {
		return mshrStructSize;
	}

	@Override
	public int numOutStandingRequests(long addr) {
		long dirAddr = addr>>>offset;
		
		OMREntry entry = mshr.get(dirAddr);
		
		if(entry==null) {
			return 0;
		} else {
			return entry.outStandingEvents.size();
		}
	}

	@Override
	public ArrayList<AddressCarryingEvent> removeRequestsByAddressIfAvailable(AddressCarryingEvent event) {
		long address = event.getAddress();
		long blockAddr = address >>> offset;
		
		OMREntry entry = this.mshr.remove(blockAddr);
		if(entry==null) {
			return null;
		} else {		
			Event removedEvent = entry.outStandingEvents.get(0);
			event.update(removedEvent.getEventQ(),
					0,
					removedEvent.getRequestingElement(),
					removedEvent.getProcessingElement(),
					removedEvent.getRequestType()
					);
			currentSize -= entry.outStandingEvents.size();
			return entry.outStandingEvents;
		}
	}
	
	@Override
	public void dump()
	{
		Enumeration<OMREntry> omrEntries = mshr.elements();
		System.out.println("size = " + getCurrentSize());
		while(omrEntries.hasMoreElements())
		{
			OMREntry omrEntry = omrEntries.nextElement();
			ArrayList<AddressCarryingEvent> events = omrEntry.outStandingEvents;
			
			AddressCarryingEvent addrEvent = (AddressCarryingEvent) events.get(0);
			System.out.print("block address = " + (addrEvent.getAddress() >>> offset));
			for(int i = 0; i < events.size(); i++)
			{
				addrEvent = (AddressCarryingEvent) events.get(i);				
				System.out.println(addrEvent);
			}
			System.out.println();
		}
	}
}
