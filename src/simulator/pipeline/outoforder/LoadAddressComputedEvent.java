package pipeline.outoforder;

import memorysystem.LSQAddressReadyEvent;
import generic.Core;
import generic.NewEvent;
import generic.NewEventQueue;
import generic.OperationType;
import generic.RequestType;
import generic.Time_t;

public class LoadAddressComputedEvent extends NewEvent {
	
	Core core;
	ReorderBufferEntry reorderBufferEntry;
	
	public LoadAddressComputedEvent(Core core, ReorderBufferEntry reorderBufferEntry,
			long eventTime )
	{
		super(new Time_t(eventTime),
				null,
				null,
				core.getExecEngine().getReorderBuffer().indexOf(reorderBufferEntry),
				RequestType.LOAD_ADDRESS_COMPUTED	);
		
		this.core = core;
		this.reorderBufferEntry = reorderBufferEntry;
	}

	@Override
	public void handleEvent(NewEventQueue newEventQueue) {

		if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.store)
		{
			reorderBufferEntry.setExecuted(true);
			reorderBufferEntry.setWriteBackDone1(true);
			reorderBufferEntry.setWriteBackDone2(true);
		}
		
		//add event to indicate address ready
		core.getExecEngine().coreMemSys.getLsqueue().getPort().put(new LSQAddressReadyEvent(core.getExecEngine().coreMemSys.getLsqueue().getLatencyDelay(), 
													null, //Requesting Element
													core.getExecEngine().coreMemSys.getLsqueue(), 
													0, //tieBreaker,
													RequestType.TLB_ADDRESS_READY,
													reorderBufferEntry.getLsqEntry()));

	}

}
