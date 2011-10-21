package generic;

import pipeline.outoforder_new_arch.ExecutionEngine;
import pipeline.statistical.StatisticalPipeline;

public class PipelineInterface {
	
	static Core[] cores;
	static EventQueue eventQ;
	
	public static void advanceBy(long numberOfCycles)
	{
		long currentTime = GlobalClock.currentTime;
		for(int i = 0; i < numberOfCycles; i++)
		{			
			oneCycleOperation(currentTime);
			currentTime++;
			GlobalClock.incrementClock();
		}
	}
	
	public static void runToCompletion()
	{
		long currentTime = GlobalClock.getCurrentTime();
		boolean queueComplete = true;		
		for(int i = 0; i < cores.length; i++)
		{
			if (cores[i].isPipelineStatistical)
				queueComplete = queueComplete && cores[i].getStatisticalPipeline().isExecutionComplete();
			else
				queueComplete = queueComplete && cores[i].getExecEngine().isExecutionComplete();
		}
		
		while(!queueComplete)
		{
			oneCycleOperation(currentTime);
			
			GlobalClock.incrementClock();
			currentTime++;
			
			queueComplete = true;		
			for(int i = 0; i < cores.length; i++)
			{
				if (cores[i].isPipelineStatistical)
					queueComplete = queueComplete && cores[i].getStatisticalPipeline().isExecutionComplete();
				else
					queueComplete = queueComplete && cores[i].getExecEngine().isExecutionComplete();
			}
		}
	}
	
	public static void oneCycleOperation(long cycleNumber)
	{
		ExecutionEngine execEngine;
		StatisticalPipeline statPipeline;
		
		if(cycleNumber%cores[0].getStepSize() == 0)
		{
			for(int i = 0; i < cores.length; i++)
			{
				if (!cores[i].isPipelineStatistical)
				{
					execEngine = cores[i].getExecEngine();
					execEngine.getReorderBuffer().performCommits();
					if(execEngine.isExecutionComplete() == false)
					{
						execEngine.getWriteBackLogic().performWriteBack();
						execEngine.getSelector().performSelect();
					}
				}
				else //Statistical Pipeline
				{
					statPipeline = cores[i].getStatisticalPipeline();
					statPipeline.performCommits();
					if (statPipeline.isExecutionComplete() == false)
					{
						statPipeline.getFetcher().performFetch();
					}
				}
			}
		}
		
		//handle events
		eventQ.processEvents();
		
		if(cycleNumber%cores[0].getStepSize() == 0)
		{
			for(int i = 0; i < cores.length 
				&& !cores[i].isPipelineStatistical; i++)
			{
				execEngine = cores[i].getExecEngine();
				if(execEngine.isExecutionComplete() == false)
				{
					execEngine.getIWPusher().performIWPush();
					execEngine.getRenamer().performRename();
					execEngine.getDecoder().performDecode();
					execEngine.getFetcher().performFetch();
				}
			}
		}
	}

	public static Core[] getCores() {
		return cores;
	}

	public static void setCores(Core[] cores) {
		PipelineInterface.cores = cores;
	}

	public static EventQueue getEventQ() {
		return eventQ;
	}

	public static void setEventQ(EventQueue eventQ) {
		PipelineInterface.eventQ = eventQ;
	}

}
