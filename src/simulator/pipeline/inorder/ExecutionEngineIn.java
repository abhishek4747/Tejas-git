package pipeline.inorder;

import memorysystem.CoreMemorySystem;
import generic.Core;

public class ExecutionEngineIn {
	
	Core core;
	StageLatch ifId,idEx,exMem,memWb,wbDone;
	private FetchUnitIn fetchUnitIn;
	private DecodeUnitIn decodeUnitIn;
	private RegFileIn regFileIn;
	private ExecUnitIn execUnitIn;
	private MemUnitIn memUnitIn;
	private WriteBackUnitIn writeBackUnitIn;
	private boolean executionComplete;
	private CoreMemorySystem coreMemorySystem;

	public ExecutionEngineIn(Core _core){
		this.core = _core;
		this.ifId = new StageLatch(_core);
		this.idEx = new StageLatch(_core);
		this.exMem = new StageLatch(_core);
		this.memWb = new StageLatch(_core);
		this.wbDone = new StageLatch(_core);
		this.setFetchUnitIn(new FetchUnitIn(core,core.getEventQueue()));
		this.setDecodeUnitIn(new DecodeUnitIn(core));
		this.setRegFileIn(new RegFileIn(core));
		this.setExecUnitIn(new ExecUnitIn(core));
		this.setMemUnitIn(new MemUnitIn(core));
		this.setWriteBackUnitIn(new WriteBackUnitIn(core));
		this.executionComplete=false;

	}

	public FetchUnitIn getFetchUnitIn(){
		return this.fetchUnitIn;
	}
	public DecodeUnitIn getDecodeUnitIn(){
		return this.decodeUnitIn;
	}
	public RegFileIn getRegFileIn(){
		return this.regFileIn;
	}
	public ExecUnitIn getExecUnitIn(){
		return this.execUnitIn;
	}
	public MemUnitIn getMemUnitIn(){
		return this.memUnitIn;
	}
	public WriteBackUnitIn getWriteBackUnitIn(){
		return this.writeBackUnitIn;
	}
	public void setFetchUnitIn(FetchUnitIn _fetchUnitIn){
		this.fetchUnitIn = _fetchUnitIn;
	}
	public void setDecodeUnitIn(DecodeUnitIn _decodeUnitIn){
		this.decodeUnitIn = _decodeUnitIn;
	}
	public void setRegFileIn(RegFileIn _regFileIn){
		this.regFileIn = _regFileIn;
	}
	public void setExecUnitIn(ExecUnitIn _execUnitIn){
		this.execUnitIn = _execUnitIn;
	}
	public void setMemUnitIn(MemUnitIn _memUnitIn){
		this.memUnitIn = _memUnitIn;
	}
	public void setWriteBackUnitIn(WriteBackUnitIn _wbUnitIn){
		this.writeBackUnitIn = _wbUnitIn;
	}
	public void setCoreMemorySystem(CoreMemorySystem coreMemSys){
		this.coreMemorySystem=coreMemSys;
	}
	public void setExecutionComplete(boolean execComplete){
		this.executionComplete=execComplete;
	}
	public StageLatch getIfIdLatch(){
		return this.ifId;
	}
	public StageLatch getIdExLatch(){
		return this.idEx;
	}
	public StageLatch getExMemLatch(){
		return this.exMem;
	}
	public StageLatch getMemWbLatch(){
		return this.memWb;
	}
	public StageLatch getWbDoneLatch(){
		return this.wbDone;
	}
	public CoreMemorySystem getCoreMemorySystem(){
		return this.coreMemorySystem;
	}
	public boolean getExecutionComplete(){
		return this.executionComplete;
	}
}
