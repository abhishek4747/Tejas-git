package pipeline.outoforder;

import memorysystem.CoreMemorySystem;
import generic.Core;

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
	
	//Core-specific memory system (a set of LSQ, TLB and L1 cache)
	public CoreMemorySystem coreMemSys;
	
	//flags
	private boolean toStallDecode1;				//if physical register cannot be
												//allocated to the dest of an instruction,
												//all subsequent processing must stall
	private boolean toStallDecode2;				//if branch mis-predicted
	private boolean isExecutionComplete;		//TRUE indicates end of simulation
	private boolean isDecodePipeEmpty;
	
	public ExecutionEngine(Core containingCore)
	{
		core = containingCore;
		
		decoder = new DecodeLogic(core);
		reorderBuffer = new ReorderBuffer(core);
		instructionWindow = new InstructionWindow(core);
		integerRegisterFile = new RegisterFile(core, core.getIntegerRegisterFileSize());
		integerRenameTable = new RenameTable(core.getNIntegerArchitecturalRegisters(), core.getIntegerRegisterFileSize(), integerRegisterFile);
		floatingPointRegisterFile = new RegisterFile(core, core.getFloatingPointRegisterFileSize());
		floatingPointRenameTable = new RenameTable(core.getNFloatingPointArchitecturalRegisters(), core.getFloatingPointRegisterFileSize(), floatingPointRegisterFile);
		machineSpecificRegisterFile = new RegisterFile(core, core.getNMachineSpecificRegisters());
		
		functionalUnitSet = new FunctionalUnitSet(core.getAllNUnits(),
													core.getAllLatencies());
		
		toStallDecode1 = false;
		toStallDecode2 = false;
		isExecutionComplete = false;
		isDecodePipeEmpty = false;
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
	
	public DecodeLogic getDecoder() {
		return decoder;
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


	public InstructionWindow getInstructionWindow() {
		return instructionWindow;
	}

	public void setInstructionWindow(InstructionWindow instructionWindow) {
		this.instructionWindow = instructionWindow;
	}


}