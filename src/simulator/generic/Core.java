package generic;

import pipeline.branchpredictor.TournamentPredictor;
import pipeline.outoforder.ExecutionEngine;
import pipeline.outoforder.PerformCommitsEvent;
import pipeline.outoforder.PerformDecodeEvent;
import config.CoreConfig;
import config.SystemConfig;

/**
 * represents a single core
 * has it's own clock, and comprises of an execution engine and an event queue
 * all core parameters are defined here
 */

public class Core extends SimulationElement{
	
	//long clock;
	ExecutionEngine execEngine;
	NewEventQueue eventQueue;
	
	//core parameters
	private int decodeWidth;
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
	private int no_of_threads;
	private InstructionList[] incomingInstructionLists;

	private int[] threadIDs;
	
	private TournamentPredictor branchPredictor;
	
	private int noOfInstructionsExecuted;

	public Core(int core_number, NewEventQueue eventQueue, int no_of_threads, InstructionList[] incomingInstructionLists,
					int[] threadIDs)
	{
		super(1, new Time_t(-1), new Time_t(-1), SystemConfig.core[core_number].frequency);			//TODO frequency from config file
		//clock = 0;
		
		initializeCoreParameters(SystemConfig.core[core_number]);
		
		//eventQueue = new EventQueue(this);
		this.core_number = core_number;
		this.eventQueue = eventQueue;
		this.no_of_threads = no_of_threads;
		this.incomingInstructionLists = incomingInstructionLists;
		this.threadIDs = threadIDs;
		this.execEngine = new ExecutionEngine(this);
		this.branchPredictor = new TournamentPredictor();
		this.noOfInstructionsExecuted = 0;
	}

	private void initializeCoreParameters(CoreConfig coreConfig)
	{
		//TODO parameters to be set according to contents of an XML configuration file
		setDecodeWidth(coreConfig.DecodeWidth);
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
	
	public void boot()
	{
		//set up initial events in the queue
		eventQueue.addEvent(new PerformDecodeEvent(GlobalClock.getCurrentTime(), this));
		eventQueue.addEvent(new PerformCommitsEvent(GlobalClock.getCurrentTime(), this));
	}
	
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

	public NewEventQueue getEventQueue() {
		return eventQueue;
	}
	
	public void setEventQueue(NewEventQueue _eventQueue) {
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

	public InstructionList getIncomingInstructions(int threadID) {
		int index = -1;
		for(int i = 0; i < no_of_threads; i++)
		{
			if(threadIDs[i] == threadID)
			{
				index = i;
				break;
			}
		}
		
		if(threadID == -1 || index == -1)
		{
			misc.Error.showErrorAndExit("threadID of -1");
		}
		
		return incomingInstructionLists[index];
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
	public InstructionList[] getIncomingInstructionLists() {
		return incomingInstructionLists;
	}

	public void setIncomingInstructionLists(InstructionList[] incomingInstructionLists) {
		this.incomingInstructionLists = incomingInstructionLists;
	}

}