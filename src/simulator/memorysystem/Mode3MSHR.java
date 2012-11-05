package memorysystem;

import generic.Event;
import generic.EventQueue;
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
	final int mshrSize;
	Hashtable<Long, OMREntry> mshr;
	int numberOfEntriesReadyToProceed;

	public ArrayList<Mode3MSHR> connectedMSHR;
	public int startIndexForPulling;
	
	public Mode3MSHR(int offset, int mshrSize, EventQueue eventQ) {
		
		super(PortType.Unlimited,
				-1,
				-1,
				eventQ,
				-1,
				-1);
		
		this.offset = offset;
		this.mshrSize = mshrSize;
		mshr = new Hashtable<Long, OMREntry>(mshrSize);
		numberOfEntriesReadyToProceed = 0;
		
		connectedMSHR = new ArrayList<Mode3MSHR>();
		startIndexForPulling = 0;
	}
	
	int getSize()
	{
		int currentSize = 0;
		Enumeration<OMREntry> omrIte = mshr.elements();
		while(omrIte.hasMoreElements())
		{
			OMREntry omrEntry = omrIte.nextElement();
			currentSize += omrEntry.outStandingEvents.size();
		}
		return currentSize;
	}
	
	@Override
	public boolean isFull()
	{
		if(getSize() < mshrSize)
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
	public boolean addOutstandingRequest(AddressCarryingEvent event)
	{
		long addr = event.getAddress();
		long blockAddr = addr >>> offset;
		
		if (!/*NOT*/mshr.containsKey(blockAddr))
		{
			/*
			 * the higher level cache must check if mshr is full before requesting
			 */
			OMREntry newOMREntry = new OMREntry(new ArrayList<Event>(), false, null);
			newOMREntry.outStandingEvents.add(event);
			mshr.put(blockAddr, newOMREntry);
			return true;
		}
		else
		{			

			ArrayList<Event> tempList = mshr.get(blockAddr).outStandingEvents;
			tempList.add(event);
			return false;
		}
	}
	
	@Override
	public ArrayList<Event> removeRequests(AddressCarryingEvent event)
	{
		long address = event.getAddress();
		long blockAddr = address >>> offset;
		ArrayList<Event> outstandingRequestList = this.mshr.remove(blockAddr).outStandingEvents;
		return outstandingRequestList;
	}
	
	@Override
	public boolean removeEvent(AddressCarryingEvent addrevent)
	{
		long addr = addrevent.getAddress();
		long blockAddr = addr >>> offset;
		OMREntry omrEntry = mshr.get(blockAddr);
		if(omrEntry.outStandingEvents.contains(addrevent))
		{
			omrEntry.outStandingEvents.remove(addrevent);
			if(omrEntry.outStandingEvents.size() == 0)
			{
				mshr.remove(blockAddr);
			}
			return true;
		}
		else
		{
			return false;
		}
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
		omrEntry.readyToProceed = true;
		incrementNumberOfEntriesReadyToProceed();
	}
	
	ArrayList<OMREntry> getElementsReadyToProceed()
	{
		ArrayList<OMREntry> eventsReadyToProceed = new ArrayList<OMREntry>();
		Enumeration<OMREntry> omrEntries = mshr.elements();
		while(omrEntries.hasMoreElements())
		{
			OMREntry omrEntry = omrEntries.nextElement();
			if(omrEntry.readyToProceed)
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
			if(omrEntry.readyToProceed && omrEntry.eventToForward.getDestinationBankId() != null && omrEntry.eventToForward.getDestinationBankId().equals(bankId))
			{
				eventsReadyToProceed.add(omrEntry);
			}
		}
		return eventsReadyToProceed;
	}
	
	@Override
	public boolean containsWriteOfEvictedLine(long address)
	{
		//if the MSHR contains a write to given address
		// AND if the eventToForward of the omrEntry is a Write
		//  then
		//    this either refers to a write to a block that is contained in the cache
		//    OR this refers to an evicted block
		OMREntry omrEntry = getMshrEntry(address >>> offset);
		if(omrEntry != null && omrEntry.containsWrite())
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/*
	 * at the time of pulling, and eventToForward being pulled is of type WRITE
	 * case 1 : if pulling from another cache
	 * 		the omrEntry can only consist of writes. since these are being pulled,
	 * 		omrEntry can be removed
	 * case 2 : if pulling from pipeline
	 * 		the omrEntry can consist of reads and writes, but it starts with a write
	 * 		here, we remove all the contiguous writes that appear at the beginning
	 * 		from the first read onwards, the events are allowed to remain
	 * 		if the omrEntry is empty, delete it
	 * 		else change eventToForward to READ
	 */
	public void removeStartingWrites(long address)
	{
		OMREntry omrEntry = getMshrEntry(address);
		boolean readFound = false;
		AddressCarryingEvent sampleReadEvent = null;
		
		int listSize = omrEntry.outStandingEvents.size();
		for(int i = 0; i < listSize; i++)
		{
			if(omrEntry.outStandingEvents.get(i).getRequestType() == RequestType.Cache_Write)
			{
				omrEntry.outStandingEvents.remove(i);
				i--;
				listSize--;
			}
			else if(omrEntry.outStandingEvents.get(i).getRequestType() == RequestType.Cache_Read)
			{
				readFound = true;
				sampleReadEvent = (AddressCarryingEvent) omrEntry.outStandingEvents.get(i);
				break;
			}
		}
		
		if(readFound == false)
		{
			mshr.remove(address >>> offset);
		}
		else
		{
			omrEntry.eventToForward = (AddressCarryingEvent) sampleReadEvent.clone();
			omrEntry.readyToProceed = true;
			incrementNumberOfEntriesReadyToProceed();
		}
	}
	
	public void incrementNumberOfEntriesReadyToProceed()
	{
		numberOfEntriesReadyToProceed++;
	}
	
	public void decrementNumberOfEntriesReadyToProceed()
	{
		numberOfEntriesReadyToProceed--;
	}
	
	public int getNumberOfEntriesReadyToProceed()
	{
		return numberOfEntriesReadyToProceed;
	}
	
	@Override
	public void dump()
	{
		Enumeration<OMREntry> omrEntries = mshr.elements();
		System.out.println("size = " + getSize());
		while(omrEntries.hasMoreElements())
		{
			OMREntry omrEntry = omrEntries.nextElement();
			ArrayList<Event> events = omrEntry.outStandingEvents;
			System.out.println("no of entries ready to proceed = " + numberOfEntriesReadyToProceed);
			/*if(events.size() == 0)
			{
				System.err.println(" outstanding event empty ");
				continue;
			}*/
			AddressCarryingEvent addrEvent = (AddressCarryingEvent) events.get(0);
			System.out.print("block address = " + (addrEvent.getAddress() >>> offset));
			if(omrEntry.eventToForward != null)
			{
				System.out.print(" : " + omrEntry.eventToForward.getRequestType() + " : " + omrEntry.readyToProceed );
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
				/*
				 * if the pulled event results in a new omrEntry,
				 * the processing of the request must be done
				 */
				handleAccess(omrEntry.eventToForward.getEventQ() , omrEntry.eventToForward);
			}
			else
			{
				/*
				 * if the pulled event is a write (omr entry already exists),
				 * it may be that the cache line already exists at this level (either in the cache, or in the MSHR),
				 * therefore, this request is effectively a hit;
				 * to handle this possibility, we call handleAccess()
				 */
				AddressCarryingEvent eventToForward = missStatusHoldingRegister.getMshrEntry(omrEntry.eventToForward.getAddress()).eventToForward; 
				if(eventToForward != null &&
						eventToForward.getRequestType() == RequestType.Cache_Write)
				{
					handleAccess(omrEntry.eventToForward.getEventQ(), omrEntry.eventToForward);
				}
			}
		}
	}
	
	public void populateConnectedMSHR(ArrayList<Cache> prevLevel)
	{
		for(int i = 0; i < prevLevel.size(); i++)
		{
			connectedMSHR.add(i, (Mode3MSHR)prevLevel.get(i).getMissStatusHoldingRegister());
		}
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
		//only receives RequestType.PerformPulls
		
		pullFromUpperMshrs();
		
		//schedule pulling for the next cycle
		event.addEventTime(1);
		event.getEventQ().addEvent(event);
		
	}

}
