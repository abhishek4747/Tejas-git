package memorysystem;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import generic.Event;
import generic.OMREntry;
import generic.RequestType;
import generic.SimulationElement;


public interface MissStatusHoldingRegister {
	
	
	public boolean isFull();
	
	public int getCurrentSize();
	
	public int getMaxSizeReached();
	
	public int getMSHRStructSize();
	
	public int numOutStandingRequests(AddressCarryingEvent event);
	/*
	 * return value signifies whether new omrentry created or not
	 * */
	public boolean addOutstandingRequest(AddressCarryingEvent event);
	
	public ArrayList<AddressCarryingEvent> removeRequestsByAddress(AddressCarryingEvent event);
	
	public boolean removeRequestsByRequestTypeAndAddress(AddressCarryingEvent addrevent);
	
	public boolean removeRequestsByRequestTypeAndAddressIfAvailable(AddressCarryingEvent addrevent);
	
	public ArrayList<AddressCarryingEvent> removeRequestsByAddressIfAvailable(AddressCarryingEvent event);
	
	public void handleLowerMshrFull( AddressCarryingEvent eventToBeSent);
	
	public boolean containsWriteOfEvictedLine(long address);
	
	public void dump();
}
