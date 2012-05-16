package generic;

import pipeline.branchpredictor.TournamentPredictor;
//import pipeline.perfect.ExecutionEnginePerfect;
//import pipeline.perfect.PerformDecodeEventPerfect;
//import pipeline.perfect.PerformCommitsEventPerfect;
import pipeline.inorder.DecodeUnitIn;
import pipeline.inorder.ExecUnitIn;
import pipeline.inorder.ExecutionEngineIn;
import pipeline.inorder.FetchUnitIn;
import pipeline.inorder.InorderPipeline;
import pipeline.inorder.MemUnitIn;
import pipeline.inorder.RegFileIn;
import pipeline.inorder.WriteBackUnitIn;
import pipeline.outoforder.ExecutionEngine;
import pipeline.outoforder.PipelineInterface;
import pipeline.statistical.StatisticalPipeline;
import pipeline.statistical.StatisticalPipelineInterface;
import power.Counters;
import config.CoreConfig;
import config.SimulationConfig;
import config.SystemConfig;

/**
 * represents a single core
 * has it's own clock, and comprises of an execution engine and an event queue
 * all core parameters are defined here
 */

public class Core extends SimulationElement{
	
	//long clock;
	StatisticalPipeline statisticalPipeline;
	ExecutionEngine execEngine;
	EventQueue eventQueue;
	ExecutionEngineIn execEngineIn;
	
	public boolean isPipelineStatistical = SimulationConfig.isPipelineStatistical;
	public boolean isPipelineInorder = SimulationConfig.isPipelineInorder;
	
	//core parameters
	private int decodeWidth;
	private int issueWidth;
	private int retireWidth;
	private int decodeTime;
	private int renamingTime;
	private int reorderBufferSize;
	private int IWSize;
	private int integerRegisterFileSize;
	private int floatingPointRegisterFileSize;
	private int nIntegerArchitecturalRegisters;
	private int nFloatingPointArchitecturalRegisters;
	private int nMachineSpecificRegisters;
	private int noOfRegFilePorts;
	private int regFileOccupancy;
	private int branchMispredictionPenalty;
	private int[] nUnits;
	private int[] latencies;
	
	private int core_number;
	private int no_of_input_pipes;
	private int no_of_threads;
	private long coreCyclesTaken;

	private int[] threadIDs;
	
	private TournamentPredictor branchPredictor;
	
	private int noOfInstructionsExecuted;
	
	private pipeline.PipelineInterface pipelineInterface;
	public int numReturns;
	public Counters powerCounters;


//	private InorderPipeline inorderPipeline;

	
	public Core(int core_number,
			int no_of_input_pipes,
			int no_of_threads,
			InstructionLinkedList[] incomingInstructionLists,
			int[] threadIDs)
	{
		super(PortType.Unlimited, -1, -1, -1, SystemConfig.core[core_number].frequency);			//TODO frequency from config file
		
		this.eventQueue = new EventQueue();
		
		initializeCoreParameters(SystemConfig.core[core_number]);
		
		this.core_number = core_number;
		this.no_of_input_pipes = no_of_input_pipes;
		this.no_of_threads = no_of_threads;
		this.threadIDs = threadIDs;
		
		if (isPipelineStatistical)
			this.statisticalPipeline = new StatisticalPipeline(this);
		else if(isPipelineInorder)
			this.execEngineIn = new ExecutionEngineIn(this);
		else
			this.execEngine = new ExecutionEngine(this);
		
		this.branchPredictor = new TournamentPredictor();
		this.noOfInstructionsExecuted = 0;
		this.numReturns=0;
		if (this.isPipelineStatistical)
			this.pipelineInterface = new StatisticalPipelineInterface(this, eventQueue);
		else if(this.isPipelineInorder)
			this.pipelineInterface = new InorderPipeline(this, eventQueue);
		else
			this.pipelineInterface = new PipelineInterface(this, eventQueue);
		this.powerCounters = new Counters();
	}

	private void initializeCoreParameters(CoreConfig coreConfig)
	{
		//TODO parameters to be set according to contents of an XML configuration file
		setDecodeWidth(coreConfig.DecodeWidth);
		setIssueWidth(coreConfig.IssueWidth);
		setRetireWidth(coreConfig.RetireWidth);
		setDecodeTime(coreConfig.DecodeTime);
		setRenamingTime(coreConfig.RenamingTime);
		setReorderBufferSize(coreConfig.ROBSize);
		setIWSize(coreConfig.IWSize);
		setIntegerRegisterFileSize(coreConfig.IntRegFileSize);
		setFloatingPointRegisterFileSize(coreConfig.FloatRegFileSize);
		setNIntegerArchitecturalRegisters(coreConfig.IntArchRegNum);
		setNFloatingPointArchitecturalRegisters(coreConfig.FloatArchRegNum);
		setNMachineSpecificRegisters(coreConfig.MSRegNum);
		setNoOfRegFilePorts(coreConfig.RegFilePorts);
		setRegFileOccupancy(coreConfig.RegFileOccupancy);
		setBranchMispredictionPenalty(coreConfig.BranchMispredPenalty);
		
		nUnits = new int[FunctionalUnitType.no_of_types.ordinal()];
		latencies = new int[FunctionalUnitType.no_of_types.ordinal() + 2];
					// +2 because memory unit has L1 latency, L2 latency, main memory latency
		
		nUnits[FunctionalUnitType.integerALU.ordinal()] = coreConfig.IntALUNum;
		nUnits[FunctionalUnitType.integerMul.ordinal()] = coreConfig.IntMulNum;
		nUnits[FunctionalUnitType.integerDiv.ordinal()] = coreConfig.IntDivNum;
		nUnits[FunctionalUnitType.floatALU.ordinal()] = coreConfig.FloatALUNum;
		nUnits[FunctionalUnitType.floatMul.ordinal()] = coreConfig.FloatMulNum;
		nUnits[FunctionalUnitType.floatDiv.ordinal()] = coreConfig.FloatDivNum;
		nUnits[FunctionalUnitType.memory.ordinal()] = coreConfig.AddressFUNum;
		
		latencies[FunctionalUnitType.integerALU.ordinal()] = coreConfig.IntALULatency;
		latencies[FunctionalUnitType.integerMul.ordinal()] = coreConfig.IntMulLatency;
		latencies[FunctionalUnitType.integerDiv.ordinal()] = coreConfig.IntDivLatency;
		latencies[FunctionalUnitType.floatALU.ordinal()] = coreConfig.FloatALULatency;
		latencies[FunctionalUnitType.floatMul.ordinal()] = coreConfig.FloatMulLatency;
		latencies[FunctionalUnitType.floatDiv.ordinal()] = coreConfig.FloatDivLatency;
		latencies[FunctionalUnitType.memory.ordinal()] = coreConfig.AddressFULatency;
	}
	
	/*public void boot()
	{
		//set up initial events in the queue
		eventQueue.addEvent(new PerformDecodeEvent(GlobalClock.getCurrentTime(), this, 0));
//TODO commented only for perfect pipeline		
		if (perfectPipeline == false)
			eventQueue.addEvent(new PerformCommitsEvent(GlobalClock.getCurrentTime(), this));
	}*/
	
	/*public void work()
	{
		execEngine.work();
	}*/

	/*public long getClock() {
		return clock;
	}

	public void setClock(long clock) {
		this.clock = clock;
	}
	
	public void incrementClock()
	{
		this.clock++;
	}*/

	public int getIssueWidth() {
		return issueWidth;
	}

	public void setIssueWidth(int issueWidth) {
		this.issueWidth = issueWidth;
	}

	public int getRetireWidth() {
		return retireWidth;
	}

	public void setRetireWidth(int retireWidth) {
		this.retireWidth = retireWidth;
	}

	public EventQueue getEventQueue() {
		return eventQueue;
	}
	
	public void setEventQueue(EventQueue _eventQueue) {
		eventQueue = _eventQueue;
	}

	public ExecutionEngine getExecEngine() {
		return execEngine;
	}

	public int getBranchMispredictionPenalty() {
		return branchMispredictionPenalty;
	}

	public void setBranchMispredictionPenalty(int branchMispredictionPenalty) {
		this.branchMispredictionPenalty = branchMispredictionPenalty;
	}

	public int getDecodeWidth() {
		return decodeWidth;
	}

	public void setDecodeWidth(int decodeWidth) {
		this.decodeWidth = decodeWidth;
	}

	public int getFloatingPointRegisterFileSize() {
		return floatingPointRegisterFileSize;
	}

	public void setFloatingPointRegisterFileSize(int floatingPointRegisterFileSize) {
		this.floatingPointRegisterFileSize = floatingPointRegisterFileSize;
	}

	public int getIntegerRegisterFileSize() {
		return integerRegisterFileSize;
	}

	public void setIntegerRegisterFileSize(int integerRegisterFileSize) {
		this.integerRegisterFileSize = integerRegisterFileSize;
	}

	public int getNFloatingPointArchitecturalRegisters() {
		return nFloatingPointArchitecturalRegisters;
	}

	public void setNFloatingPointArchitecturalRegisters(
			int floatingPointArchitecturalRegisters) {
		nFloatingPointArchitecturalRegisters = floatingPointArchitecturalRegisters;
	}

	public int getNIntegerArchitecturalRegisters() {
		return nIntegerArchitecturalRegisters;
	}

	public void setNIntegerArchitecturalRegisters(int integerArchitecturalRegisters) {
		nIntegerArchitecturalRegisters = integerArchitecturalRegisters;
	}

	public int getNMachineSpecificRegisters() {
		return nMachineSpecificRegisters;
	}

	public void setNMachineSpecificRegisters(int machineSpecificRegisters) {
		nMachineSpecificRegisters = machineSpecificRegisters;
	}

	public int getReorderBufferSize() {
		return reorderBufferSize;
	}

	public void setReorderBufferSize(int reorderBufferSize) {
		this.reorderBufferSize = reorderBufferSize;
	}
	
	public int[] getAllNUnits()
	{
		return nUnits;
	}
	
	public int[] getAllLatencies()
	{
		return latencies;
	}
	
	public int getLatency(int FUType)
	{
		return latencies[FUType];
	}

	public int getDecodeTime() {
		return decodeTime;
	}

	public void setDecodeTime(int decodeTime) {
		this.decodeTime = decodeTime;
	}

	public int getRenamingTime() {
		return renamingTime;
	}

	public void setRenamingTime(int renamingTime) {
		this.renamingTime = renamingTime;
	}

	public int getIWSize() {
		return IWSize;
	}

	public void setIWSize(int size) {
		IWSize = size;
	}
	
	public int[] getThreadIDs() {
		return threadIDs;
	}

	public int getNo_of_input_pipes() {
		return no_of_input_pipes;
	}
	
	public int getNo_of_threads() {
		return no_of_threads;
	}
	
	public int getCore_number() {
		return core_number;
	}

	public TournamentPredictor getBranchPredictor() {
		return branchPredictor;
	}

	public int getNoOfRegFilePorts() {
		return noOfRegFilePorts;
	}

	public void setNoOfRegFilePorts(int noOfRegFilePorts) {
		this.noOfRegFilePorts = noOfRegFilePorts;
	}

	public int getRegFileOccupancy() {
		return regFileOccupancy;
	}

	public void setRegFileOccupancy(int regFileOccupancy) {
		this.regFileOccupancy = regFileOccupancy;
	}
	
	public int getNoOfInstructionsExecuted() {
		return noOfInstructionsExecuted;
	}

	public void setNoOfInstructionsExecuted(int noOfInstructionsExecuted) {
		this.noOfInstructionsExecuted = noOfInstructionsExecuted;
	}
	
	public void incrementNoOfInstructionsExecuted()
	{
		this.noOfInstructionsExecuted++;
	}
	
	public StatisticalPipeline getStatisticalPipeline() {
		return statisticalPipeline;
	}

	public void setStatisticalPipeline(StatisticalPipeline statisticalPipeline) {
		this.statisticalPipeline = statisticalPipeline;
	}
	

//	public InorderPipeline getInorderPipeline(){
//		return this.inorderPipeline;
//	}
	
	public ExecutionEngineIn getExecutionEngineIn(){
		return this.execEngineIn;
	}


	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}

	public pipeline.PipelineInterface getPipelineInterface() {
		return pipelineInterface;
	}
//	public void setInorderPipeline(InorderPipeline _inorderPipeline){
//		this.inorderPipeline = _inorderPipeline;
//	}
	public void setPipelineInterface(PipelineInterface pipelineInterface) {
		this.pipelineInterface = pipelineInterface;
	}
	
	public void setInputToPipeline(InstructionLinkedList[] inputsToPipeline)
	{
		if(this.isPipelineInorder){
			//System.out.println("Input to pipeline is set");			
			this.getExecutionEngineIn().getFetchUnitIn().setInputToPipeline(inputsToPipeline[0]);
		}
		else
			if (this.isPipelineStatistical)
				this.getStatisticalPipeline().getFetcher().setInputToPipeline(inputsToPipeline);
			else
				this.getExecEngine().getFetcher().setInputToPipeline(inputsToPipeline);

			
	}
	
	public void setStepSize(int stepSize)
	{
		this.stepSize = stepSize;
		this.pipelineInterface.setcoreStepSize(stepSize);
	}

	public long getCoreCyclesTaken() {
		return coreCyclesTaken;
	}

	public void setCoreCyclesTaken(long coreCyclesTaken) {
		this.coreCyclesTaken = coreCyclesTaken;
	}

}