package pipeline.outoforder;

import generic.Core;
import generic.EventQueue;
import generic.GenericCircularQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.Statistics;

public class OutOfOrderPipeline implements pipeline.PipelineInterface {
	
	Core core;
	EventQueue eventQ;
	int coreStepSize;
	
	public OutOfOrderPipeline(Core core, EventQueue eventQ)
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
		
		OutOrderExecutionEngine execEngine = (OutOrderExecutionEngine) core.getExecEngine();
		execEngine.getReorderBuffer().setTimingStatistics();
		
	}

	@Override
	public void setPerCoreMemorySystemStatistics() {
		
		OutOrderExecutionEngine execEngine = (OutOrderExecutionEngine) core.getExecEngine();
		execEngine.getReorderBuffer().setPerCoreMemorySystemStatistics();
		
	}
	
	@Override
	public void setPerCorePowerStatistics(){
		OutOrderExecutionEngine execEngine = (OutOrderExecutionEngine) core.getExecEngine();
		execEngine.getReorderBuffer().setPerCorePowerStatistics();
	}


	@Override
	public void setExecutionComplete(boolean status) {
				
	}


	@Override
	public void adjustRunningThreads(int adjval) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInputToPipeline(
			GenericCircularQueue<Instruction>[] inputToPipeline) {
		
		this.core.getExecEngine().setInputToPipeline(inputToPipeline);
		
	}
	

}