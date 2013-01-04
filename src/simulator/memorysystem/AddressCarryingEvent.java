package memorysystem;

import java.util.Stack;
import java.util.Vector;

import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class AddressCarryingEvent extends Event implements Cloneable
{
	private long address;
	private Vector<Integer> sourceBankId;
	private Vector<Integer> destinationBankId;
	public long event_id;
	
	public AddressCarryingEvent(EventQueue eventQ, long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address) {
		super(eventQ, eventTime, requestingElement, processingElement,
				requestType, -1);
		this.address = address;
		sourceBankId = null;
		destinationBankId = null;
	}
	
	public Object clone()
    {
        try
        {
            // call clone in Object.
            return super.clone();
        } catch(CloneNotSupportedException e)
        {
            System.out.println("Cloning not allowed.");
            return this;
        }
    }
	public AddressCarryingEvent()
	{
		super(null, -1, null, null, RequestType.Cache_Read, -1);
		this.address = -1;
		sourceBankId = null;
		destinationBankId = null;
	}
	
	public AddressCarryingEvent(EventQueue eventQ, long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address,int coreId,
			Vector<Integer> sourceBankId, Vector<Integer> destinationBankId) {
		super(eventQ, eventTime, requestingElement, processingElement,
				requestType, coreId);
		this.address = address;
		this.sourceBankId = (Vector<Integer>) sourceBankId.clone();
		this.destinationBankId = (Vector<Integer>) destinationBankId.clone();
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
			Vector<Integer> sourceBankId, Vector<Integer> destinationBankId) {
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
			Vector<Integer> sourceBankId,
			Vector<Integer> destinationBankId) {
		this.sourceBankId = (Vector<Integer>) sourceBankId.clone();
		this.destinationBankId = (Vector<Integer>) destinationBankId.clone();
		return (AddressCarryingEvent) this.update(eventQ, eventTime, requestingElement, processingElement, requestType);
	}
	
	public long getAddress() {
		return address;
	}

	public void setAddress(long address) {
		this.address = address;
	}

	public void setSourceBankId(Vector<Integer> sourceBankId) {
		this.sourceBankId = (Vector<Integer>) sourceBankId.clone();
	}

	public Vector<Integer> getSourceBankId() {
		return sourceBankId;
	}

	public void setDestinationBankId(Vector<Integer> destinationBankId) {
		this.destinationBankId = (Vector<Integer>) destinationBankId.clone();
	}

	public Vector<Integer> getDestinationBankId() {
		return destinationBankId;
	}
	
	public void dump()
	{
		System.out.println(coreId + " : " + requestType + " : " + requestingElement + " : " + processingElement + " : " + eventTime + " : " + address);
	}
	
	public String toString(){
		String s = (coreId + " : " + requestType + " : " + requestingElement + " : " + processingElement + " : " + eventTime + " : " + address); 
		return s;
	}
}
