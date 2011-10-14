package generic;

import pipeline.outoforder_new_arch.ReorderBufferEntry;

public class ExecCompleteEvent extends Event {
	
	ReorderBufferEntry ROBEntry;

	public ExecCompleteEvent(long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType,
			ReorderBufferEntry ROBEntry)
	{
		super(eventTime, requestingElement, processingElement, requestType);
		
		this.ROBEntry = ROBEntry;
	}

	public ReorderBufferEntry getROBEntry() {
		return ROBEntry;
	}

}
