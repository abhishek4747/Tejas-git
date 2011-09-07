package pipeline.perfect;

import memorysystem.CoreMemorySystem;
import generic.Core;

/**
 * execution engine comprises of : decode logic, ROB, instruction window, register files,
 * rename tables and functional units
 */
public class ExecutionEnginePerfect {

	//the containing core
	private Core core;
	
	//components of the execution engine
	private DecodeLogicPerfect decoder;

//	private ReorderBufferPerfect reorderBuffer;
	
	//Core-specific memory system (a set of LSQ, TLB and L1 cache)
	public CoreMemorySystem coreMemSys;
	
	//flags
	private boolean toStallDecode1;				//if physical register cannot be
												//allocated to the dest of an instruction,
												//all subsequent processing must stall
	
	private boolean toStallDecode2;				//if branch mis-predicted

	private boolean isExecutionComplete;		//TRUE indicates end of simulation
	private boolean isDecodePipeEmpty[];
	private boolean allPipesEmpty;

	public ExecutionEnginePerfect(Core containingCore)
	{
		core = containingCore;
		
		decoder = new DecodeLogicPerfect(core);
//		reorderBuffer = new ReorderBufferPerfect(core);
		
		toStallDecode1 = false;
		toStallDecode2 = false;
		isExecutionComplete = false;
		isDecodePipeEmpty = new boolean[core.getNo_of_threads()];
		allPipesEmpty = false;
	}
	
	/*public void work()
	{
		if(!isExecutionComplete && !toStallDecode2)
		{
			//commit instruction at head of ROB
			getReorderBuffer().performCommits();
			
			if(!isDecodePipeEmpty && !toStallDecode1)
			{
				//read decode pipe to add more instructions to ROB
				decoder.scheduleDecodeCompletion();
			}
		}
	}*/

	public Core getCore() {
		return core;
	}
	
	public DecodeLogicPerfect getDecoder() {
		return decoder;
	}

	public boolean isDecodePipeEmpty(int threadIndex) {
		return isDecodePipeEmpty[threadIndex];
	}

	public void setDecodePipeEmpty(int threadIndex, boolean isDecodePipeEmpty) {
		this.isDecodePipeEmpty[threadIndex] = isDecodePipeEmpty;
	}

	public boolean isExecutionComplete() {
		return isExecutionComplete;
	}

	public void setExecutionComplete(boolean isExecutionComplete) {
		this.isExecutionComplete = isExecutionComplete;
	}
	
//	public ReorderBufferPerfect getReorderBuffer() {
//		return reorderBuffer;
//	}

	public boolean isStallDecode1() {
		return toStallDecode1;
	}

	public void setStallDecode1(boolean stallDecode) {
		this.toStallDecode1 = stallDecode;
	}
	
	public boolean isStallDecode2() {
		return toStallDecode2;
	}

	public void setStallDecode2(boolean toStallDecode2) {
		this.toStallDecode2 = toStallDecode2;
	}
	
	public boolean isAllPipesEmpty() {
		return allPipesEmpty;
	}

	public void setAllPipesEmpty(boolean allPipesEmpty) {
		this.allPipesEmpty = allPipesEmpty;
	}


}