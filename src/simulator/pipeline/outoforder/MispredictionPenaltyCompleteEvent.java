package pipeline.outoforder;

import generic.Core;
import generic.NewEvent;
import generic.NewEventQueue;
import generic.RequestType;
import generic.Time_t;

public class MispredictionPenaltyCompleteEvent extends NewEvent {
	
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
	public void handleEvent(NewEventQueue newEventQueue) {
		
		core.getExecEngine().setStallDecode2(false);
	}

}
