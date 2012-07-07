package memorysystem;

import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class LSQEntryContainingEvent extends Event
{
	private LSQEntry lsqEntry;
	
	public LSQEntryContainingEvent(EventQueue eventQ, long eventTime, SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, LSQEntry lsqEntry) {
		super(eventQ, eventTime, requestingElement, processingElement,
				requestType);
		this.lsqEntry = lsqEntry;
	}
	
	public void updateEvent(EventQueue eventQ, long eventTime, SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, LSQEntry lsqEntry) {
		this.lsqEntry = lsqEntry;
		this.update(eventQ, eventTime, requestingElement, processingElement, requestType);
	}

	public LSQEntry getLsqEntry() {
		return lsqEntry;
	}

	public void setLsqEntry(LSQEntry lsqEntry) {
		this.lsqEntry = lsqEntry;
	}
}
