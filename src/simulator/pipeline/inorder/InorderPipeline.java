package pipeline.inorder;

import memorysystem.CoreMemorySystem;
import pipeline.PipelineInterface;
import power.Counters;
import generic.Core;
import generic.EventQueue;
import generic.GenericCircularQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.Statistics;

public class InorderPipeline implements PipelineInterface{
	
	Core core;
	InorderExecutionEngine containingExecutionEngine;
	EventQueue eventQ;
	int coreStepSize;
	StageLatch ifId,idEx,exMem,memWb,wbDone;
	private int id;
	

	public InorderPipeline(Core _core, EventQueue eventQ, int id){
	
		this.core = _core;
		containingExecutionEngine = (InorderExecutionEngine)core.getExecEngine();
		this.eventQ = eventQ;
		this.coreStepSize = core.getStepSize();	//Not correct. Global clock hasn't been initialized yet
												//So, step sizes of the cores hasn't been set.
												//It will be set when the step sizes of the cores will be set.
		this.ifId = containingExecutionEngine.getIfIdLatch(id);
		this.idEx = containingExecutionEngine.getIdExLatch(id);
		this.exMem = containingExecutionEngine.getExMemLatch(id);
		this.memWb = containingExecutionEngine.getMemWbLatch(id);
		this.wbDone = containingExecutionEngine.getWbDoneLatch(id);
//		core.getExecutionEngineIn().setExecutionComplete(true);
		this.id = id;
	}
	
	public void oneCycleOperation(){
		long currentTime = GlobalClock.getCurrentTime();
/*if(core.getCore_number()==1)
	System.out.println(" exec complete "+containingExecutionEngine.getExecutionComplete());
*/
		if(currentTime % getCoreStepSize()==0 && !containingExecutionEngine.getExecutionComplete()){
			writeback();
		}
		drainEventQueue();
		if(currentTime % getCoreStepSize()==0 && !containingExecutionEngine.getExecutionComplete()){
			mem();
			exec();
			decode();
			fetch();

			this.core.powerCounters.perCycleAccessRecordUpdate();

//			this.core.powerCounters.updatePowerStatsPerCycle();
//			this.core.powerCounters.clearAccessStats();

//			if(this.containingExecutionEngine.getFetchUnitIn().getStallLowerMSHRFull()>0)
//				this.containingExecutionEngine.getFetchUnitIn().decrementStallLowerMSHRFull(1);
//			else
			if(this.containingExecutionEngine.getStallFetch()>0){
//			System.out.println("Stalled for "+this.containingExecutionEngine.getStallFetch());
				this.containingExecutionEngine.decrementStallFetch(1);
			}

//			this.containingExecutionEngine.incrementNumCycles(1);	//FIXME redundant operation. We are not using this for final statistics.
																		//Global clock cycle/core step size is used instead.
		}

		//System.out.println("Ins executed = "+ core.getNoOfInstructionsExecuted());
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private void drainEventQueue(){
		eventQ.processEvents();		
	}
	public void writeback(){
		containingExecutionEngine.getWriteBackUnitIn().performWriteBack(this);		
	}
	public void mem(){
		containingExecutionEngine.getMemUnitIn().performMemEvent(this);
	}
	public void exec(){
		containingExecutionEngine.getExecUnitIn().execute(this);
	}
	public void regfile(){
		containingExecutionEngine.getRegFileIn().fetchOperands(this);
	}
	public void decode(){
		containingExecutionEngine.getDecodeUnitIn().performDecode(this);
		regfile();
	}
	public void fetch(){
		containingExecutionEngine.getFetchUnitIn().performFetch(this);
	}

	@Override
	public boolean isExecutionComplete() {
		// TODO Auto-generated method stub
		return (containingExecutionEngine.getExecutionComplete());
		}

	@Override
	public int getCoreStepSize() {
//		return coreStepSize;
		return this.core.getStepSize();
	}

	@Override
	public void setcoreStepSize(int stepSize) {
		this.coreStepSize=stepSize;
	}

	@Override
	public void resumePipeline() {
		containingExecutionEngine.getFetchUnitIn().resumePipeline();
		// TODO Auto-generated method stub
		
	}

	@Override
	public Core getCore() {
		return core;
	}

	@Override
	public boolean isSleeping() {
		return containingExecutionEngine.getFetchUnitIn().getSleep();
	}
	public void setTimingStatistics()
	{
		core.setCoreCyclesTaken(containingExecutionEngine.getNumCycles());
		Statistics.setCoreCyclesTaken(containingExecutionEngine.getNumCycles(), core.getCore_number());
		Statistics.setCoreFrequencies(core.getFrequency(), core.getCore_number());
		Statistics.setNumCoreInstructions(core.getNoOfInstructionsExecuted(), core.getCore_number());
		
		System.out.println("Mem Stalls = "+this.containingExecutionEngine.getMemStall());
		System.out.println("Data Hazard Stalls = "+this.containingExecutionEngine.getDataHazardStall());
		System.out.println("Instruction Mem Stalls = "+this.containingExecutionEngine.getInstructionMemStall());

		System.out.println("IcacheHits = "+this.containingExecutionEngine.icachehit);
		System.out.println("Fresh l2 requests = "+this.containingExecutionEngine.freshl2req);
		System.out.println("Old l2 requests = "+this.containingExecutionEngine.oldl2req);
		System.out.println("L2 mem response = "+this.containingExecutionEngine.l2memres);
		System.out.println("L2 mem outstanding = "+this.containingExecutionEngine.l2memoutstanding);
		System.out.println("L2 accesses = "+this.containingExecutionEngine.l2accesses);
		System.out.println("L2 hits = "+this.containingExecutionEngine.l2hits);

	}
	
	public void setPerCoreMemorySystemStatistics()
	{
		CoreMemorySystem coreMemSys = containingExecutionEngine.inorderCoreMemorySystem;
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
	public void setPerCorePowerStatistics(){
		core.powerCounters.clearAccessStats();
		core.powerCounters.updatePowerAfterCompletion(core.getCoreCyclesTaken());
		Statistics.setPerCorePowerStatistics(core.powerCounters, core.getCore_number());
	}
	
	public StageLatch getIfIdLatch(){
		return this.ifId;
	}
	public StageLatch getIdExLatch(){
		return this.idEx;
	}
	public StageLatch getExMemLatch(){
		return this.exMem;
	}
	public StageLatch getMemWbLatch(){
		return this.memWb;
	}
	public StageLatch getWbDoneLatch(){
		return this.wbDone;
	}

	@Override
	public void setExecutionComplete(boolean status) {
		// TODO Auto-generated method stub
		containingExecutionEngine.setExecutionComplete(status);		
	}

	@Override
	public void adjustRunningThreads(int adjval) {
		// TODO Auto-generated method stub
//		this.getCore().currentThreads += adjval;
		
	}

	@Override
	public void setInputToPipeline(
			GenericCircularQueue<Instruction>[] inputToPipeline) {
		
		this.core.getExecEngine().setInputToPipeline(inputToPipeline);
		
	}
}
