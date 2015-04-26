package pipeline.multi_issue_inorder;

import pipeline.PipelineInterface;
import generic.Core;
import generic.EventQueue;
import generic.GenericCircularQueue;
import generic.GlobalClock;
import generic.Instruction;

public class MultiIssueInorderPipeline implements PipelineInterface {

	Core core;
	MultiIssueInorderExecutionEngine containingExecutionEngine;
	EventQueue eventQ;
	int coreStepSize;
	StageLatch_MII ifId, exMem, memWb, wbDone;
	ReservationStation idEx;

	public MultiIssueInorderPipeline(Core _core, EventQueue eventQ) {

		this.core = _core;
		containingExecutionEngine = (MultiIssueInorderExecutionEngine) core
				.getExecEngine();
		this.eventQ = eventQ;
		this.coreStepSize = core.getStepSize(); // Not Necessary. Global clock
												// hasn't been initialized yet
												// So, step sizes of the cores
												// hasn't been set.
												// It will be set when the step
												// sizes of the cores will be
												// set.
		this.ifId = containingExecutionEngine.getIfIdLatch();
		this.idEx = containingExecutionEngine.getIdExRS();
		this.exMem = containingExecutionEngine.getExMemLatch();
		this.memWb = containingExecutionEngine.getMemWbLatch();
		this.wbDone = containingExecutionEngine.getWbDoneLatch();
	}

	public void oneCycleOperation() {
		long currentTime = GlobalClock.getCurrentTime();
		int ifid_, idex_, idexfree_, exmem_, memwb_, robsize_;
		ifid_ = idex_ = idexfree_ = exmem_ = memwb_ = robsize_ = 0;
		int ifid, idex, idexfree, exmem, memwb, robsize;
		ifid = containingExecutionEngine.getIfIdLatch().curSize;
		idex = containingExecutionEngine.getIdExRS().getBusy();
		idexfree = containingExecutionEngine.getIdExRS().getBusy()-containingExecutionEngine.getIdExRS().getExecuted();
		exmem = containingExecutionEngine.getExMemLatch().curSize;
		memwb = containingExecutionEngine.getMemWbLatch().curSize;
		robsize = containingExecutionEngine.getROB().rob.size();
		System.out.println("Distribution: "
				+ ifid
				+"-"+ idex
				+"("+(idexfree)+")"
				+"-"+ exmem
				+"-"+ memwb
				+" ROB:"+robsize);
		if (ifid==ifid_ && idex==idex_ && idexfree==idexfree_ && exmem==exmem_ 
				&& memwb==memwb_ && robsize==robsize_){
			System.out.println("Same variables");
		}
		if (ifid>0){
			System.out.println("Start Instructions");
		}
		
		ifid_ = ifid;
		idex_ = idex;
		idexfree_ = idexfree;
		exmem_ = exmem;
		memwb_ = memwb;
		robsize_ = robsize;
		
//		for (int i=0; i<ReservationStation.getRSSize(); i++){
//			if (containingExecutionEngine.getIdExRS().rs[i].busy && containingExecutionEngine.getIdExRS().rs[i].executionComplete)
//				System.out.println("ins::::"+containingExecutionEngine.getIdExRS().rs[i].opType);
//		}
		if (containingExecutionEngine.getIfIdLatch().curSize>0){
			System.out.println();
		}
		if (currentTime % getCoreStepSize() == 0
				&& containingExecutionEngine.isExecutionBegun() == true
				&& !containingExecutionEngine.getExecutionComplete()) {
			commit();
			writeback();
		}
		drainEventQueue(); // Process Memory Requests
		if (currentTime % getCoreStepSize() == 0
				&& containingExecutionEngine.isExecutionBegun() == true
				&& !containingExecutionEngine.getExecutionComplete()) {
			mem();
			exec();
			decode();
			fetch();

			// if(this.containingExecutionEngine.getStallFetch()>0){
			// this.containingExecutionEngine.decrementStallFetch(1);
			// }
		}
	}

	private void drainEventQueue() {
		eventQ.processEvents();
	}
	
	public void commit(){
		containingExecutionEngine.getCommitUnitIn().performCommit(this);
	}

	public void writeback() {
		containingExecutionEngine.getWriteBackUnitIn().performWriteBack(this);
	}

	public void mem() {
		containingExecutionEngine.getMemUnitIn().performMemEvent(this);
	}

	public void exec() {
		for (int i = 0; i < containingExecutionEngine.getExecUnitIns().length; i++) {
			containingExecutionEngine.getExecUnitIn(i).execute(this);
		}
	}

	public void decode() {
		containingExecutionEngine.getDecodeUnitIn().performDecode(this);
	}

	public void fetch() {
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
		this.coreStepSize = stepSize;
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

	public StageLatch_MII getIfIdLatch() {
		return this.ifId;
	}

	public ReservationStation getIdExRS() {
		return this.idEx;
	}
	
	public StageLatch_MII getExMemLatch() {
		return this.exMem;
	}

	public StageLatch_MII getMemWbLatch() {
		return this.memWb;
	}

	public StageLatch_MII getWbDoneLatch() {
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
	public void setTimingStatistics() {
		// Not needed here, set by inorderexecutionengine

	}

	@Override
	public void setPerCoreMemorySystemStatistics() {
		// Not needed here, set by inorderexecutionengine

	}
}
