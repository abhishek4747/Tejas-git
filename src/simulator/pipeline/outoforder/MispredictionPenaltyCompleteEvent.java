package pipeline.outoforder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.Time_t;

public class MispredictionPenaltyCompleteEvent extends Event {
	
	Core core;
	
	public MispredictionPenaltyCompleteEvent(long eventTime, Core core)
	{
		super(new Time_t(eventTime),
				null,
				null,
				0,
				RequestType.MISPRED_PENALTY_COMPLETE);
		
		this.core = core;
	}

	@Override
	public void handleEvent(EventQueue eventQueue) {
		
		core.getExecEngine().setStallDecode2(false);
	}

}
