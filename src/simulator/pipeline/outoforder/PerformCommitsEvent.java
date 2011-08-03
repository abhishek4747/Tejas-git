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
		
		if(core.getExecEngine().isExecutionComplete() == false)
		{
			return (new PerformCommitsEvent(GlobalClock.getCurrentTime()+1, core));
		}
		else
		{			
			System.out.println();
			System.out.println("core " + core.getCore_number() + " reaches the finish line!!");
			System.out.println(GlobalClock.getCurrentTime() + " cycles");
			return null;
		}
	}

}
