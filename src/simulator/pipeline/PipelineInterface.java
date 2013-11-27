package pipeline;

import generic.Core;
import generic.GenericCircularQueue;
import generic.Instruction;

public interface PipelineInterface {
	
	public void oneCycleOperation();	
	public boolean isExecutionComplete();
	public void setcoreStepSize(int stepSize);
	public int getCoreStepSize();
	public void resumePipeline();
	public Core getCore();
	public boolean isSleeping();
	public void setTimingStatistics();
	public void setPerCoreMemorySystemStatistics();
	public void setExecutionComplete(boolean status);
	public void adjustRunningThreads(int adjval);
	public void setInputToPipeline(GenericCircularQueue<Instruction>[] inputToPipeline);
	
	public long getBranchCount();
	public long getMispredCount();
	public long getNoOfMemRequests();
	public long getNoOfLoads();
	public long getNoOfStores();
	public long getNoOfValueForwards();
	public long getNoOfTLBRequests();
	public long getNoOfTLBHits();
	public long getNoOfTLBMisses();
	public long getNoOfL1Requests();
	public long getNoOfL1Hits();
	public long getNoOfL1Misses();
	public long getNoOfIRequests();
	public long getNoOfIHits();
	public long getNoOfIMisses();
}
