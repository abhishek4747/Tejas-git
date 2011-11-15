package pipeline;

public interface PipelineInterface {
	
	public void oneCycleOperation();	
	public boolean isExecutionComplete();
	public void setcoreStepSize(int stepSize);
	public int getCoreStepSize();
	public void resumePipeline();
}
