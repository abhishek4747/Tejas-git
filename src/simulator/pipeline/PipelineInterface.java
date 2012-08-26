package pipeline;

import generic.Core;

public interface PipelineInterface {
	
	public void oneCycleOperation();	
	public boolean isExecutionComplete();
	public boolean isAvailable();
	public void setcoreStepSize(int stepSize);
	public int getCoreStepSize();
	public void resumePipeline();
	public Core getCore();
	public boolean isSleeping();
	public void setTimingStatistics();
	public void setPerCoreMemorySystemStatistics();
	public void setPerCorePowerStatistics();
	public void setExecutionComplete(boolean status);
	public void setAvailable(boolean isAvailable);
	public void adjustRunningThreads(int adjval);
}
