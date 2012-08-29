package pipeline.inorder.multiissue;

import generic.Core;
import generic.EventQueue;
import generic.GlobalClock;
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
	/*
	 * Only those pipelines are run in the following one cycle operation whose id is less than the 
	 * stall id currently being set. i.e. all the pipelines after the stalled pipeline are stalled to maintain the 
	 * "inorder" characteristic.
	 * */
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
/*
		for(int i=0;i<numPipelines;i++)
			pipelines[i].oneCycleOperation();
		if(this.core.getExecutionEngineIn().getStallFetch()>0)
			this.core.getExecutionEngineIn().decrementStallFetch(1);
*/		
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

}
