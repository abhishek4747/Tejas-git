package pipeline.outoforder;

import generic.Core;
import generic.EventQueue;

/**
 * execution engine comprises of : decode logic, ROB, instruction window, register files,
 * rename tables and functional units
 */
public class ExecutionEngine {

	//the containing core
	private Core core;
	
	//components of the execution engine
	private DecodeLogic decoder;
	private ReorderBuffer reorderBuffer;
	private InstructionWindow instructionWindow;
	private RegisterFile integerRegisterFile;
	private RegisterFile floatingPointRegisterFile;
	private RegisterFile machineSpecificRegisterFile;
	private RenameTable integerRenameTable;
	private RenameTable floatingPointRenameTable;
	private FunctionalUnitSet functionalUnitSet;
	
	//flags
	private boolean toStallDecode;				//if physical register cannot be
												//allocated to the dest of an instruction,
												//all subsequent processing must stall
	private boolean isExecutionComplete;		//TRUE indicates end of simulation
	private boolean isDecodePipeEmpty;
	
	private EventQueue eventQ;
	
	public ExecutionEngine(Core containingCore)
	{
		core = containingCore;
		
		decoder = new DecodeLogic(core);
		reorderBuffer = new ReorderBuffer(core);
		instructionWindow = new InstructionWindow(core);
		integerRegisterFile = new RegisterFile(core.getIntegerRegisterFileSize());
		integerRenameTable = new RenameTable(core.getNIntegerArchitecturalRegisters(), core.getIntegerRegisterFileSize(), integerRegisterFile);
		floatingPointRegisterFile = new RegisterFile(core.getFloatingPointRegisterFileSize());
		floatingPointRenameTable = new RenameTable(core.getNFloatingPointArchitecturalRegisters(), core.getFloatingPointRegisterFileSize(), floatingPointRegisterFile);
		machineSpecificRegisterFile = new RegisterFile(core.getNMachineSpecificRegisters());
		eventQ = core.getEventQueue();
		
		functionalUnitSet = new FunctionalUnitSet(core.getAllNUnits(),
													core.getAllLatencies());
		
		toStallDecode = false;
		isExecutionComplete = false;
		isDecodePipeEmpty = false;
	}
	
	public void boot()
	{
		while(!isExecutionComplete)
		{
			core.incrementClock();
			
			//commit instruction at head of ROB
			getReorderBuffer().performCommits();
			
			//process events that are scheduled for the current clock cycle
			eventQ.processEvents();
			
			if(!isDecodePipeEmpty && !toStallDecode)
			{
				//read decode pipe to add more instructions to ROB
				decoder.scheduleDecodeCompletion();
			}
		}
	}

	public Core getCore() {
		return core;
	}

	public RegisterFile getFloatingPointRegisterFile() {
		return floatingPointRegisterFile;
	}

	public RenameTable getFloatingPointRenameTable() {
		return floatingPointRenameTable;
	}

	public FunctionalUnitSet getFunctionalUnitSet() {
		return functionalUnitSet;
	}

	public RegisterFile getIntegerRegisterFile() {
		return integerRegisterFile;
	}

	public RenameTable getIntegerRenameTable() {
		return integerRenameTable;
	}

	public boolean isDecodePipeEmpty() {
		return isDecodePipeEmpty;
	}

	public void setDecodePipeEmpty(boolean isDecodePipeEmpty) {
		this.isDecodePipeEmpty = isDecodePipeEmpty;
	}

	public boolean isExecutionComplete() {
		return isExecutionComplete;
	}

	public void setExecutionComplete(boolean isExecutionComplete) {
		this.isExecutionComplete = isExecutionComplete;
	}

	public RegisterFile getMachineSpecificRegisterFile() {
		return machineSpecificRegisterFile;
	}
	
	public ReorderBuffer getReorderBuffer() {
		return reorderBuffer;
	}

	public boolean isStallDecode() {
		return toStallDecode;
	}

	public void setStallDecode(boolean stallDecode) {
		this.toStallDecode = stallDecode;
	}

	public InstructionWindow getInstructionWindow() {
		return instructionWindow;
	}

	public void setInstructionWindow(InstructionWindow instructionWindow) {
		this.instructionWindow = instructionWindow;
	}


}