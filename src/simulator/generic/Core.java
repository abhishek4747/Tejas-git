package generic;

import java.io.FileWriter;
import java.io.IOException;

import main.ArchitecturalComponent;
import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import memorysystem.nuca.NucaCache;
import net.BusInterface;
import net.NocInterface;
import pipeline.ExecutionEngine;
import pipeline.multi_issue_inorder.InorderCoreMemorySystem_MII;
import pipeline.multi_issue_inorder.MultiIssueInorderExecutionEngine;
import pipeline.multi_issue_inorder.MultiIssueInorderPipeline;
import pipeline.outoforder.OutOfOrderPipeline;
import pipeline.outoforder.OutOrderCoreMemorySystem;
import pipeline.outoforder.OutOrderExecutionEngine;
import config.CoreConfig;
import config.EnergyConfig;
import config.PipelineType;
import config.SystemConfig;

/**
 * represents a single core
 * has it's own clock, and comprises of an execution engine and an event queue
 * all core parameters are defined here
 */

public class Core extends SimulationElement{
	
	//long clock;
	public static NucaCache nucaCache;
	Port port;
	int stepSize;
	long frequency;
	ExecutionEngine execEngine;
	public EventQueue eventQueue;
	public int currentThreads;
		
	public boolean isPipelineInOrder() {
		return (SystemConfig.core[this.core_number].pipelineType==PipelineType.inOrder);
	}
	
	public boolean isPipelineOutOfOrder() {
		return (SystemConfig.core[this.core_number].pipelineType==PipelineType.outOfOrder);
	}
	

	//core parameters
	private int decodeWidth;
	private int issueWidth;
	private int retireWidth;
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
	private int[] reciprocalOfThroughputs;
	
	//core power parameters
	private EnergyConfig bPredPower;
	private EnergyConfig decodePower;
	private EnergyConfig intRATPower;
	private EnergyConfig floatRATPower;
	private EnergyConfig intFreeListPower;
	private EnergyConfig floatFreeListPower;
	private EnergyConfig lsqPower;
	private EnergyConfig intRegFilePower;
	private EnergyConfig floatRegFilePower;
	private EnergyConfig iwPower;
	private EnergyConfig robPower;
	private EnergyConfig intALUPower;
	private EnergyConfig floatALUPower;
	private EnergyConfig complexALUPower;
	private EnergyConfig resultsBroadcastBusPower;
	private EnergyConfig iTLBPower;
	private EnergyConfig dTLBPower;
	
	private int core_number;
	private int no_of_input_pipes;
	private int no_of_threads;
	private long coreCyclesTaken;
	
	private int[] threadIDs;
	
	private long noOfInstructionsExecuted;
	
	private pipeline.PipelineInterface pipelineInterface;
	public int numReturns;
	private int numInorderPipelines;
	
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
		super(PortType.Unlimited, -1, -1, -1, SystemConfig.core[core_number].frequency);	
		//TODO frequency from config file
		
		this.port = new Port(PortType.Unlimited, -1, -1);
		this.eventQueue = new EventQueue();
		this.frequency = SystemConfig.core[core_number].frequency;
		initializeCoreParameters(SystemConfig.core[core_number]);
		
		this.core_number = core_number;
		this.no_of_input_pipes = no_of_input_pipes;
		this.no_of_threads = no_of_threads;
		this.threadIDs = threadIDs;
		this.currentThreads =0;

		this.noOfInstructionsExecuted = 0;
		this.numReturns=0;

		// Create execution engine
		if(this.isPipelineInOrder()) {
			this.execEngine = new MultiIssueInorderExecutionEngine(this, issueWidth);
		} else if (isPipelineOutOfOrder()){
			this.execEngine = new OutOrderExecutionEngine(this);
		} else {
			misc.Error.showErrorAndExit("pipeline type not identified : " + 
				SystemConfig.core[core_number].pipelineType);
		}
		
		// Create pipeline interface
		if(isPipelineInOrder()) {
			this.pipelineInterface = new MultiIssueInorderPipeline(this, eventQueue);
		} else if (isPipelineOutOfOrder()) {
			this.pipelineInterface = new OutOfOrderPipeline(this, eventQueue);
		} else {
			misc.Error.showErrorAndExit("pipeline type not identified : " + 
				SystemConfig.core[core_number].pipelineType);
		}
		
		// Create core memory interface
		CoreMemorySystem coreMemSys = null;
		if(isPipelineInOrder()) {
			coreMemSys = new InorderCoreMemorySystem_MII(this);
		} else if (isPipelineOutOfOrder()) {
			coreMemSys = new  OutOrderCoreMemorySystem(this);
		} else {
			misc.Error.showErrorAndExit("pipeline type not identified : " + 
				SystemConfig.core[core_number].pipelineType);
		}
		
		this.execEngine.setCoreMemorySystem(coreMemSys);
		ArchitecturalComponent.coreMemSysArray.add(coreMemSys);
		
		setPowerConfigs();
	}
	
	private void setPowerConfigs()
	{
		CoreConfig coreConfig = SystemConfig.core[getCore_number()];
		bPredPower = coreConfig.bPredPower;
		decodePower = coreConfig.decodePower;
		intRATPower = coreConfig.intRATPower;
		floatRATPower = coreConfig.floatRATPower;
		intFreeListPower = coreConfig.intFreeListPower;
		floatFreeListPower = coreConfig.floatFreeListPower;
		lsqPower = coreConfig.lsqPower;
		intRegFilePower = coreConfig.intRegFilePower;
		floatRegFilePower = coreConfig.floatRegFilePower;
		iwPower = coreConfig.iwPower;
		robPower = coreConfig.robPower;
		intALUPower = coreConfig.intALUPower;
		floatALUPower = coreConfig.floatALUPower;
		complexALUPower = coreConfig.complexALUPower;
		resultsBroadcastBusPower = coreConfig.resultsBroadcastBusPower;
		iTLBPower = coreConfig.iTLBPower;
		dTLBPower = coreConfig.dTLBPower;
	}
	
	private void initializeCoreParameters(CoreConfig coreConfig)
	{
		//TODO parameters to be set according to contents of an XML configuration file
		setDecodeWidth(coreConfig.DecodeWidth);
		setIssueWidth(coreConfig.IssueWidth);
		setRetireWidth(coreConfig.RetireWidth);
		setReorderBufferSize(coreConfig.ROBSize);
		setIWSize(coreConfig.IWSize);
		setIntegerRegisterFileSize(coreConfig.IntRegFileSize);
		setFloatingPointRegisterFileSize(coreConfig.FloatRegFileSize);
		setNIntegerArchitecturalRegisters(coreConfig.IntArchRegNum);
		setNFloatingPointArchitecturalRegisters(coreConfig.FloatArchRegNum);
		
		setBranchMispredictionPenalty(coreConfig.BranchMispredPenalty);
		setBranchMispredictionPenalty(coreConfig.BranchMispredPenalty);
		setNumInorderPipelines(coreConfig.IssueWidth);
		setTreeBarrier(coreConfig.TreeBarrier);
		setBarrierLatency(coreConfig.barrierLatency);
		setBarrierUnit(coreConfig.barrierUnit);
		
		nUnits = new int[FunctionalUnitType.no_of_types.ordinal()];
		latencies = new int[FunctionalUnitType.no_of_types.ordinal()];
					// +2 because memory unit has L1 latency, L2 latency, main memory latency
		reciprocalOfThroughputs = new int[FunctionalUnitType.no_of_types.ordinal()];
		
		nUnits[FunctionalUnitType.integerALU.ordinal()] = coreConfig.IntALUNum;
		nUnits[FunctionalUnitType.integerMul.ordinal()] = coreConfig.IntMulNum;
		nUnits[FunctionalUnitType.integerDiv.ordinal()] = coreConfig.IntDivNum;
		nUnits[FunctionalUnitType.floatALU.ordinal()] = coreConfig.FloatALUNum;
		nUnits[FunctionalUnitType.floatMul.ordinal()] = coreConfig.FloatMulNum;
		nUnits[FunctionalUnitType.floatDiv.ordinal()] = coreConfig.FloatDivNum;
		
		latencies[FunctionalUnitType.integerALU.ordinal()] = coreConfig.IntALULatency;
		latencies[FunctionalUnitType.integerMul.ordinal()] = coreConfig.IntMulLatency;
		latencies[FunctionalUnitType.integerDiv.ordinal()] = coreConfig.IntDivLatency;
		latencies[FunctionalUnitType.floatALU.ordinal()] = coreConfig.FloatALULatency;
		latencies[FunctionalUnitType.floatMul.ordinal()] = coreConfig.FloatMulLatency;
		latencies[FunctionalUnitType.floatDiv.ordinal()] = coreConfig.FloatDivLatency;
		
		reciprocalOfThroughputs[FunctionalUnitType.integerALU.ordinal()] = coreConfig.IntALUReciprocalOfThroughput;
		reciprocalOfThroughputs[FunctionalUnitType.integerMul.ordinal()] = coreConfig.IntMulReciprocalOfThroughput;
		reciprocalOfThroughputs[FunctionalUnitType.integerDiv.ordinal()] = coreConfig.IntDivReciprocalOfThroughput;
		reciprocalOfThroughputs[FunctionalUnitType.floatALU.ordinal()] = coreConfig.FloatALUReciprocalOfThroughput;
		reciprocalOfThroughputs[FunctionalUnitType.floatMul.ordinal()] = coreConfig.FloatMulReciprocalOfThroughput;
		reciprocalOfThroughputs[FunctionalUnitType.floatDiv.ordinal()] = coreConfig.FloatDivReciprocalOfThroughput;
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
		
		((MultiIssueInorderExecutionEngine)this.getExecEngine()).getFetchUnitIn().inputToPipeline.enqueue(Instruction.getSyncInstruction());
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
	
	public int[] getAllReciprocalsOfThroughputs()
	{
		return reciprocalOfThroughputs;
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
	
	public long getNoOfInstructionsExecuted() {
		return noOfInstructionsExecuted;
	}

	public void setNoOfInstructionsExecuted(long noOfInstructionsExecuted) {
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
	
	@Override
	public void handleEvent(EventQueue eventQ, Event event) 
	{
	}	
	public EnergyConfig getbPredPower() {
		return bPredPower;
	}

	public void setbPredPower(EnergyConfig bPredPower) {
		this.bPredPower = bPredPower;
	}

	public EnergyConfig getDecodePower() {
		return decodePower;
	}

	public void setDecodePower(EnergyConfig decodePower) {
		this.decodePower = decodePower;
	}

	public EnergyConfig getIntRATPower() {
		return intRATPower;
	}

	public void setIntRATPower(EnergyConfig intRATPower) {
		this.intRATPower = intRATPower;
	}

	public EnergyConfig getFpRATPower() {
		return floatRATPower;
	}

	public void setFpRATPower(EnergyConfig fpRATPower) {
		this.floatRATPower = fpRATPower;
	}

	public EnergyConfig getIntFreeListPower() {
		return intFreeListPower;
	}

	public void setIntFreeListPower(EnergyConfig intFreeListPower) {
		this.intFreeListPower = intFreeListPower;
	}

	public EnergyConfig getFpFreeListPower() {
		return floatFreeListPower;
	}

	public void setFpFreeListPower(EnergyConfig fpFreeListPower) {
		this.floatFreeListPower = fpFreeListPower;
	}

	public EnergyConfig getLsqPower() {
		return lsqPower;
	}

	public void setLsqPower(EnergyConfig lsqPower) {
		this.lsqPower = lsqPower;
	}

	public EnergyConfig getIntRegFilePower() {
		return intRegFilePower;
	}

	public void setIntRegFilePower(EnergyConfig intRegFilePower) {
		this.intRegFilePower = intRegFilePower;
	}

	public EnergyConfig getFpRegFilePower() {
		return floatRegFilePower;
	}

	public void setFpRegFilePower(EnergyConfig fpRegFilePower) {
		this.floatRegFilePower = fpRegFilePower;
	}

	public EnergyConfig getIwPower() {
		return iwPower;
	}

	public void setIwPower(EnergyConfig iwPower) {
		this.iwPower = iwPower;
	}

	public EnergyConfig getRobPower() {
		return robPower;
	}

	public void setRobPower(EnergyConfig robPower) {
		this.robPower = robPower;
	}

	public EnergyConfig getIntALUPower() {
		return intALUPower;
	}

	public void setIntALUPower(EnergyConfig intALUPower) {
		this.intALUPower = intALUPower;
	}

	public EnergyConfig getFloatALUPower() {
		return floatALUPower;
	}

	public void setFloatALUPower(EnergyConfig floatALUPower) {
		this.floatALUPower = floatALUPower;
	}

	public EnergyConfig getComplexALUPower() {
		return complexALUPower;
	}

	public void setComplexALUPower(EnergyConfig complexALUPower) {
		this.complexALUPower = complexALUPower;
	}

	public EnergyConfig getResultsBroadcastBusPower() {
		return resultsBroadcastBusPower;
	}

	public void setResultsBroadcastBusPower(EnergyConfig resultsBroadcastBusPower) {
		this.resultsBroadcastBusPower = resultsBroadcastBusPower;
	}

	public EnergyConfig getiTLBPower() {
		return iTLBPower;
	}

	public void setiTLBPower(EnergyConfig iTLBPower) {
		this.iTLBPower = iTLBPower;
	}

	public EnergyConfig getdTLBPower() {
		return dTLBPower;
	}

	public void setdTLBPower(EnergyConfig dTLBPower) {
		this.dTLBPower = dTLBPower;
	}

	public void setComInterface(CommunicationInterface comInterface) {
		this.comInterface = comInterface;
		this.getExecEngine().getCoreMemorySystem().setComInterface(comInterface);
		for(Cache cache : getExecEngine().getCoreMemorySystem().getCoreCacheList()) {
			cache.setComInterface(comInterface);
		}
	}

	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException
	{
		EnergyConfig totalPower = new EnergyConfig(0, 0);
		
		if(coreCyclesTaken == 0)
		{
			return totalPower;
		}
		
		outputFileWriter.write("\n\n");
		
		// --------- Core Memory System -------------------------
		EnergyConfig iCachePower =  this.execEngine.getCoreMemorySystem().getiCache().calculateAndPrintEnergy(outputFileWriter, componentName + ".iCache");
		totalPower.add(totalPower, iCachePower);
		EnergyConfig iTLBPower =  this.execEngine.getCoreMemorySystem().getiTLB().calculateAndPrintEnergy(outputFileWriter, componentName + ".iTLB");
		totalPower.add(totalPower, iTLBPower);
		
		EnergyConfig dCachePower =  this.execEngine.getCoreMemorySystem().getL1Cache().calculateAndPrintEnergy(outputFileWriter, componentName + ".dCache");
		totalPower.add(totalPower, dCachePower);
		
		EnergyConfig dTLBPower =  this.execEngine.getCoreMemorySystem().getdTLB().calculateAndPrintEnergy(outputFileWriter, componentName + ".dTLB");
		totalPower.add(totalPower, dTLBPower);
		
		// -------- Pipeline -----------------------------------
		EnergyConfig pipelinePower =  this.execEngine.calculateAndPrintEnergy(outputFileWriter, componentName + ".pipeline");
		totalPower.add(totalPower, pipelinePower);
		
		totalPower.printEnergyStats(outputFileWriter, componentName + ".total");
		
		return totalPower;
	}
}
