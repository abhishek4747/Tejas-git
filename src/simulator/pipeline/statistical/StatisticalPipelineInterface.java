package pipeline.statistical;

import generic.Core;
import generic.EventQueue;

public class StatisticalPipelineInterface implements pipeline.PipelineInterface {
	
	Core core;
	EventQueue eventQ;
	int coreStepSize;
	
	public StatisticalPipelineInterface(Core core, EventQueue eventQ)
	{
		this.core = core;
		this.eventQ = eventQ;
	}

	@Override
	public void oneCycleOperation() {
		
		StatisticalPipeline statPipeline;
		
		statPipeline = core.getStatisticalPipeline();
		statPipeline.performCommits();
		if (statPipeline.isExecutionComplete() == false)
		{
			statPipeline.getFetcher().performFetch();
		}
		
		//handle events
		eventQ.processEvents();		
	}
	public Core getCore() {
		return core;
	}

	public void setCore(Core core) {
		this.core = core;
	}

	public EventQueue getEventQ() {
		return eventQ;
	}

	public void setEventQ(EventQueue eventQ) {
		this.eventQ = eventQ;
	}
	
	@Override
	public boolean isExecutionComplete() {
		
		/*if (core.isPipelineStatistical)
            return core.getStatisticalPipeline().isExecutionComplete();
        else*/
        return core.getStatisticalPipeline().isExecutionComplete();
		
		
	}
	
	public void setcoreStepSize(int stepSize)
	{
		this.coreStepSize = stepSize;
	}
	
	public int getCoreStepSize()
	{
		return coreStepSize;
	}
	
	public void resumePipeline()
	{
		
	}

}