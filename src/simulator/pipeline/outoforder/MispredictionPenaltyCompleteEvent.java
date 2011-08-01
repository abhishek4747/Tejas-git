package pipeline.outoforder;

import generic.Core;
import generic.NewEvent;
import generic.RequestType;

public class MispredictionPenaltyCompleteEvent extends NewEvent {
	
	Core core;
	
	public MispredictionPenaltyCompleteEvent(long eventTime, Core core)
	{
		super(eventTime,
				null,
				null,
				0,
				RequestType.MISPRED_PENALTY_COMPLETE);
		
		this.core = core;
	}

	@Override
	public NewEvent handleEvent() {
		
		core.getExecEngine().setStallDecode2(false);
		return null;
	}

}
