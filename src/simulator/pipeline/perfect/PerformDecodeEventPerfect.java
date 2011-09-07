package pipeline.perfect;

import generic.Core;
import generic.GlobalClock;
import generic.NewEvent;
import generic.NewEventQueue;
import generic.RequestType;
import generic.Time_t;

public class PerformDecodeEventPerfect extends NewEvent {
	
	Core core;
	NewEventQueue eventQueue;
	
	//the decoder is supposed to read from all pipes in round-robin fashion
	private int pipeToReadFrom;
	
	public PerformDecodeEventPerfect(long eventTime, Core core, int pipeToReadFrom)
	{
		super(new Time_t(eventTime),
				null,
				null,
				0,
				RequestType.PERFORM_DECODE);
		
		this.core = core;
		this.pipeToReadFrom = pipeToReadFrom;
	}

	@Override
	public void handleEvent(NewEventQueue newEventQueue) {
		
		this.eventQueue = newEventQueue;
		
		if(core.getExecEngine().isStallDecode1() == false &&
				core.getExecEngine().isStallDecode2() == false)
		{
			int ctr = 0;
			while(core.getExecEngine().isDecodePipeEmpty(pipeToReadFrom) == true
					&& ctr < core.getNo_of_threads())
			{
				ctr++;
			}
			
			if(ctr == core.getNo_of_threads())
			{
				core.getExecEngine().setAllPipesEmpty(true);
			}
			else
			{
				core.getExecEngine().getDecoder().scheduleDecodeCompletion(pipeToReadFrom);
			}
		}
		
		if(core.getExecEngine().isAllPipesEmpty() == false)
		{
			/*this.eventQueue.addEvent(new PerformDecodeEvent(GlobalClock.getCurrentTime()+1, core));*/
			//this.setEventTime(new Time_t(GlobalClock.getCurrentTime()+1));
			this.getEventTime().setTime(GlobalClock.getCurrentTime()+core.getStepSize());
			pipeToReadFrom = (pipeToReadFrom + 1)%core.getNo_of_threads();
			this.eventQueue.addEvent(this);
		}
	}

}