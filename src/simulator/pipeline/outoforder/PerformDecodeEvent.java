package pipeline.outoforder;

import generic.Core;
import generic.GlobalClock;
import generic.NewEvent;
import generic.NewEventQueue;
import generic.RequestType;
import generic.Time_t;

public class PerformDecodeEvent extends NewEvent {
	
	Core core;
	NewEventQueue eventQueue;
	
	public PerformDecodeEvent(long eventTime, Core core)
	{
		super(new Time_t(eventTime),
				null,
				null,
				0,
				RequestType.PERFORM_DECODE);
		
		this.core = core;
	}

	@Override
	public void handleEvent(NewEventQueue newEventQueue) {
		
		this.eventQueue = newEventQueue;
		
		if(core.getExecEngine().isStallDecode2() == false &&
				core.getExecEngine().isStallDecode1() == false &&
				core.getExecEngine().isDecodePipeEmpty() == false)
		{
			//TODO if multiple threads executing on same core, fetch to be performed accordingly
			//currently fetching from thread 0
			core.getExecEngine().getDecoder().scheduleDecodeCompletion(0);
		}
		
		if(core.getExecEngine().isDecodePipeEmpty() == false)
		{
			/*this.eventQueue.addEvent(new PerformDecodeEvent(GlobalClock.getCurrentTime()+1, core));*/
			//this.setEventTime(new Time_t(GlobalClock.getCurrentTime()+1));
			this.getEventTime().setTime(GlobalClock.getCurrentTime()+core.getStepSize());
			this.eventQueue.addEvent(this);
		}
	}

}
