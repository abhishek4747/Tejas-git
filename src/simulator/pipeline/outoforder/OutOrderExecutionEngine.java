package pipeline.outoforder;

import memorysystem.CoreMemorySystem;
import pipeline.ExecutionEngine;
import generic.Core;
import generic.GenericCircularQueue;
import generic.Instruction;

/**
 * execution engine comprises of : decode logic, ROB, instruction window, register files,
 * rename tables and functional units
 */
public class OutOrderExecutionEngine extends ExecutionEngine {

	//the containing core
	private Core core;
	
	//components of the execution engine
	private ICacheBuffer iCacheBuffer;
	private FetchLogic fetcher;
	private GenericCircularQueue<Instruction> fetchBuffer;
	private DecodeLogic decoder;
	private GenericCircularQueue<ReorderBufferEntry> decodeBuffer;
	private RenameLogic renamer;
	private GenericCircularQueue<ReorderBufferEntry> renameBuffer;
	private IWPushLogic IWPusher;
	private SelectLogic selector;
	private ExecutionLogic executer;
	private WriteBackLogic writeBackLogic;

	private ReorderBuffer reorderBuffer;
	private InstructionWindow instructionWindow;
	private RegisterFile integerRegisterFile;
	private RegisterFile floatingPointRegisterFile;
	private RegisterFile machineSpecificRegisterFile[];
	private RenameTable integerRenameTable;
	private RenameTable floatingPointRenameTable;
	private FunctionalUnitSet functionalUnitSet;
	
	//Core-specific memory system (a set of LSQ, TLB and L1 cache)
	private OutOrderCoreMemorySystem outOrderCoreMemorySystem;
	
	//flags
	private boolean toStall1;					//if IW full
												//fetcher, decoder and renamer stall

	private boolean toStall2;					//if physical register cannot be
												//allocated to the dest of an instruction,
												//all subsequent processing must stall
												//fetcher and decoder stall
		
	private boolean toStall3;					//if LSQ full, and a load/store needs to be
												//allocated an entry
												//fetcher stall
	
	private boolean toStall4;					//if ROB full
												//fetcher stall
	
	private boolean toStall5;					//if branch mis-predicted
												//fetcher stall
	
	private boolean toStall6[];					//if physical register (msr) cannot be
												//allocated to the dest of an instruction,
												//all subsequent processing of that thread must stall
												//fetcher and decoder stall

	
	public long prevCycles;

	public OutOrderExecutionEngine(Core containingCore)
	{
		super();
		
		core = containingCore;
		
		
		reorderBuffer = new ReorderBuffer(core, this);
		instructionWindow = new InstructionWindow(core, this);
		integerRegisterFile = new RegisterFile(core, core.getIntegerRegisterFileSize());
		integerRenameTable = new RenameTable(core.getNIntegerArchitecturalRegisters(), core.getIntegerRegisterFileSize(), integerRegisterFile, core.getNo_of_input_pipes());
		floatingPointRegisterFile = new RegisterFile(core, core.getFloatingPointRegisterFileSize());
		floatingPointRenameTable = new RenameTable(core.getNFloatingPointArchitecturalRegisters(), core.getFloatingPointRegisterFileSize(), floatingPointRegisterFile, core.getNo_of_input_pipes());
		machineSpecificRegisterFile = new RegisterFile[core.getNo_of_input_pipes()];
		for(int i = 0; i < core.getNo_of_input_pipes(); i++)
		{
			machineSpecificRegisterFile[i] = new RegisterFile(core, core.getNMachineSpecificRegisters());
		}
		
		functionalUnitSet = new FunctionalUnitSet(core.getAllNUnits(),
													core.getAllLatencies());
		
		
		fetchBuffer = new GenericCircularQueue(Instruction.class, core.getDecodeWidth());
		fetcher = new FetchLogic(core, this);
		decodeBuffer = new GenericCircularQueue(ReorderBufferEntry.class, core.getDecodeWidth());
		decoder = new DecodeLogic(core, this);
		renameBuffer = new GenericCircularQueue(ReorderBufferEntry.class, core.getDecodeWidth());
		renamer = new RenameLogic(core, this);
		IWPusher = new IWPushLogic(core, this);
		selector = new SelectLogic(core, this);
		executer = new ExecutionLogic(core, this);
		writeBackLogic = new WriteBackLogic(core, this);
		
		
		toStall1 = false;
		toStall2 = false;
		toStall3 = false;
		toStall4 = false;
		toStall5 = false;
		toStall6 = new boolean[1];
		for(int i = 0; i < 1; i++)
		{
			toStall6[i] = false;
		}
		prevCycles=0;
	}
	
	public ICacheBuffer getiCacheBuffer() {
		return iCacheBuffer;
	}

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

	public RegisterFile getMachineSpecificRegisterFile(int threadID) {
		return machineSpecificRegisterFile[threadID];
	}
	
	public ReorderBuffer getReorderBuffer() {
		return reorderBuffer;
	}

	public InstructionWindow getInstructionWindow() {
		return instructionWindow;
	}

	public void setInstructionWindow(InstructionWindow instructionWindow) {
		this.instructionWindow = instructionWindow;
	}
	
	public boolean isToStall1() {
		return toStall1;
	}

	public void setToStall1(boolean toStall1) {
		this.toStall1 = toStall1;
	}

	public boolean isToStall2() {
		return toStall2;
	}

	public void setToStall2(boolean toStall2) {
		this.toStall2 = toStall2;
	}

	public boolean isToStall3() {
		return toStall3;
	}

	public void setToStall3(boolean toStall3) {
		this.toStall3 = toStall3;
	}

	public boolean isToStall4() {
		return toStall4;
	}

	public void setToStall4(boolean toStall4) {
		this.toStall4 = toStall4;
	}

	public boolean isToStall5() {
		return toStall5;
	}

	public void setToStall5(boolean toStall5) {
		this.toStall5 = toStall5;
	}

	public boolean isToStall6() {
		return toStall6[0];
	}

	public void setToStall6(boolean toStall5) {
		this.toStall6[0] = toStall5;
	}
	
	public GenericCircularQueue<Instruction> getFetchBuffer() {
		return fetchBuffer;
	}

	public GenericCircularQueue<ReorderBufferEntry> getDecodeBuffer() {
		return decodeBuffer;
	}

	public GenericCircularQueue<ReorderBufferEntry> getRenameBuffer() {
		return renameBuffer;
	}
	
	public FetchLogic getFetcher() {
		return fetcher;
	}

	public RenameLogic getRenamer() {
		return renamer;
	}

	public IWPushLogic getIWPusher() {
		return IWPusher;
	}

	public SelectLogic getSelector() {
		return selector;
	}

	public ExecutionLogic getExecuter() {
		return executer;
	}

	public WriteBackLogic getWriteBackLogic() {
		return writeBackLogic;
	}

	@Override
	public void setInputToPipeline(GenericCircularQueue<Instruction>[] inpList) {
		
		fetcher.setInputToPipeline(inpList);
		
	}
	
	public OutOrderCoreMemorySystem getCoreMemorySystem()
	{
		return outOrderCoreMemorySystem;
	}

	public void setCoreMemorySystem(CoreMemorySystem coreMemorySystem) {
		this.coreMemorySystem = coreMemorySystem;
		this.outOrderCoreMemorySystem = (OutOrderCoreMemorySystem)coreMemorySystem;
		this.iCacheBuffer = new ICacheBuffer((int)(core.getDecodeWidth() *
											coreMemorySystem.getiCache().getLatency()));
		this.fetcher.setICacheBuffer(iCacheBuffer);
	}
}