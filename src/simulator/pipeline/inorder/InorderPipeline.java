package pipeline.inorder;

import generic.Core;

public class InorderPipeline {
	Core core;
	StageLatch ifId,idEx,exMem,memWb,wbDone;
	
	public InorderPipeline(Core _core){
		this.core = _core;
		this.ifId = new StageLatch(_core);
		this.idEx = new StageLatch(_core);
		this.exMem = new StageLatch(_core);
		this.memWb = new StageLatch(_core);
		this.wbDone = new StageLatch(_core);
	}
	
	public void startPipeline(){
		core.setFetchUnitIn(new FetchUnitIn(core));
		core.setDecodeUnitIn(new DecodeUnitIn(core));
		core.setRegFileIn(new RegFileIn(core));
		core.setExecUnitIn(new ExecUnitIn(core));
		core.setMemUnitIn(new MemUnitIn(core));
		core.setWriteBackUnitIn(new WriteBackUnitIn(core));
		while(true){
			writeback();
			mem();
			exec();
			decode();
			fetch();
		}
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
	
	public void writeback(){
		core.getWriteBackUnitIn().performWriteBack();		
	}
	public void mem(){
		core.getMemUnitIn().performMemEvent();
	}
	public void exec(){
		core.getExecUnitIn().execute();
	}
	public void regfile(){
		core.getRegFileIn().fetchOperands();
	}
	public void decode(){
		core.getDecodeUnitIn().performDecode();
		regfile();
	}
	public void fetch(){
		core.getFetchUnitIn().performFetch();
	}
}
