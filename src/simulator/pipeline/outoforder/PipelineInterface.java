package pipeline.outoforder;

import generic.Core;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Statistics;

public class PipelineInterface implements pipeline.PipelineInterface {
	
	Core core;
	EventQueue eventQ;
	int coreStepSize;
	
	public PipelineInterface(Core core, EventQueue eventQ)
	{
		this.core = core;
		this.eventQ = eventQ;
	}

	@Override
	public void oneCycleOperation() {
		
		coreStepSize = core.getStepSize();
		if(coreStepSize == 0)
		{
			//this core has been initialised, but has no pipeline running on it
			return;
		}
		
		OutOrderExecutionEngine execEngine;
		
		execEngine = (OutOrderExecutionEngine) core.getExecEngine();
		
		long currentTime = GlobalClock.getCurrentTime();
		if(currentTime % coreStepSize == 0 && execEngine.isExecutionComplete() == false)
		{
			execEngine.getReorderBuffer().performCommits();
			execEngine.getWriteBackLogic().performWriteBack();
			execEngine.getSelector().performSelect2();
		}
		
		/*else //Statistical Pipeline
		{
			statPipeline = core.getStatisticalPipeline();
			statPipeline.performCommits();
			if (statPipeline.isExecutionComplete() == false)
			{
				statPipeline.getFetcher().performFetch();
			}
		}*/
		
		//handle events
		eventQ.processEvents();
		
		if(currentTime % coreStepSize == 0 && execEngine.isExecutionComplete() == false)
		{
			execEngine.getIWPusher().performIWPush();
			execEngine.getRenamer().performRename();
			execEngine.getDecoder().performDecode();
			execEngine.getFetcher().performFetch();
			this.core.powerCounters.perCycleAccessRecordUpdate();
//			this.core.powerCounters.updatePowerStatsPerCycle();
//			this.core.powerCounters.clearAccessStats();

		}
		
	}
	public Core getCore() {
		return core;
	}

	public void setCore(Core core) {
		this.core = core;
	}

	public EventQueue getEventQ() {
		return eventQ;
	}

	public void setEventQ(EventQueue eventQ) {
		this.eventQ = eventQ;
	}

	@Override
	public boolean isExecutionComplete() {
		
		/*if (core.isPipelineStatistical)
            return core.getStatisticalPipeline().isExecutionComplete();
        else*/
        return core.getExecEngine().isExecutionComplete();
		
		
	}
	
	public void setcoreStepSize(int stepSize)
	{
		this.coreStepSize = stepSize;
	}
	
	public int getCoreStepSize()
	{
		return coreStepSize;
	}

	@Override
	public void resumePipeline() {
		((OutOrderExecutionEngine)core.getExecEngine()).getFetcher().setSleep(false);
	}

	@Override
	public boolean isSleeping() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getFetcher().isSleep();
	}

	@Override
	public void setTimingStatistics() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPerCoreMemorySystemStatistics() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setPerCorePowerStatistics(){
		Statistics.setPerCorePowerStatistics(core.powerCounters, core.getCore_number());
	}


	@Override
	public void setExecutionComplete(boolean status) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void adjustRunningThreads(int adjval) {
		// TODO Auto-generated method stub
		
	}
	

}
