package memorysystem;

import generic.Event;
import generic.RequestType;
import generic.SimulationElement;

public class AddressCarryingEvent extends Event
{
	private long address;
	
	public AddressCarryingEvent(long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address) {
		super(eventTime, requestingElement, processingElement,
				requestType);
		this.address = address;
	}

	public void updateEvent(long eventTime, SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address) {
		this.address = address;
		this.update(eventTime, requestingElement, processingElement, requestType);
	}
	
	public long getAddress() {
		return address;
	}

	public void setAddress(long address) {
		this.address = address;
	}
}
