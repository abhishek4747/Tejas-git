package memorysystem;

import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

import java.util.Vector;


public class DestinationBankEvent extends AddressCarryingEvent{
	
	private Vector<Integer> destination;
	private Vector<Integer> source;
	public DestinationBankEvent(EventQueue eventQ, long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address,Vector<Integer> source ,Vector<Integer> destination){
		super(eventQ, eventTime, requestingElement, processingElement,
				requestType, address);
		this.destination = destination;
		this.source = source;
	}
	public void updateEvent(EventQueue eventQ, long eventTime, SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address,Vector<Integer> Destination) {
		//this.address = address;
		this.destination = new Vector<Integer>(2);
		this.destination = Destination;
		super.updateEvent(eventQ, eventTime, requestingElement, processingElement, requestType, address);
	}
	
	public Vector<Integer> getDestination() {
		return this.destination;
	}

	public void setDestination(Vector<Integer> destination) {
		this.destination = new Vector<Integer>(2);
		this.destination = destination;
	}
}