package pipeline.outoforder;

import generic.Core;
import generic.Event;

/**
 * this event is scheduled at the clock_time at which an FU becomes available
 * note : this event is scheduled with respect to a particular ROB entry,
 *      that is, a particular instruction attempts to secure an FU when this event transpires

 * handling of the event
 * call issueInstruction()
 */

public class FunctionalUnitAvailableEvent extends Event {
	
	Core core;
	ReorderBufferEntry reorderBufferEntry;
	
	public FunctionalUnitAvailableEvent(Core core, ReorderBufferEntry reorderBufferEntry,
			long eventTime )
	{
		super(eventTime,
				4,
				core.getExecEngine().getReorderBuffer()
					.getROB().indexOf(reorderBufferEntry) );
		
		this.core = core;
		this.reorderBufferEntry = reorderBufferEntry;
	}

	@Override
	public void handleEvent() {

		reorderBufferEntry.getAssociatedIWEntry().issueInstruction();
		
	}

}