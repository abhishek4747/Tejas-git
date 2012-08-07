package pipeline;

import generic.InstructionLinkedList;
import memorysystem.CoreMemorySystem;

public abstract class ExecutionEngine {
	
	protected boolean executionComplete;
	protected CoreMemorySystem coreMemorySystem;
	
	public ExecutionEngine()
	{
		executionComplete = false;
		coreMemorySystem = null;
	}
	
	public abstract void setInputToPipeline(InstructionLinkedList[] inpList);

	public void setExecutionComplete(boolean executionComplete) {
		this.executionComplete = executionComplete;
	}

	public boolean isExecutionComplete() {
		return executionComplete;
	}

	public void setCoreMemorySystem(CoreMemorySystem coreMemorySystem) {
		this.coreMemorySystem = coreMemorySystem;
	}

	public CoreMemorySystem getCoreMemorySystem() {
		return coreMemorySystem;
	}

}
