package pipeline.inorder.multiissue;

import generic.Core;
import generic.EventQueue;
import generic.GenericCircularQueue;
import generic.GlobalClock;
import generic.Instruction;
import pipeline.PipelineInterface;
import pipeline.inorder.InorderExecutionEngine;
import pipeline.inorder.InorderPipeline;

public class MultiIssueInorder implements PipelineInterface {

	private InorderPipeline[] pipelines;
	private int numPipelines;
	private Core core;
	private EventQueue eventQ;
	private int coreStepSize;
	InorderExecutionEngine containingExecutionEngine;
	
	public MultiIssueInorder(Core core, EventQueue eventQ){
		this.core = core;
		containingExecutionEngine = (InorderExecutionEngine)core.getExecEngine();
		this.eventQ = eventQ;
		this.coreStepSize = core.getStepSize();
		this.numPipelines = containingExecutionEngine.getNumPipelines();
		pipelines = new InorderPipeline[numPipelines];
		for(int i=0;i<numPipelines;i++)
					pipelines[i] = new InorderPipeline(core, eventQ, i);
	}
	@Override
	/* Run all inorder pipelines */
	public void oneCycleOperation() {
		long currentTime = GlobalClock.getCurrentTime();
		if(currentTime % getCoreStepSize()==0 && !containingExecutionEngine.getExecutionComplete()){
	 		for(int i=0;i<numPipelines;i++)
					pipelines[i].writeback();
		}
		drainEventQueue();
		if(currentTime % getCoreStepSize()==0 && !containingExecutionEngine.getExecutionComplete()){
			
			for(int i=0;i<numPipelines;i++)
						pipelines[i].mem();
	
			for(int i=0;i<numPipelines;i++){
					pipelines[i].exec();
			}
		
			for(int i=0;i<numPipelines;i++){
					pipelines[i].decode();
			}
			
			if(containingExecutionEngine.getStallFetch()>0)
				containingExecutionEngine.decrementStallFetch(1);
			else
				for(int i=0;i<numPipelines;i++)
					pipelines[i].fetch();				
		}		
	}

	private void drainEventQueue(){
		eventQ.processEvents();		
	}
	@Override
	public boolean isExecutionComplete() {
		return containingExecutionEngine.getExecutionComplete();
	}

	@Override
	public void setcoreStepSize(int stepSize) {
		this.coreStepSize=stepSize;
	}

	@Override
	public int getCoreStepSize() {
		return this.core.getStepSize();
	}

	@Override
	public void resumePipeline() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Core getCore() {
		return this.core;
	}

	@Override
	public boolean isSleeping() {
		// TODO Auto-generated method stub
		return false;
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
	public void setPerCorePowerStatistics() {
		// TODO Auto-generated method stub
		
	}
	public InorderPipeline getInorderPipeLine()
	{
		return this.pipelines[0];
	}
	@Override
	public void setExecutionComplete(boolean status) {
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getMispredCount() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getNoOfIHits() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getNoOfIMisses() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getNoOfIRequests() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getNoOfL1Hits() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getNoOfL1Misses() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getNoOfL1Requests() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getNoOfLoads() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getNoOfMemRequests() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getNoOfStores() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getNoOfTLBHits() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getNoOfTLBMisses() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getNoOfTLBRequests() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getNoOfValueForwards() {
		// TODO Auto-generated method stub
		return 0;
	}

}
