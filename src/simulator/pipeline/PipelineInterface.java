package pipeline;

import generic.Core;

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
	public void setPerCorePowerStatistics();
}
