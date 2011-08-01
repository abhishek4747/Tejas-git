package pipeline.outoforder;

import generic.Core;
import generic.GlobalClock;
import generic.NewEvent;
import generic.RequestType;

public class PerformCommitsEvent extends NewEvent {
	
	Core core;
	
	public PerformCommitsEvent(long eventTime, Core core)
	{
		super(eventTime,
				null,
				null,
				0,
				RequestType.PERFORM_COMMITS);
		
		this.core = core;
	}

	@Override
	public NewEvent handleEvent() {
		
		if(core.getExecEngine().isStallDecode2() == false)
		{
			core.getExecEngine().getReorderBuffer().performCommits();
		}
		
		return (new PerformCommitsEvent(GlobalClock.getCurrentTime()+1, core));
	}

}
