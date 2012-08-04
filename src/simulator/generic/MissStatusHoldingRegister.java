package generic;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import emulatorinterface.Newmain;

import pipeline.inorder.MemUnitIn;

import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import memorysystem.LSQ;

public class MissStatusHoldingRegister {
	
	
	int offset;
	final int mshrSize;
	Hashtable<Long, OMREntry> mshr;
	int numberOfEntriesReadyToProceed;
	
	public static boolean debugMode =false;
	
	public MissStatusHoldingRegister(int offset, int mshrSize) {
		
		this.offset = offset;
		this.mshrSize = mshrSize;
		mshr = new Hashtable<Long, OMREntry>(mshrSize);
		numberOfEntriesReadyToProceed = 0;
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
	
	public boolean isFull()
	{
		if(getSize() < mshrSize)
		{
			return false;
		}
		else
		{
			if(debugMode) System.out.println("mshr full ; offset = " + offset);
			return true;
		}
	}
	/*
	 * return value signifies whether new omrentry created or not
	 * */
	public boolean addOutstandingRequest(AddressCarryingEvent event)
	{
		if(event.getRequestType() == RequestType.Mem_Response)
		{
			System.err.println("mem response being pushed into the mshr!!" + event.getAddress() + " : " + offset + " : " + event.coreId);
			System.exit(1);
		}
		long addr = event.getAddress();
		long blockAddr = addr >>> offset;
		
		if(debugMode) System.out.println("adding event " + event.getRequestType() + " : " + addr + " : " + blockAddr);
		
		if (!/*NOT*/mshr.containsKey(blockAddr))
		{
			/*
			 * the higher level cache must check if mshr is full before requesting
			if(mshr.size() > mshrSize) {
				System.out.println("mshr full");
				return 2;
			}
			*/
			if(debugMode) System.out.println("creating new omr entry for blockAddr = " + blockAddr);
			OMREntry newOMREntry = new OMREntry(new ArrayList<Event>(), false, null);
			newOMREntry.outStandingEvents.add(event);
			mshr.put(blockAddr, newOMREntry);
			return true;
		}
		else
		{
			
			ArrayList<Event> tempList = mshr.get(blockAddr).outStandingEvents;
			if(tempList.size() == 0)
			{
				System.err.println(" outstanding request list empty  ");
				Newmain.dumpAllMSHRs();
				Newmain.dumpAllEventQueues();
				System.exit(1);
			}
			tempList.add(event);
			return false;
		}
	}
	
	public ArrayList<Event> removeRequests(long address)
	{
		long blockAddr = address >>> offset;
		if (!this.mshr.containsKey(blockAddr))
		{
			Newmain.dumpAllMSHRs();		
			Newmain.dumpAllEventQueues();
			
			System.err.println("Memory System Error : An outstanding request not found in the requesting element : " + address + " : " + blockAddr +"  : " + offset);
			//return new ArrayList<Event>();
			System.exit(1);
		}
		ArrayList<Event> outstandingRequestList = this.mshr.remove(blockAddr).outStandingEvents;
		
		for(int i = 0; i < outstandingRequestList.size(); i++)
		{
			AddressCarryingEvent event = (AddressCarryingEvent) outstandingRequestList.get(i);
			if(debugMode) System.out.println("removing event " + event.getRequestType() + " : " + event.getAddress() + " : " + blockAddr);
		}
		
		return outstandingRequestList;
	}
	
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
	
	public boolean contains(long address)
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
	
	public OMREntry getMshrEntry(long address)
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
	
	public void handleLowerMshrFull( AddressCarryingEvent eventToBeSent)
	{
		OMREntry omrEntry =  getMshrEntry(eventToBeSent.getAddress());
		if(omrEntry.eventToForward != null)
		{
			omrEntry.readyToProceed = false;
			return;
		}
		omrEntry.eventToForward = (AddressCarryingEvent) eventToBeSent.clone();
		omrEntry.readyToProceed = true;
		incrementNumberOfEntriesReadyToProceed();
	}
	
	public ArrayList<OMREntry> getElementsReadyToProceed()
	{
		ArrayList<OMREntry> eventsReadyToProceed = new ArrayList<OMREntry>();
		Enumeration<OMREntry> omrEntries = mshr.elements();
		while(omrEntries.hasMoreElements())
		{
			OMREntry omrEntry = omrEntries.nextElement();
			if(omrEntry.readyToProceed)
			{
				if(omrEntry.eventToForward.getRequestType() == RequestType.Mem_Response)
				{
					System.err.println(" mem response in mshr!!!!!!!!!!   ");
					System.exit(1);
				}
				eventsReadyToProceed.add(omrEntry);
			}
		}
		return eventsReadyToProceed;
	}
	
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
				AddressCarryingEvent addrEvent =  (AddressCarryingEvent) omrEntry.outStandingEvents.remove(i);
				if(debugMode)
					System.out.println(" removing from removeStartingWrites  "+  addrEvent.getRequestType() +addrEvent.getAddress() +  " : " + ( addrEvent.getAddress() >>> offset) );
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
	
	public void dump()
	{
		Enumeration<OMREntry> omrEntries = mshr.elements();
		System.out.println("size = " + getSize());
		while(omrEntries.hasMoreElements())
		{
			OMREntry omrEntry = omrEntries.nextElement();
			ArrayList<Event> events = omrEntry.outStandingEvents;
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
}
