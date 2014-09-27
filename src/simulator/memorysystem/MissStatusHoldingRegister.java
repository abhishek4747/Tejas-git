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
	
	public int getMSHRStructSize();
	
	public int numOutStandingRequests(long addr);
	/*
	 * return value signifies whether new omrentry created or not
	 * */
	public boolean addOutstandingRequest(AddressCarryingEvent event);
	
	public ArrayList<AddressCarryingEvent> removeRequestsByAddress(long addr);
	
	public ArrayList<AddressCarryingEvent> removeRequestsByAddressIfAvailable(AddressCarryingEvent event);
	
	public void dump();	
}
