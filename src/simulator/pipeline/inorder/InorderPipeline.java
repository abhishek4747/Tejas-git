package pipeline.inorder;

import pipeline.PipelineInterface;
import generic.Core;
import generic.EventQueue;
import generic.GenericCircularQueue;
import generic.GlobalClock;
import generic.Instruction;

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
		this.coreStepSize = core.getStepSize();	//Not Necessary. Global clock hasn't been initialized yet
												//So, step sizes of the cores hasn't been set.
												//It will be set when the step sizes of the cores will be set.
		this.ifId = containingExecutionEngine.getIfIdLatch(id);
		this.idEx = containingExecutionEngine.getIdExLatch(id);
		this.exMem = containingExecutionEngine.getExMemLatch(id);
		this.memWb = containingExecutionEngine.getMemWbLatch(id);
		this.wbDone = containingExecutionEngine.getWbDoneLatch(id);
		this.id = id;
	}
	
	public void oneCycleOperation(){
		long currentTime = GlobalClock.getCurrentTime();
		if(currentTime % getCoreStepSize()==0 && !containingExecutionEngine.getExecutionComplete()){
			writeback();
		}
		drainEventQueue();		//Process Memory Requests
		if(currentTime % getCoreStepSize()==0 && !containingExecutionEngine.getExecutionComplete()){
			mem();
			exec();
			decode();
			fetch();

			this.core.powerCounters.perCycleAccessRecordUpdate();
			if(this.containingExecutionEngine.getStallFetch()>0){
				this.containingExecutionEngine.decrementStallFetch(1); 
			}
		}
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
	public void decode(){
		containingExecutionEngine.getDecodeUnitIn().performDecode(this);
	}
	public void fetch(){
		containingExecutionEngine.getFetchUnitIn().performFetch(this);
	}

	@Override
	public boolean isExecutionComplete() {
		return (containingExecutionEngine.getExecutionComplete());
		}

	@Override
	public int getCoreStepSize() {
		return this.core.getStepSize();
	}

	@Override
	public void setcoreStepSize(int stepSize) {
		this.coreStepSize=stepSize;
	}

	@Override
	public void resumePipeline() {
		containingExecutionEngine.getFetchUnitIn().resumePipeline();		
	}

	@Override
	public Core getCore() {
		return core;
	}

	@Override
	public boolean isSleeping() {
		return containingExecutionEngine.getFetchUnitIn().getSleep();
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
		containingExecutionEngine.setExecutionComplete(status);		
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
		return core.powerCounters.getBpredAccess();
	}

	@Override
	public long getMispredCount() {
		return core.powerCounters.getBpredMisses();
	}

	@Override
	public long getNoOfMemRequests() {
		return ((InorderExecutionEngine)core.getExecEngine()).getNoOfMemRequests();
	}

	@Override
	public long getNoOfLoads() {
		return ((InorderExecutionEngine)core.getExecEngine()).getNoOfLd();
	}

	@Override
	public long getNoOfStores() {
		return ((InorderExecutionEngine)core.getExecEngine()).getNoOfSt();
	}

	@Override
	public long getNoOfValueForwards() {
		return 0;
	}

	@Override
	public long getNoOfTLBRequests() {
		return ((InorderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getTLBuffer().getTlbRequests();
	}

	@Override
	public long getNoOfTLBHits() {
		return ((InorderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getTLBuffer().getTlbHits();
	}

	@Override
	public long getNoOfTLBMisses() {
		return ((InorderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getTLBuffer().getTlbMisses();
	}

	@Override
	public long getNoOfL1Requests() {
		return ((InorderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getL1Cache().noOfRequests;
	}

	@Override
	public long getNoOfL1Hits() {
		return ((InorderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getL1Cache().hits;
	}

	@Override
	public long getNoOfL1Misses() {
		return ((InorderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getL1Cache().misses;
	}

	@Override
	public long getNoOfIRequests() {
		return ((InorderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getiCache().noOfRequests;
	}

	@Override
	public long getNoOfIHits() {
		return ((InorderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getiCache().hits;
	}

	@Override
	public long getNoOfIMisses() {
		return ((InorderExecutionEngine)core.getExecEngine()).getCoreMemorySystem().getiCache().misses;
	}

	@Override
	public void setTimingStatistics() {
		// Not needed here, set by inorderexecutionengine
		
	}

	@Override
	public void setPerCoreMemorySystemStatistics() {
		// Not needed here, set by inorderexecutionengine
		
	}

	@Override
	public void setPerCorePowerStatistics() {
		// Not needed here, set by inorderexecutionengine
		
	}
}
