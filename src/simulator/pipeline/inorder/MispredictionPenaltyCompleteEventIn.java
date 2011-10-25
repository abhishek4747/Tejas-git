package pipeline.inorder;
import generic.Core;
import generic.NewEvent;
import generic.NewEventQueue;
import generic.RequestType;
import generic.Time_t;

public class MispredictionPenaltyCompleteEventIn extends NewEvent{

	
	Core core;
	
	public MispredictionPenaltyCompleteEventIn(long eventTime, Core core)
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
		
		core.getFetchUnitIn().setStall(false);
	}

}
