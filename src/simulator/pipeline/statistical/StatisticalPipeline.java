package pipeline.statistical;

import pipeline.outoforder.ReorderBufferEntry;
import config.SimulationConfig;
import memorysystem.CoreMemorySystem;
import generic.Core;
import generic.GlobalClock;
import generic.Instruction;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;
import generic.Statistics;

public class StatisticalPipeline 
{	
	//the containing core
	private Core core;
	
	//components of the execution engine
	private FetchEngine fetcher;
//	private Instruction[] fetchBuffer;
	//Core-specific memory system (a set of LSQ, TLB and L1 cache)
	public CoreMemorySystem coreMemSys;
	
	//flags
	private boolean toStall;					//if IW full
												//fetcher, decoder and renamer stall

	private boolean isExecutionComplete;		//TRUE indicates end of simulation
	private boolean isInputPipeEmpty[];
	private boolean allPipesEmpty;
	

	public StatisticalPipeline(Core containingCore)
	{
		core = containingCore;
				
//		fetchBuffer = new Instruction[core.getDecodeWidth()];
		fetcher = new FetchEngine(core, this);
		
		toStall = false;
		isExecutionComplete = false;
		isInputPipeEmpty = new boolean[core.getNo_of_input_pipes()];
		for (int i = 0; i < isInputPipeEmpty.length; i++)
			isInputPipeEmpty[i] = false;
		allPipesEmpty = false;
	}

	public void performCommits()
	{
		coreMemSys.getLsqueue().processROBCommitForStatisticalPipeline(core.getEventQueue());
		if(this.isAllPipesEmpty() && coreMemSys.getLsqueue().isEmpty())
		{
			this.setExecutionComplete(true);
			
			setTimingStatistics();			
			setPerCoreMemorySystemStatistics();
		}
//		core.incrementNoOfInstructionsExecuted();
	}
	
	protected Core getCore() {
		return core;
	}


	protected void setCore(Core core) {
		this.core = core;
	}


	public FetchEngine getFetcher() {
		return fetcher;
	}


	protected void setFetcher(FetchEngine fetcher) {
		this.fetcher = fetcher;
	}


//	protected Instruction[] getFetchBuffer() {
//		return fetchBuffer;
//	}
//
//
//	protected void setFetchBuffer(Instruction[] fetchBuffer) {
//		this.fetchBuffer = fetchBuffer;
//	}


	protected CoreMemorySystem getCoreMemSys() {
		return coreMemSys;
	}


	protected void setCoreMemSys(CoreMemorySystem coreMemSys) {
		this.coreMemSys = coreMemSys;
	}


	protected boolean isToStall() {
		return toStall;
	}


	protected void setToStall(boolean toStall) {
		this.toStall = toStall;
	}


	public boolean isExecutionComplete() {
		return isExecutionComplete;
	}


	public void setExecutionComplete(boolean isExecutionComplete) {
		this.isExecutionComplete = isExecutionComplete;
	}


	public boolean isInputPipeEmpty(int threadIndex) {
		return isInputPipeEmpty[threadIndex];
	}

	public void setInputPipeEmpty(int threadIndex, boolean isInputPipeEmpty) {
		this.isInputPipeEmpty[threadIndex] = isInputPipeEmpty;
	}


	protected boolean isAllPipesEmpty() {
		return allPipesEmpty;
	}


	protected void setAllPipesEmpty(boolean allPipesEmpty) {
		this.allPipesEmpty = allPipesEmpty;
	}
	

	public void setTimingStatistics()
	{
		Statistics.setCoreCyclesTaken(GlobalClock.getCurrentTime()/core.getStepSize(), core.getCore_number());
		Statistics.setCoreFrequencies(core.getFrequency(), core.getCore_number());
		Statistics.setNumCoreInstructions(core.getNoOfInstructionsExecuted(), core.getCore_number());
	}
	
	public void setPerCoreMemorySystemStatistics()
	{
		Statistics.setNoOfMemRequests(coreMemSys.getLsqueue().noOfMemRequests, core.getCore_number());
		Statistics.setNoOfLoads(coreMemSys.getLsqueue().NoOfLd, core.getCore_number());
		Statistics.setNoOfStores(coreMemSys.getLsqueue().NoOfSt, core.getCore_number());
		Statistics.setNoOfValueForwards(coreMemSys.getLsqueue().NoOfForwards, core.getCore_number());
		Statistics.setNoOfTLBRequests(coreMemSys.getTLBuffer().getTlbRequests(), core.getCore_number());
		Statistics.setNoOfTLBHits(coreMemSys.getTLBuffer().getTlbHits(), core.getCore_number());
		Statistics.setNoOfTLBMisses(coreMemSys.getTLBuffer().getTlbMisses(), core.getCore_number());
		Statistics.setNoOfL1Requests(coreMemSys.getL1Cache().noOfRequests, core.getCore_number());
		Statistics.setNoOfL1Hits(coreMemSys.getL1Cache().hits, core.getCore_number());
		Statistics.setNoOfL1Misses(coreMemSys.getL1Cache().misses, core.getCore_number());
	}
}
