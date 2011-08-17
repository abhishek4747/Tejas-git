package pipeline.outoforder;

import memorysystem.MemorySystem;
import generic.Core;
import generic.GlobalClock;
import generic.NewEvent;
import generic.NewEventQueue;
import generic.RequestType;
import generic.Time_t;

public class PerformCommitsEvent extends NewEvent {
	
	Core core;
	NewEventQueue eventQueue;
	
	public PerformCommitsEvent(long eventTime, Core core)
	{
		super(new Time_t(eventTime),
				null,
				null,
				0,
				RequestType.PERFORM_COMMITS);
		
		this.core = core;
	}

	@Override
	public void handleEvent(NewEventQueue newEventQueue) {
		
		this.eventQueue = newEventQueue;
		
		if(core.getExecEngine().isStallDecode2() == false)
		{
			core.getExecEngine().getReorderBuffer().performCommits();
		}
		
		if(core.getExecEngine().isExecutionComplete() == false)
		{
			/*this.eventQueue.addEvent(new PerformCommitsEvent(GlobalClock.getCurrentTime()+1, core));*/
			this.setEventTime(new Time_t(GlobalClock.getCurrentTime()+core.getStepSize()));
			this.eventQueue.addEvent(this);
		}
		else
		{			
			System.out.println();
			System.out.println("core " + core.getCore_number() + " reaches the finish line!!");
			System.out.println(GlobalClock.getCurrentTime() + " cycles");
			System.out.println(GlobalClock.getCurrentTime() * GlobalClock.getStepValue()+ " microseconds");
			
			MemorySystem.printMemSysResults();
		}
	}

}
