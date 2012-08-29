package pipeline.outoforder;

import memorysystem.CoreMemorySystem;
import pipeline.ExecutionEngine;
import generic.Core;
import generic.Instruction;
import generic.InstructionLinkedList;

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
	private Instruction[] fetchBuffer;
	private DecodeLogic decoder;
	private ReorderBufferEntry[] decodeBuffer;
	private RenameLogic renamer;
	private ReorderBufferEntry[] renameBuffer;
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
	
	private OutOrderCoreMemorySystem outOrderCoreMemorySystem;
	
	//Core-specific memory system (a set of LSQ, TLB and L1 cache)
	//public CoreMemorySystem coreMemSys;
	
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

	//private boolean isExecutionComplete;		//TRUE indicates end of simulation
	private boolean isInputPipeEmpty[];
	private boolean allPipesEmpty;

	public long prevCycles;
	private boolean isAvailable;

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
		
		
		//iCacheBuffer = new ICacheBuffer(core.getDecodeWidth());
		fetchBuffer = new Instruction[core.getDecodeWidth()];
		fetcher = new FetchLogic(core, this);
		decodeBuffer = new ReorderBufferEntry[core.getDecodeWidth()];
		decoder = new DecodeLogic(core, this);
		renameBuffer = new ReorderBufferEntry[core.getDecodeWidth()];
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
		isInputPipeEmpty = new boolean[core.getNo_of_input_pipes()];
		allPipesEmpty = false;
		prevCycles=0;
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

	public boolean isInputPipeEmpty(int threadIndex) {
		return isInputPipeEmpty[threadIndex];
	}

	public void setInputPipeEmpty(int threadIndex, boolean isInputPipeEmpty) {
		this.isInputPipeEmpty[threadIndex] = isInputPipeEmpty;
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
	
	public boolean isAllPipesEmpty() {
		return allPipesEmpty;
	}

	public void setAllPipesEmpty(boolean allPipesEmpty) {
		this.allPipesEmpty = allPipesEmpty;
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
	
	public Instruction[] getFetchBuffer() {
		return fetchBuffer;
	}

	public ReorderBufferEntry[] getDecodeBuffer() {
		return decodeBuffer;
	}

	public ReorderBufferEntry[] getRenameBuffer() {
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
	public void setInputToPipeline(InstructionLinkedList[] inpList) {
		
		fetcher.setInputToPipeline(inpList);
		
	}
	
	public OutOrderCoreMemorySystem getCoreMemorySystem()
	{
		return outOrderCoreMemorySystem;
	}

	public void setCoreMemorySystem(CoreMemorySystem coreMemorySystem) {
		this.coreMemorySystem = coreMemorySystem;
		this.outOrderCoreMemorySystem = (OutOrderCoreMemorySystem)coreMemorySystem;
		System.out.println("icache buffer size = " + (int)(core.getDecodeWidth() *
											coreMemorySystem.getiCache().getLatency()));
		this.iCacheBuffer = new ICacheBuffer((int)(core.getDecodeWidth() *
											coreMemorySystem.getiCache().getLatency()));
		this.fetcher.setICacheBuffer(iCacheBuffer);
	}
}