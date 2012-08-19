package pipeline.inorder;

import memorysystem.CoreMemorySystem;
import pipeline.PipelineInterface;
import power.Counters;
import generic.Core;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Statistics;

public class InorderPipeline implements PipelineInterface{
	Core core;
	EventQueue eventQ;
	int coreStepSize;
	StageLatch ifId,idEx,exMem,memWb,wbDone;
	private int id;

	public InorderPipeline(Core _core, EventQueue eventQ, int id){
		this.core = _core;
		this.eventQ = eventQ;
		this.coreStepSize = core.getStepSize();	//Not correct. Global clock hasn't been initialized yet
												//So, step sizes of the cores hasn't been set.
												//It will be set when the step sizes of the cores will be set.
		this.ifId = core.getExecutionEngineIn().getIfIdLatch(id);
		this.idEx = core.getExecutionEngineIn().getIdExLatch(id);
		this.exMem = core.getExecutionEngineIn().getExMemLatch(id);
		this.memWb = core.getExecutionEngineIn().getMemWbLatch(id);
		this.wbDone = core.getExecutionEngineIn().getWbDoneLatch(id);
//		core.getExecutionEngineIn().setExecutionComplete(true);
		this.id = id;
	}
	
	public void oneCycleOperation(){
		long currentTime = GlobalClock.getCurrentTime();
/*if(core.getCore_number()==1)
	System.out.println(" exec complete "+core.getExecutionEngineIn().getExecutionComplete());
*/
		if(currentTime % getCoreStepSize()==0 && !core.getExecutionEngineIn().getExecutionComplete()){
			writeback();
		}
		drainEventQueue();
		if(currentTime % getCoreStepSize()==0 && !core.getExecutionEngineIn().getExecutionComplete()){
			mem();
			exec();
			decode();
			fetch();

			this.core.powerCounters.perCycleAccessRecordUpdate();

//			this.core.powerCounters.updatePowerStatsPerCycle();
//			this.core.powerCounters.clearAccessStats();

//			if(this.core.getExecutionEngineIn().getFetchUnitIn().getStallLowerMSHRFull()>0)
//				this.core.getExecutionEngineIn().getFetchUnitIn().decrementStallLowerMSHRFull(1);
//			else
			if(this.core.getExecutionEngineIn().getStallFetch()>0){
//			System.out.println("Stalled for "+this.core.getExecutionEngineIn().getStallFetch());
				this.core.getExecutionEngineIn().decrementStallFetch(1);
			}

//			this.core.getExecutionEngineIn().incrementNumCycles(1);	//FIXME redundant operation. We are not using this for final statistics.
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
		core.getExecutionEngineIn().getWriteBackUnitIn().performWriteBack(this);		
	}
	public void mem(){
		core.getExecutionEngineIn().getMemUnitIn().performMemEvent(this);
	}
	public void exec(){
		core.getExecutionEngineIn().getExecUnitIn().execute(this);
	}
	public void regfile(){
		core.getExecutionEngineIn().getRegFileIn().fetchOperands(this);
	}
	public void decode(){
		core.getExecutionEngineIn().getDecodeUnitIn().performDecode(this);
		regfile();
	}
	public void fetch(){
		core.getExecutionEngineIn().getFetchUnitIn().performFetch(this);
	}

	@Override
	public boolean isExecutionComplete() {
		// TODO Auto-generated method stub
		return (core.getExecutionEngineIn().getExecutionComplete());
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
		core.setCoreCyclesTaken(core.getExecutionEngineIn().getNumCycles());
		Statistics.setCoreCyclesTaken(core.getExecutionEngineIn().getNumCycles(), core.getCore_number());
		Statistics.setCoreFrequencies(core.getFrequency(), core.getCore_number());
		Statistics.setNumCoreInstructions(core.getNoOfInstructionsExecuted(), core.getCore_number());
		
		System.out.println("Mem Stalls = "+this.core.getExecutionEngineIn().getMemStall());
		System.out.println("Data Hazard Stalls = "+this.core.getExecutionEngineIn().getDataHazardStall());
		System.out.println("Instruction Mem Stalls = "+this.core.getExecutionEngineIn().getInstructionMemStall());

		System.out.println("IcacheHits = "+this.core.getExecutionEngineIn().icachehit);
		System.out.println("Fresh l2 requests = "+this.core.getExecutionEngineIn().freshl2req);
		System.out.println("Old l2 requests = "+this.core.getExecutionEngineIn().oldl2req);
		System.out.println("L2 mem response = "+this.core.getExecutionEngineIn().l2memres);
		System.out.println("L2 mem outstanding = "+this.core.getExecutionEngineIn().l2memoutstanding);
		System.out.println("L2 accesses = "+this.core.getExecutionEngineIn().l2accesses);
		System.out.println("L2 hits = "+this.core.getExecutionEngineIn().l2hits);

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
	public void setPerCorePowerStatistics(){
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
		core.getExecutionEngineIn().setExecutionComplete(status);		
	}
}
