package pipeline.outoforder;

import generic.Core;
import generic.NewEvent;
import generic.RequestType;

/**
 * this event is scheduled at the clock_time at which an FU becomes available
 * note : this event is scheduled with respect to a particular ROB entry,
 *      that is, a particular instruction attempts to secure an FU when this event transpires

 * handling of the event
 * call issueInstruction()
 */

public class FunctionalUnitAvailableEvent extends NewEvent {
	
	Core core;
	ReorderBufferEntry reorderBufferEntry;
	
	public FunctionalUnitAvailableEvent(Core core, ReorderBufferEntry reorderBufferEntry,
			long eventTime )
	{
		super(eventTime,
				null,
				null,
				core.getExecEngine().getReorderBuffer()
					.getROB().indexOf(reorderBufferEntry),
				RequestType.FUNC_UNIT_AVAILABLE	);
		
		this.core = core;
		this.reorderBufferEntry = reorderBufferEntry;
	}

	@Override
	public NewEvent handleEvent() {

		return reorderBufferEntry.getAssociatedIWEntry().issueInstruction();
		
	}

}