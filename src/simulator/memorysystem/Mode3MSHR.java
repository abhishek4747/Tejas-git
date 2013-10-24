package memorysystem;

import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.OMREntry;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import memorysystem.nuca.NucaCacheBank;

//TODO needs huge fixes!

public class Mode3MSHR extends SimulationElement implements MissStatusHoldingRegister{
	
	int offset;
	final int mshrStructSize;
	Hashtable<Long, OMREntry> mshr;
	public int maxSizeReached = Integer.MIN_VALUE;
	
	public Mode3MSHR(int offset, int mshrSize, EventQueue eventQ) {
		
		super(PortType.Unlimited, -1, -1, // Port parameter (type, numPorts, occupancy)
				eventQ, -1, -1);          // Simulation element (event queue, latency, frequency)
		
		this.offset = offset;
		this.mshrStructSize = mshrSize;
		mshr = new Hashtable<Long, OMREntry>(mshrSize);
	}
	
	public int getCurrentSize()
	{
		int currentSize = 0;
		Enumeration<OMREntry> omrIte = mshr.elements();
		while(omrIte.hasMoreElements())
		{
			OMREntry omrEntry = omrIte.nextElement();
			currentSize += omrEntry.outStandingEvents.size();
		}
		
		if(currentSize>maxSizeReached) {
			maxSizeReached = currentSize;
		}
		
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
//		System.out.println("addRequestMSHR\ttime = " + GlobalClock.getCurrentTime() + 
//				"\tevent = " + eventAdded);
		AddressCarryingEvent event = (AddressCarryingEvent)eventAdded.clone();
		
		//System.out.println("Adding outstanding request : " + event);
		long addr = event.getAddress();
		long blockAddr = addr >>> offset;
		
		if (!/*NOT*/mshr.containsKey(blockAddr))
		{
			/*
			 * the higher level cache must check if mshr is full before requesting
			 */
			OMREntry newOMREntry = new OMREntry(new ArrayList<AddressCarryingEvent>(), null);
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
	public ArrayList<AddressCarryingEvent> removeRequestsByAddress(AddressCarryingEvent event)
	{
		long address = event.getAddress();
		long blockAddr = address >>> offset;
		
		OMREntry entry = this.mshr.remove(blockAddr);
		if(entry==null) {
			misc.Error.showErrorAndExit("event not in MSHR : " + event);
			return null;
		} else {
			Event removedEvent = entry.outStandingEvents.get(0);
			// This update event method is just a copy of Mode1MSHR's code
			event.update(removedEvent.getEventQ(),
					0,
					removedEvent.getRequestingElement(),
					removedEvent.getProcessingElement(),
					removedEvent.getRequestType()
					);
			
			return entry.outStandingEvents;
		}
	}
	
	@Override
	public boolean removeRequestsByRequestTypeAndAddress(AddressCarryingEvent addrevent)
	{
		long addr = addrevent.getAddress();
		long blockAddr = addr >>> offset;
		RequestType requestType = addrevent.getRequestType();
		
		OMREntry omrEntry = mshr.get(blockAddr);
		
		for(int i=0; i<omrEntry.outStandingEvents.size(); i++) {
			if (omrEntry.outStandingEvents.get(i).getAddress()>>>offset==blockAddr &&
					omrEntry.outStandingEvents.get(i).getRequestType()==requestType)
			{
				omrEntry.outStandingEvents.remove(i);
				if(omrEntry.outStandingEvents.size()==0 && omrEntry.eventToForward==null) {
					mshr.remove(blockAddr);
				}
				return true;
			}
		}
		
		return false;
	}
	
	boolean contains(long address)
	{
		long blockaddr = address >>> offset;
		if( mshr.containsKey(blockaddr) )
		{
			return true;
		}
		else 
		{
			return false;
		}
	}
	
	OMREntry getMshrEntry(long address)
	{
		long blockaddr = address >>> offset;
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
	public void handleLowerMshrFull( AddressCarryingEvent eventToBeSent)
	{
		OMREntry omrEntry =  getMshrEntry(eventToBeSent.getAddress());
		if(omrEntry.eventToForward != null)
		{
			return;
		}
		omrEntry.eventToForward = eventToBeSent;
		
		// Try in the next cycle
		eventToBeSent.actualRequestType = eventToBeSent.getRequestType();
		eventToBeSent.setRequestType(RequestType.MSHR_Full);
		eventToBeSent.setEventTime(eventToBeSent.getEventTime() +1);
		eventToBeSent.getEventQ().addEvent(eventToBeSent);
	}
	
	ArrayList<OMREntry> getElementsReadyToProceed()
	{
		ArrayList<OMREntry> eventsReadyToProceed = new ArrayList<OMREntry>();
		Enumeration<OMREntry> omrEntries = mshr.elements();
		while(omrEntries.hasMoreElements())
		{
			OMREntry omrEntry = omrEntries.nextElement();
			if(omrEntry.eventToForward!=null)
			{
				eventsReadyToProceed.add(omrEntry);
			}
		}
		return eventsReadyToProceed;
	}
	
	ArrayList<OMREntry> getElementsReadyToProceed(Vector<Integer> bankId)
	{
		ArrayList<OMREntry> eventsReadyToProceed = new ArrayList<OMREntry>();
		Enumeration<OMREntry> omrEntries = mshr.elements();
		while(omrEntries.hasMoreElements())
		{
			OMREntry omrEntry = omrEntries.nextElement();
			if(omrEntry.eventToForward!=null && omrEntry.eventToForward.getDestinationId() != null && omrEntry.eventToForward.getDestinationId().equals(bankId))
			{
				eventsReadyToProceed.add(omrEntry);
			}
		}
		return eventsReadyToProceed;
	}
	
	@Override
	public boolean containsWriteOfEvictedLine(long address)
	{
		if(true) {
			return false;
		}
		//if the MSHR contains a write to given address
		// AND if the eventToForward of the omrEntry is a Write
		//  then
		//    this either refers to a write to a block that is contained in the cache
		//    OR this refers to an evicted block
		OMREntry omrEntry = getMshrEntry(address >>> offset);
		if(omrEntry != null && omrEntry.containsWriteToAddress(address))
		{
			return true;
		}
		else
		{
			return false;
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
			
			/*if(events.size() == 0)
			{
				System.err.println(" outstanding event empty ");
				continue;
			}*/
			AddressCarryingEvent addrEvent = (AddressCarryingEvent) events.get(0);
			System.out.print("block address = " + (addrEvent.getAddress() >>> offset));
			if(omrEntry.eventToForward != null)
			{
				System.out.print(" : " + omrEntry.eventToForward.getRequestType() + " : " + (omrEntry.eventToForward!=null) );
			}
			System.out.println();
			for(int i = 0; i < events.size(); i++)
			{
				addrEvent = (AddressCarryingEvent) events.get(i);				
				System.out.print(addrEvent.getAddress() + "," + addrEvent.getRequestType() + "," + addrEvent.coreId + "\t");
			}
			System.out.println();
		}
	}
	/*methods with errors TODO
	public void pullFromUpperMshrs()
	{
		startIndexForPulling = (startIndexForPulling + 1)%connectedMSHR.size();
		
		for(int i = 0, j = startIndexForPulling;
			i < connectedMSHR.size();   
			i++, j = (j+1)%connectedMSHR.size())
		{
			pullFrom(connectedMSHR.get(j));				
		}
	}
	
	public void pullFrom(MissStatusHoldingRegister mshr)
	{
		if(mshr.getNumberOfEntriesReadyToProceed() == 0)
		{
			return;
		}
		
		ArrayList<OMREntry> eventToProceed = mshr.getElementsReadyToProceed();
		processReadyEvents(eventToProceed,mshr);
	}
	
	public void processReadyEvents(ArrayList<OMREntry> eventToProceed,MissStatusHoldingRegister mshr)
	{
		for(int k = 0;k < eventToProceed.size();k++)
		{
			if(missStatusHoldingRegister.isFull())
			{
				break;
			}
			
			OMREntry omrEntry = eventToProceed.get(k);
			omrEntry.readyToProceed = false;
			
			boolean entryCreated = missStatusHoldingRegister.addOutstandingRequest(omrEntry.eventToForward);//####
			
			if(omrEntry.eventToForward.getRequestType() == RequestType.Cache_Write)
			{
				mshr.removeStartingWrites(omrEntry.eventToForward.getAddress());
			}
			
			mshr.decrementNumberOfEntriesReadyToProceed();

			if(this.getClass() == NucaCacheBank.class)
			{
				if (omrEntry.eventToForward.getDestinationBankId() == null || omrEntry.eventToForward.getSourceBankId() == null)
				{
					System.out.println("error from pulling ");
				}
			}
			if(entryCreated)
			{
				*//*
				 * if the pulled event results in a new omrEntry,
				 * the processing of the request must be done
				 *//*
				handleAccess(omrEntry.eventToForward.getEventQ() , omrEntry.eventToForward);
			}
			else
			{
				*//*
				 * if the pulled event is a write (omr entry already exists),
				 * it may be that the cache line already exists at this level (either in the cache, or in the MSHR),
				 * therefore, this request is effectively a hit;
				 * to handle this possibility, we call handleAccess()
				 *//*
				AddressCarryingEvent eventToForward = missStatusHoldingRegister.getMshrEntry(omrEntry.eventToForward.getAddress()).eventToForward; 
				if(eventToForward != null &&
						eventToForward.getRequestType() == RequestType.Cache_Write)
				{
					handleAccess(omrEntry.eventToForward.getEventQ(), omrEntry.eventToForward);
				}
			}
		}
	}
	*/

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
		//only receives RequestType.PerformPulls
		
		/*TODO error in this line. pullFromUpperMshrs();*/
		
		//schedule pulling for the next cycle
		event.addEventTime(1);
		event.getEventQ().addEvent(event);
		
	}

	@Override
	public int getMSHRStructSize() {
		return mshrStructSize;
	}

	@Override
	public int getMaxSizeReached() {
		return maxSizeReached;
	}

	@Override
	public int numOutStandingRequests(AddressCarryingEvent event) {
		long addr = event.getAddress();
		long dirAddr = addr>>>offset;
		
		OMREntry entry = mshr.get(dirAddr);
		
		if(entry==null) {
			return 0;
		} else {
			return entry.outStandingEvents.size();
		}
	}

	@Override
	public boolean removeRequestsByRequestTypeAndAddressIfAvailable(AddressCarryingEvent addrevent) {
		return removeRequestsByRequestTypeAndAddress(addrevent);
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
			// This update event method is just a copy of Mode1MSHR's code
			event.update(removedEvent.getEventQ(),
					0,
					removedEvent.getRequestingElement(),
					removedEvent.getProcessingElement(),
					removedEvent.getRequestType()
					);
			return entry.outStandingEvents;
		}
	}

}
