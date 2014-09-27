package memorysystem;

import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class AddressCarryingEvent extends Event implements Cloneable
{
	private long address;
	public long event_id;
	public boolean hasArrivedAtDestination = false;	
	public RequestType payloadRequestType;	
	public SimulationElement payloadElement;
	public int hopLength;

	public AddressCarryingEvent(EventQueue eventQ, long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address) {
		super(eventQ, eventTime, requestingElement, processingElement,
				requestType, -1);
		this.address = address;
	}
	
	public AddressCarryingEvent()
	{
		super(null, -1, null, null, RequestType.Cache_Read, -1);
		this.address = -1;
	}
	
	public AddressCarryingEvent(long eventId, EventQueue eventQ, long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address,int coreId) {
		super(eventQ, eventTime, requestingElement, processingElement,
				requestType, coreId);
		this.event_id = eventId;
		this.address = address;
	}
	
	public AddressCarryingEvent updateEvent(EventQueue eventQ, long eventTime, 
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address,int coreId) {
		this.address = address;
		this.coreId = coreId;
		return (AddressCarryingEvent)this.update(eventQ, eventTime, requestingElement, processingElement, requestType);
	}
	
	public AddressCarryingEvent updateEvent(EventQueue eventQ, long eventTime, 
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType) {
		return (AddressCarryingEvent) this.update(eventQ, eventTime, requestingElement, processingElement, requestType);
	}
	
	public long getAddress() {
		return address;
	}

	public void setAddress(long address) {
		this.address = address;
	}
	
	public void dump()
	{
		System.out.println(coreId + " : " + requestType + " : " + requestingElement + " : " + processingElement + " : " + eventTime + " : " + address);
	}
	
	public String toString(){
		String s = (coreId + " : " + requestType + " : " + requestingElement + " : " + processingElement + " : " + eventTime + " : " + address + " # " + serializationID); 
		return s;
	}

	public void setHasArrivedAtDestination(boolean b) {
		hasArrivedAtDestination = b;
	}
	
	public boolean hasArrivedAtDestination() {
		return hasArrivedAtDestination;
	}
	
	public SimulationElement getPayloadElement() {
		return payloadElement;
	}
}
