package pipeline.inorder;

import memorysystem.CoreMemorySystem;
import pipeline.PipelineInterface;
import generic.Core;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Statistics;

public class InorderPipeline implements PipelineInterface{
	Core core;
	EventQueue eventQ;
	int coreStepSize;
	
	public InorderPipeline(Core _core, EventQueue eventQ){
		this.core = _core;
		this.eventQ = eventQ;
		coreStepSize = core.getStepSize();
	}
	
	public void oneCycleOperation(){
		long currentTime = GlobalClock.getCurrentTime();
/*if(core.getCore_number()==1)
	System.out.println(" exec complete "+core.getExecutionEngineIn().getExecutionComplete());
*/
		if(currentTime % coreStepSize==0 && !core.getExecutionEngineIn().getExecutionComplete()){
			this.core.getExecutionEngineIn().incrementNumCycles(1);
			writeback();
		}
		drainEventQueue();
		if(currentTime % coreStepSize==0 && !core.getExecutionEngineIn().getExecutionComplete()){
			mem();
			exec();
			decode();
			fetch();
		}

		//System.out.println("Ins executed = "+ core.getNoOfInstructionsExecuted());
	}

	private void drainEventQueue(){
		eventQ.processEvents();		
	}
	public void writeback(){
		core.getExecutionEngineIn().getWriteBackUnitIn().performWriteBack();		
	}
	public void mem(){
		core.getExecutionEngineIn().getMemUnitIn().performMemEvent();
	}
	public void exec(){
		core.getExecutionEngineIn().getExecUnitIn().execute();
	}
	public void regfile(){
		core.getExecutionEngineIn().getRegFileIn().fetchOperands();
	}
	public void decode(){
		core.getExecutionEngineIn().getDecodeUnitIn().performDecode();
		regfile();
	}
	public void fetch(){
		core.getExecutionEngineIn().getFetchUnitIn().performFetch();
	}

	@Override
	public boolean isExecutionComplete() {
		// TODO Auto-generated method stub
		return (core.getExecutionEngineIn().getExecutionComplete());
		}

	@Override
	public int getCoreStepSize() {
		// TODO Auto-generated method stub
		return coreStepSize;
	}

	@Override
	public void setcoreStepSize(int stepSize) {
		// TODO Auto-generated method stub
		this.coreStepSize=stepSize;
	}

	@Override
	public void resumePipeline() {
		core.getExecutionEngineIn().getFetchUnitIn().resumePipeline();
		// TODO Auto-generated method stub
		
	}

	@Override
	public Core getCore() {
		return core;
	}

	@Override
	public boolean isSleeping() {
		return core.getExecutionEngineIn().getFetchUnitIn().getSleep();
	}
	public void setTimingStatistics()
	{
		Statistics.setCoreCyclesTaken(core.getCoreCyclesTaken(), core.getCore_number());
		Statistics.setCoreFrequencies(core.getFrequency(), core.getCore_number());
		Statistics.setNumCoreInstructions(core.getNoOfInstructionsExecuted(), core.getCore_number());
	}
	
	public void setPerCoreMemorySystemStatistics()
	{
		CoreMemorySystem coreMemSys = core.getExecutionEngineIn().coreMemorySystem;
		Statistics.setNoOfMemRequests(coreMemSys.getLsqueue().noOfMemRequests, core.getCore_number());
		Statistics.setNoOfLoads(coreMemSys.getLsqueue().NoOfLd, core.getCore_number());
		Statistics.setNoOfStores(coreMemSys.getLsqueue().NoOfSt, core.getCore_number());
		Statistics.setNoOfL1Requests(coreMemSys.getL1Cache().noOfRequests, core.getCore_number());
		Statistics.setNoOfL1Hits(coreMemSys.getL1Cache().hits, core.getCore_number());
		Statistics.setNoOfL1Misses(coreMemSys.getL1Cache().misses, core.getCore_number());
		Statistics.setNoOfIRequests(coreMemSys.getiCache().noOfRequests, core.getCore_number());
		Statistics.setNoOfIHits(coreMemSys.getiCache().hits, core.getCore_number());
		Statistics.setNoOfIMisses(coreMemSys.getiCache().misses, core.getCore_number());
	}

}
