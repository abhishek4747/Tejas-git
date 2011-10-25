package pipeline.inorder;

import generic.Core;

public class InorderPipeline {
	Core core;
	
	public InorderPipeline(Core _core){
		this.core = _core;

	}
	
	public void startPipeline(){

		while(true){
			writeback();
			mem();
			exec();
			decode();
			fetch();
		}
	}
	
	public void writeback(){
		core.getExecutionEngineIn().getWriteBackUnitIn().performWriteBack();		
	}
	public void mem(){
		core.getExecutionEngineIn().getMemUnitIn().performMemEvent();
	}
	public void exec(){
		core.getExecutionEngineIn().getExecUnitIn().execute();
	}
	public void regfile(){
		core.getExecutionEngineIn().getRegFileIn().fetchOperands();
	}
	public void decode(){
		core.getExecutionEngineIn().getDecodeUnitIn().performDecode();
		regfile();
	}
	public void fetch(){
		core.getExecutionEngineIn().getFetchUnitIn().performFetch();
	}
}
