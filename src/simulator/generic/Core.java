package generic;

import pipeline.ExecutionEngine;
import pipeline.branchpredictor.AlwaysNotTaken;
import pipeline.branchpredictor.AlwaysTaken;
import pipeline.branchpredictor.BimodalPredictor;
import pipeline.branchpredictor.BranchPredictor;
import pipeline.branchpredictor.GAgpredictor;
import pipeline.branchpredictor.GApPredictor;
import pipeline.branchpredictor.GShare;
import pipeline.branchpredictor.NoPredictor;
import pipeline.branchpredictor.PAgPredictor;
import pipeline.branchpredictor.PApPredictor;
import pipeline.branchpredictor.PerfectPredictor;
import pipeline.branchpredictor.TournamentPredictor;
//import pipeline.perfect.ExecutionEnginePerfect;
//import pipeline.perfect.PerformDecodeEventPerfect;
//import pipeline.perfect.PerformCommitsEventPerfect;
import pipeline.inorder.InorderExecutionEngine;

import pipeline.inorder.InorderPipeline;
import pipeline.inorder.multiissue.MultiIssueInorder;
import pipeline.outoforder.OutOrderExecutionEngine;
import pipeline.outoforder.OutOfOrderPipeline;
import power.Counters;
import config.BranchPredictorConfig;
import config.BranchPredictorConfig.BP;
import config.CoreConfig;
import config.SimulationConfig;
import config.SystemConfig;

/**
 * represents a single core
 * has it's own clock, and comprises of an execution engine and an event queue
 * all core parameters are defined here
 */

public class Core {
	
	//long clock;
	
	Port port;
	int stepSize;
	long frequency;
	ExecutionEngine execEngine;
	public EventQueue eventQueue;
	public int currentThreads;
	
	public boolean isPipelineStatistical = SimulationConfig.isPipelineStatistical;
	public boolean isPipelineInorder = SimulationConfig.isPipelineInorder;
	public boolean isPipelineMultiIssueInorder = SimulationConfig.isPipelineMultiIssueInorder;

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
	
	private BranchPredictor branchPredictor;
	
	private int noOfInstructionsExecuted;
	
	private pipeline.PipelineInterface pipelineInterface;
	public int numReturns;
	public Counters powerCounters;
	private int numInorderPipelines;
	public CoreBcastBus coreBcastBus;
	public int barrier_latency;
	public boolean TreeBarrier;
	public int barrierUnit; //0=>central 1=>distributed

//	private InorderPipeline inorderPipeline;

	
	public Core(int core_number,
			int no_of_input_pipes,
			int no_of_threads,
			InstructionLinkedList[] incomingInstructionLists,
			int[] threadIDs)
	{
		//super(PortType.Unlimited, -1, -1, -1, SystemConfig.core[core_number].frequency);			//TODO frequency from config file
		this.port = new Port(PortType.Unlimited, -1, -1);
		this.eventQueue = new EventQueue();
		this.frequency = SystemConfig.core[core_number].frequency;
		initializeCoreParameters(SystemConfig.core[core_number]);
		
		this.core_number = core_number;
		this.no_of_input_pipes = no_of_input_pipes;
		this.no_of_threads = no_of_threads;
		this.threadIDs = threadIDs;
		this.currentThreads =0;
		if(this.isPipelineInorder)
			this.execEngine = new InorderExecutionEngine(this,1);
		else if(this.isPipelineMultiIssueInorder)
			this.execEngine = new InorderExecutionEngine(this,this.numInorderPipelines);
		else
			this.execEngine = new OutOrderExecutionEngine(this);
		
		if(SystemConfig.branchPredictor.predictorMode == BP.NoPredictor)
			this.branchPredictor = new NoPredictor();
		else if(SystemConfig.branchPredictor.predictorMode == BP.PerfectPredictor)
			this.branchPredictor = new PerfectPredictor();
		else if(SystemConfig.branchPredictor.predictorMode == BP.AlwaysTaken)
			this.branchPredictor = new AlwaysTaken();
		else if(SystemConfig.branchPredictor.predictorMode == BP.AlwaysNotTaken)
			this.branchPredictor = new AlwaysNotTaken();
		else if(SystemConfig.branchPredictor.predictorMode == BP.Tournament)
			this.branchPredictor = new TournamentPredictor();
		else if(SystemConfig.branchPredictor.predictorMode == BP.Bimodal)
			this.branchPredictor = new BimodalPredictor(SystemConfig.branchPredictor.PCBits,
					SystemConfig.branchPredictor.saturating_bits);
		else if(SystemConfig.branchPredictor.predictorMode == BP.GShare)
			this.branchPredictor = new GShare(SystemConfig.branchPredictor.BHRsize, 
					SystemConfig.branchPredictor.saturating_bits);
		else if(SystemConfig.branchPredictor.predictorMode == BP.GAg)
			this.branchPredictor = new GAgpredictor(SystemConfig.branchPredictor.BHRsize);
		else if(SystemConfig.branchPredictor.predictorMode == BP.GAp)
			this.branchPredictor = new GApPredictor(SystemConfig.branchPredictor.BHRsize, 
					SystemConfig.branchPredictor.PCBits);
		else if(SystemConfig.branchPredictor.predictorMode == BP.PAg)
			this.branchPredictor = new PAgPredictor(SystemConfig.branchPredictor.PCBits, 
					SystemConfig.branchPredictor.BHRsize, 
					SystemConfig.branchPredictor.saturating_bits);
		else if(SystemConfig.branchPredictor.predictorMode == BP.PAp)
			this.branchPredictor = new PApPredictor(SystemConfig.branchPredictor.PCBits, 
					SystemConfig.branchPredictor.BHRsize, 
					SystemConfig.branchPredictor.saturating_bits);
		
		
		this.noOfInstructionsExecuted = 0;
		this.numReturns=0;
		if(this.isPipelineInorder)
			this.pipelineInterface = new InorderPipeline(this, eventQueue,0);
		else if(this.isPipelineMultiIssueInorder)
			this.pipelineInterface = new MultiIssueInorder(this, eventQueue);
		else
			this.pipelineInterface = new OutOfOrderPipeline(this, eventQueue);
		this.powerCounters = new Counters();
	}
	public void setCoreBcastBus(CoreBcastBus coreBcastBus){
		this.coreBcastBus = coreBcastBus;
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
		setBranchMispredictionPenalty(coreConfig.BranchMispredPenalty);
		setNumInorderPipelines(SimulationConfig.numInorderPipelines);
		setTreeBarrier(coreConfig.TreeBarrier);
		setBarrierLatency(coreConfig.barrierLatency);
		setBarrierUnit(coreConfig.barrierUnit);
		
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
	
	private void setBarrierLatency(int barrierLatency) {
		this.barrier_latency = barrierLatency;
		
	}
	private void setBarrierUnit(int barrierUnit){
		this.barrierUnit = barrierUnit;
	}
	public void activatePipeline(){
		this.pipelineInterface.resumePipeline();
	}
	public void sleepPipeline(){
		
		((InorderExecutionEngine)this.getExecEngine()).getFetchUnitIn().inputToPipeline.enqueue(Instruction.getSyncInstruction());
	}

	public void setTreeBarrier(boolean bar)
	{
		TreeBarrier = bar;
	}
	public int getIssueWidth() {
		return issueWidth;
	}

	public int getNumInorderPipelines() {
		return numInorderPipelines;
	}

	public void setNumInorderPipelines(int numInorderPipelines) {
		this.numInorderPipelines = numInorderPipelines;
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

	public BranchPredictor getBranchPredictor() {
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
	
	
//	public InorderPipeline getInorderPipeline(){
//		return this.inorderPipeline;
//	}
	
	

	
	public pipeline.PipelineInterface getPipelineInterface() {
		return pipelineInterface;
	}
//	public void setInorderPipeline(InorderPipeline _inorderPipeline){
//		this.inorderPipeline = _inorderPipeline;
//	}
	public void setPipelineInterface(OutOfOrderPipeline pipelineInterface) {
		this.pipelineInterface = pipelineInterface;
	}
	
	public void setInputToPipeline(GenericCircularQueue<Instruction>[] inputsToPipeline)
	{
		this.getExecEngine().setInputToPipeline(inputsToPipeline);
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
	
	public long getFrequency()
	{
		return this.frequency;
	}
	
	public void setFrequency(long frequency)
	{
		this.frequency = frequency;
	}
	
	public int getStepSize()
	{
		return stepSize;
	}	
}