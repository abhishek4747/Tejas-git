package memorysystem;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import generic.Event;
import generic.OMREntry;
import generic.RequestType;


public interface MissStatusHoldingRegister {
	
	
	public boolean isFull();
	
	public int getCurrentSize();
	
	public int getMaxSizeReached();
	
	public int getMSHRStructSize();
	
	public int numOutStandingRequests(Event event);
	/*
	 * return value signifies whether new omrentry created or not
	 * */
	public boolean addOutstandingRequest(AddressCarryingEvent event);
	
	public ArrayList<Event> removeRequests(AddressCarryingEvent event);
	
	public boolean removeEvent(AddressCarryingEvent addrevent);
	
	public boolean removeEventIfAvailable(AddressCarryingEvent addrevent);
	
	public ArrayList<Event> removeRequestsIfAvailable(AddressCarryingEvent event);
	
	public void handleLowerMshrFull( AddressCarryingEvent eventToBeSent);
	
	public boolean containsWriteOfEvictedLine(long address);
	
	public void dump();
}
