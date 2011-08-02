package generic;

import pipeline.outoforder.ExecutionEngine;
import pipeline.outoforder.PerformCommitsEvent;
import pipeline.outoforder.PerformDecodeEvent;

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
	private int branchMispredictionPenalty;
	private int[] nUnits;
	private int[] latencies;
	
	private InstructionList incomingInstructions;
	private int threadID;

	public Core(InstructionList incomingInstructions,
					int threadID)
	{
		super(-1, new Time_t(-1), new Time_t(-1));
		//clock = 0;
		
		//eventQueue = new EventQueue(this);
		execEngine = new ExecutionEngine(this);
		this.incomingInstructions = incomingInstructions;
		this.threadID = threadID;
		
		initializeCoreParameters();
	}
	
	private void initializeCoreParameters()
	{
		//TODO parameters to be set according to contents of an xml configuration file
		setDecodeWidth(4);
		setDecodeTime(1);
		setRenamingTime(2);
		setReorderBufferSize(128);
		setIWSize(128);
		setIntegerRegisterFileSize(128);
		setFloatingPointRegisterFileSize(128);
		setNIntegerArchitecturalRegisters(32);
		setNFloatingPointArchitecturalRegisters(32);
		setNMachineSpecificRegisters(64);
		setBranchMispredictionPenalty(50);
		
		nUnits = new int[FunctionalUnitType.no_of_types.ordinal()];
		latencies = new int[FunctionalUnitType.no_of_types.ordinal() + 2];
					// +2 because memory unit has L1 latency, L2 latency, main memory latency
		
		nUnits[FunctionalUnitType.integerALU.ordinal()] = 4;
		nUnits[FunctionalUnitType.integerMul.ordinal()] = 1;
		nUnits[FunctionalUnitType.integerDiv.ordinal()] = 1;
		nUnits[FunctionalUnitType.floatALU.ordinal()] = 2;
		nUnits[FunctionalUnitType.floatMul.ordinal()] = 1;
		nUnits[FunctionalUnitType.floatDiv.ordinal()] = 1;		
		nUnits[FunctionalUnitType.memory.ordinal()] = 4;
		
		latencies[FunctionalUnitType.integerALU.ordinal()] = 1;
		latencies[FunctionalUnitType.integerMul.ordinal()] = 4;
		latencies[FunctionalUnitType.integerDiv.ordinal()] = 8;
		latencies[FunctionalUnitType.floatALU.ordinal()] = 2;
		latencies[FunctionalUnitType.floatMul.ordinal()] = 8;
		latencies[FunctionalUnitType.floatDiv.ordinal()] = 16;
		latencies[FunctionalUnitType.memory.ordinal()] = 2;
		latencies[FunctionalUnitType.memory.ordinal()+1] = 20;
		latencies[FunctionalUnitType.memory.ordinal()+2] = 100;
	}
	
	public void boot()
	{
		//set up initial events in the queue
		eventQueue.addEvent(new PerformDecodeEvent(0, this));
		eventQueue.addEvent(new PerformCommitsEvent(0, this));
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
	
	public int getThreadID() {
		return threadID;
	}

	public InstructionList getIncomingInstructions() {
		return incomingInstructions;
	}

}