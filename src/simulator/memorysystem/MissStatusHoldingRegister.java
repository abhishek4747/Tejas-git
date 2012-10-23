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
	
	/*
	 * return value signifies whether new omrentry created or not
	 * */
	public boolean addOutstandingRequest(AddressCarryingEvent event);
	
	public ArrayList<Event> removeRequests(AddressCarryingEvent event);
	
	public boolean removeEvent(AddressCarryingEvent addrevent);
	
	public void handleLowerMshrFull( AddressCarryingEvent eventToBeSent);
	
	public boolean containsWriteOfEvictedLine(long address);
	
	public void dump();
}
