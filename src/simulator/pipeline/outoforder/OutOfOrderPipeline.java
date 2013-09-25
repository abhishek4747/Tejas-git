package pipeline.outoforder;

import generic.Core;
import generic.EventQueue;
import generic.GenericCircularQueue;
import generic.GlobalClock;
import generic.Instruction;

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
			execEngine.getSelector().performSelect();
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

	@Override
	public long getBranchCount() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getReorderBuffer().getBranchCount();
	}

	@Override
	public long getMispredCount() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getReorderBuffer().getMispredCount();
	}

	@Override
	public long getNoOfMemRequests() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getLsqueue().noOfMemRequests;
	}

	@Override
	public long getNoOfLoads() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getLsqueue().NoOfLd;
	}

	@Override
	public long getNoOfStores() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getLsqueue().NoOfSt;
	}

	@Override
	public long getNoOfValueForwards() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getLsqueue().NoOfForwards;
	}

	@Override
	public long getNoOfTLBRequests() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getTLBuffer().getTlbRequests();
	}

	@Override
	public long getNoOfTLBHits() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getTLBuffer().getTlbHits();
	}

	@Override
	public long getNoOfTLBMisses() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getTLBuffer().getTlbMisses();
	}

	@Override
	public long getNoOfL1Requests() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getL1Cache().noOfRequests;
	}

	@Override
	public long getNoOfL1Hits() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getL1Cache().hits;
	}

	@Override
	public long getNoOfL1Misses() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getL1Cache().misses;
	}

	@Override
	public long getNoOfIRequests() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getiCache().noOfRequests;
	}

	@Override
	public long getNoOfIHits() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getiCache().hits;
	}

	@Override
	public long getNoOfIMisses() {
		return ((OutOrderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getiCache().misses;
	}
	

}
