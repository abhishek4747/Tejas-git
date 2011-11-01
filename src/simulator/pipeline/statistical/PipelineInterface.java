package pipeline.statistical;

import generic.Core;
import generic.EventQueue;

public class PipelineInterface implements pipeline.PipelineInterface {
	
	Core core;
	EventQueue eventQ;
	
	public PipelineInterface(Core core, EventQueue eventQ)
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

}
