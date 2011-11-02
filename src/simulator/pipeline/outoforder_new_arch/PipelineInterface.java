package pipeline.outoforder_new_arch;

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
		
		ExecutionEngine execEngine;		
		
		execEngine = core.getExecEngine();
		execEngine.getReorderBuffer().performCommits();
		if(execEngine.isExecutionComplete() == false)
		{
			execEngine.getWriteBackLogic().performWriteBack();
			execEngine.getSelector().performSelect();
		}
		
		/*else //Statistical Pipeline
		{
			statPipeline = core.getStatisticalPipeline();
			statPipeline.performCommits();
			if (statPipeline.isExecutionComplete() == false)
			{
				statPipeline.getFetcher().performFetch();
			}
		}*/
		
		//handle events
		eventQ.processEvents();
		
		execEngine = core.getExecEngine();
		if(execEngine.isExecutionComplete() == false)
		{
			execEngine.getIWPusher().performIWPush();
			execEngine.getRenamer().performRename();
			execEngine.getDecoder().performDecode();
			execEngine.getFetcher().performFetch();
		}
		
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
        return core.getExecEngine().isExecutionComplete();
		
		
	}

}
