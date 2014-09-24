package memorysystem;


import net.ID;

import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class AddressCarryingEvent extends Event implements Cloneable
{
	private long address;
	private ID sourceId;
	private ID destinationId;
	public long event_id;
	
	public RequestType actualRequestType;
	
	public SimulationElement actualProcessingElement;
	public int hopLength;

	public AddressCarryingEvent(EventQueue eventQ, long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address) {
		super(eventQ, eventTime, requestingElement, processingElement,
				requestType, -1);
		this.address = address;
		sourceId = null;
		destinationId = null;
	}
	
	public AddressCarryingEvent()
	{
		super(null, -1, null, null, RequestType.Cache_Read, -1);
		this.address = -1;
		sourceId = null;
		destinationId = null;
	}
	
	public AddressCarryingEvent(EventQueue eventQ, long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address,int coreId,
			ID sourceId, ID destinationId) {
		super(eventQ, eventTime, requestingElement, processingElement,
				requestType, coreId);
		this.address = address;
		this.sourceId = new ID(sourceId.getx(), sourceId.gety());
		this.destinationId = new ID(destinationId.getx(),destinationId.gety());
	}
	public AddressCarryingEvent(long eventId, EventQueue eventQ, long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address,int coreId,
			ID sourceId, ID destinationId) {
		super(eventQ, eventTime, requestingElement, processingElement,
				requestType, coreId);
		this.event_id = eventId;
		this.address = address;
		this.sourceId = new ID(sourceId.getx(), sourceId.gety());
		this.destinationId = new ID(destinationId.getx(),destinationId.gety());
	}
	public AddressCarryingEvent(EventQueue eventQ, long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address,int coreId) {
		super(eventQ, eventTime, requestingElement, processingElement,
				requestType, coreId);
		this.address = address;
	}
	
	public AddressCarryingEvent updateEvent(EventQueue eventQ, long eventTime, 
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address,int coreId,
			ID sourceId, ID destinationId) {
		this.address = address;
		this.coreId = coreId;
		return (AddressCarryingEvent)this.update(eventQ, eventTime, requestingElement, processingElement, requestType);
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
			RequestType requestType, 
			ID sourceId,
			ID destinationId) {
		this.sourceId = new ID(sourceId.getx(), sourceId.gety());
		this.destinationId = new ID(destinationId.getx(),destinationId.gety());
		return (AddressCarryingEvent) this.update(eventQ, eventTime, requestingElement, processingElement, requestType);
	}
	
	public long getAddress() {
		return address;
	}

	public void setAddress(long address) {
		this.address = address;
	}

	public void setSourceId(ID sourceId) {
		this.sourceId = new ID(sourceId.getx(), sourceId.gety());
	}

	public ID getSourceId() {
		return sourceId;
	}

	public void setDestinationId(ID destinationId) {
		this.destinationId = new ID(destinationId.getx(),destinationId.gety());
	}

	public ID getDestinationId() {
		return destinationId;
	}
	
	public void dump()
	{
		System.out.println(coreId + " : " + requestType + " : " + requestingElement + " : " + processingElement + " : " + eventTime + " : " + address);
	}
	
	public String toString(){
		String s = (coreId + " : " + requestType + " : " + requestingElement + " : " + processingElement + " : " + eventTime + " : " + address + " # " + serializationID); 
		return s;
	}
}
