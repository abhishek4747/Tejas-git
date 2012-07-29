package generic;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import pipeline.inorder.MemUnitIn;

import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.LSQ;

public class MissStatusHoldingRegister {
	
	
	int offset;
	final int mshrSize;
	Hashtable<Long, OMREntry> mshr;
	
	public MissStatusHoldingRegister(int offset, int mshrSize) {
		
		this.offset = offset;
		this.mshrSize = mshrSize;
		mshr = new Hashtable<Long, OMREntry>(mshrSize);
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
			return true;
		}
	}
	/*
	 * return value signifies whether new omrentry created or not
	 * */
	public boolean addOutstandingRequest(AddressCarryingEvent event)
	{
		long addr = event.getAddress();
		long blockAddr = addr >>> offset;
		
		if (!/*NOT*/mshr.containsKey(blockAddr))
		{
			/*
			 * the higher level cache must check if mshr is full before requesting
			if(mshr.size() > mshrSize) {
				System.out.println("mshr full");
				return 2;
			}
			*/
			OMREntry newOMREntry = new OMREntry(new ArrayList<Event>(), false, null);
			newOMREntry.outStandingEvents.add(event);
			mshr.put(blockAddr, newOMREntry);
			return true;
		}
		else
		{
			mshr.get(blockAddr).outStandingEvents.add(event);
			return false;
		}
	}
	
	public ArrayList<Event> removeRequests(long address)
	{
		long blockAddr = address >>> offset;
		if (!this.mshr.containsKey(blockAddr))
		{
			System.err.println("Memory System Error : An outstanding request not found in the requesting element");
			System.exit(1);
		}
		ArrayList<Event> outstandingRequestList = this.mshr.remove(blockAddr).outStandingEvents;
		return outstandingRequestList;
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
		return mshr.get(blockaddr);
	}
	
	public void handleLowerMshrFull( AddressCarryingEvent eventToBeSent)
	{
		OMREntry omrEntry =  getMshrEntry(eventToBeSent.getAddress());
		omrEntry.eventToForward = eventToBeSent;
		omrEntry.readyToProceed = true;
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
				eventsReadyToProceed.add(omrEntry);
			}
		}
		return eventsReadyToProceed;
	}
}
