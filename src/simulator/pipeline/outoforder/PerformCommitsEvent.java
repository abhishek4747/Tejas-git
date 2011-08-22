package pipeline.outoforder;

import generic.Core;
import generic.GlobalClock;
import generic.NewEvent;
import generic.NewEventQueue;
import generic.RequestType;
import generic.Statistics;
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
			/*System.out.println();
			System.out.println("core " + core.getCore_number() + " reaches the finish line!!");
			System.out.println(GlobalClock.getCurrentTime() + " global clock cycles");
			System.out.println(GlobalClock.getCurrentTime()/core.getStepSize() + " core cycles at "
					+ core.getFrequency() + " MHz, viz., "
					+ GlobalClock.getCurrentTime() * GlobalClock.getStepValue()+ " microseconds");
			
			MemorySystem.printMemSysResults();*/
			
			setTimingStatistics();			
			setPerCoreMemorySystemStatistics();
		}
	}
	
	public void setTimingStatistics()
	{
		Statistics.setCoreCyclesTaken(GlobalClock.getCurrentTime()/core.getStepSize(), core.getCore_number());
		Statistics.setCoreFrequencies(core.getFrequency(), core.getCore_number());
		Statistics.setNumCoreInstructions(core.getNoOfInstructionsExecuted(), core.getCore_number());
	}
	
	public void setPerCoreMemorySystemStatistics()
	{
		Statistics.setNoOfLoads(core.getExecEngine().coreMemSys.getLsqueue().NoOfLd, core.getCore_number());
		Statistics.setNoOfStores(core.getExecEngine().coreMemSys.getLsqueue().NoOfSt, core.getCore_number());
		Statistics.setNoOfValueForwards(core.getExecEngine().coreMemSys.getLsqueue().NoOfForwards, core.getCore_number());
		Statistics.setNoOfTLBHits(core.getExecEngine().coreMemSys.getTLBuffer().getTlbHits(), core.getCore_number());
		Statistics.setNoOfTLBMisses(core.getExecEngine().coreMemSys.getTLBuffer().getTlbMisses(), core.getCore_number());
		Statistics.setNoOfL1Hits(core.getExecEngine().coreMemSys.getL1Cache().hits, core.getCore_number());
		Statistics.setNoOfL1Misses(core.getExecEngine().coreMemSys.getL1Cache().misses, core.getCore_number());
	}

}
