package pipeline.statistical;

import memorysystem.CoreMemorySystem;
import generic.Core;
import generic.InstructionLinkedList;
import pipeline.ExecutionEngine;

public class StatisticalExecutionEngine extends ExecutionEngine {
	
	Core core;
	StatisticalPipeline statisticalPipeline;
	
	public StatisticalExecutionEngine(Core core)
	{
		super();
		this.core = core;
		this.statisticalPipeline = new StatisticalPipeline(core, this);
	}

	@Override
	public void setInputToPipeline(InstructionLinkedList[] inpList) {
		
		this.statisticalPipeline.getFetcher().setInputToPipeline(inpList);
	}

	public void setCoreMemorySystem(CoreMemorySystem coreMemorySystem) {
		this.coreMemorySystem = coreMemorySystem;
		this.statisticalPipeline.coreMemSys = coreMemorySystem;
	}

	public StatisticalPipeline getStatisticalPipeline() {
		return statisticalPipeline;
	}

}
