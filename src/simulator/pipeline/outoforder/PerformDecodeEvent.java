package pipeline.outoforder;

import generic.Core;
import generic.GlobalClock;
import generic.NewEvent;
import generic.RequestType;

public class PerformDecodeEvent extends NewEvent {
	
	Core core;
	
	public PerformDecodeEvent(long eventTime, Core core)
	{
		super(eventTime,
				null,
				null,
				0,
				RequestType.PERFORM_DECODE);
		
		this.core = core;
	}

	@Override
	public NewEvent handleEvent() {
		
		if(core.getExecEngine().isStallDecode2() == false &&
				core.getExecEngine().isStallDecode1() == false &&
				core.getExecEngine().isDecodePipeEmpty() == false)
		{
			core.getExecEngine().getDecoder().scheduleDecodeCompletion();
		}
		
		if(core.getExecEngine().isDecodePipeEmpty() == false)
		{
			return (new PerformDecodeEvent(GlobalClock.getCurrentTime()+1, core));
		}
		else
		{
			return null;
		}
	}

}