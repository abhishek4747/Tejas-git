package pipeline.outoforder_new_arch;

import generic.Event;
import generic.RequestType;
import generic.SimulationElement;

public class BroadCast1Event extends Event {
	
	ReorderBufferEntry ROBEntry;

	public BroadCast1Event(long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType,
			ReorderBufferEntry ROBEntry)
	{
		super(null, eventTime, requestingElement, processingElement, requestType);
		
		this.ROBEntry = ROBEntry;
	}

	public ReorderBufferEntry getROBEntry() {
		return ROBEntry;
	}

}
