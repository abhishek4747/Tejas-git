package generic;

import pipeline.outoforder.ReorderBufferEntry;

public class ExecCompleteEvent extends Event {
	
	ReorderBufferEntry ROBEntry;

	public ExecCompleteEvent(EventQueue eventQ,
			long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType,
			ReorderBufferEntry ROBEntry)
	{
		super(eventQ, eventTime, requestingElement, processingElement, requestType);
		
		this.ROBEntry = ROBEntry;
	}

	public ReorderBufferEntry getROBEntry() {
		return ROBEntry;
	}

}
