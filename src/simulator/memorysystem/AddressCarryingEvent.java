package memorysystem;

import java.util.Stack;
import java.util.Vector;

import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class AddressCarryingEvent extends Event
{
	private long address;
	private Vector<Integer> sourceBankId;
	private Vector<Integer> destinationBankId;
	public RequestType oldRequestType;
	public SimulationElement oldRequestingElement;
	public Vector<Integer> oldSourceBankId;
	public boolean copyLine;
	public int coreId;
	public Stack<SimulationElement> requestingElementStack = new Stack<SimulationElement>();
	public Stack<RequestType> requestTypeStack = new Stack<RequestType>();
	public AddressCarryingEvent(EventQueue eventQ, long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address) {
		super(eventQ, eventTime, requestingElement, processingElement,
				requestType);
		this.address = address;
		sourceBankId = null;
		destinationBankId = null;
		oldSourceBankId = null;
		copyLine = false;
	}

	public AddressCarryingEvent(EventQueue eventQ, long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address,int coreId) {
		super(eventQ, eventTime, requestingElement, processingElement,
				requestType);
		this.address = address;
		sourceBankId = null;
		destinationBankId = null;
		oldSourceBankId = null;
		copyLine = false;
		this.coreId = coreId;
	}
	
	public AddressCarryingEvent updateEvent(EventQueue eventQ, long eventTime, 
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address) {
		this.address = address;
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
}
