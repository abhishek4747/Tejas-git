package pipeline.inorder;

import pipeline.PipelineInterface;
import generic.Core;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Statistics;

public class InorderPipeline implements PipelineInterface{
	Core core;
	EventQueue eventQ;
	int coreStepSize;
	
	public InorderPipeline(Core _core, EventQueue eventQ){
		this.core = _core;
		this.eventQ = eventQ;
	}
	
	public void oneCycleOperation(){
		coreStepSize = core.getStepSize();
		writeback();
		mem();
		exec();
		decode();
		fetch();
//System.out.println("Ins executed = "+ core.getNoOfInstructionsExecuted());
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

	@Override
	public boolean isExecutionComplete() {
		// TODO Auto-generated method stub
		return (core.getExecutionEngineIn().getExecutionComplete());
		}

	@Override
	public int getCoreStepSize() {
		// TODO Auto-generated method stub
		return coreStepSize;
	}

	@Override
	public void setcoreStepSize(int stepSize) {
		// TODO Auto-generated method stub
		this.coreStepSize=stepSize;
	}

	@Override
	public void resumePipeline() {
		core.getExecutionEngineIn().getFetchUnitIn().resumePipeline();
		// TODO Auto-generated method stub
		
	}

	@Override
	public Core getCore() {
		// TODO Auto-generated method stub
		return core;
	}
	
	
}
