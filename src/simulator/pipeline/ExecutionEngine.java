package pipeline;

import java.io.FileWriter;
import java.io.IOException;

import config.PowerConfigNew;
import config.SystemConfig;
import config.BranchPredictorConfig.BP;
import pipeline.branchpredictor.AlwaysNotTaken;
import pipeline.branchpredictor.AlwaysTaken;
import pipeline.branchpredictor.BimodalPredictor;
import pipeline.branchpredictor.BranchPredictor;
import pipeline.branchpredictor.GAgpredictor;
import pipeline.branchpredictor.GApPredictor;
import pipeline.branchpredictor.GShare;
import pipeline.branchpredictor.NoPredictor;
import pipeline.branchpredictor.PAgPredictor;
import pipeline.branchpredictor.PApPredictor;
import pipeline.branchpredictor.PerfectPredictor;
import pipeline.branchpredictor.TournamentPredictor;
import generic.Core;
import generic.GenericCircularQueue;
import generic.Instruction;
import memorysystem.CoreMemorySystem;

public abstract class ExecutionEngine {
	
	protected Core containingCore;
	protected boolean executionComplete;
	protected CoreMemorySystem coreMemorySystem;

	private long instructionMemStall;
	
	private BranchPredictor branchPredictor;
	
	public ExecutionEngine(Core containingCore)
	{
		this.containingCore = containingCore;
		executionComplete = false;
		coreMemorySystem = null;
		instructionMemStall=0;
		
		if(SystemConfig.branchPredictor.predictorMode == BP.NoPredictor)
			this.branchPredictor = new NoPredictor(this);
		else if(SystemConfig.branchPredictor.predictorMode == BP.PerfectPredictor)
			this.branchPredictor = new PerfectPredictor(this);
		else if(SystemConfig.branchPredictor.predictorMode == BP.AlwaysTaken)
			this.branchPredictor = new AlwaysTaken(this);
		else if(SystemConfig.branchPredictor.predictorMode == BP.AlwaysNotTaken)
			this.branchPredictor = new AlwaysNotTaken(this);
		else if(SystemConfig.branchPredictor.predictorMode == BP.Tournament)
			this.branchPredictor = new TournamentPredictor(this);
		else if(SystemConfig.branchPredictor.predictorMode == BP.Bimodal)
			this.branchPredictor = new BimodalPredictor(this, SystemConfig.branchPredictor.PCBits,
					SystemConfig.branchPredictor.saturating_bits);
		else if(SystemConfig.branchPredictor.predictorMode == BP.GShare)
			this.branchPredictor = new GShare(this, SystemConfig.branchPredictor.BHRsize, 
					SystemConfig.branchPredictor.saturating_bits);
		else if(SystemConfig.branchPredictor.predictorMode == BP.GAg)
			this.branchPredictor = new GAgpredictor(this, SystemConfig.branchPredictor.BHRsize);
		else if(SystemConfig.branchPredictor.predictorMode == BP.GAp)
			this.branchPredictor = new GApPredictor(this, SystemConfig.branchPredictor.BHRsize, 
					SystemConfig.branchPredictor.PCBits);
		else if(SystemConfig.branchPredictor.predictorMode == BP.PAg)
			this.branchPredictor = new PAgPredictor(this, SystemConfig.branchPredictor.PCBits, 
					SystemConfig.branchPredictor.BHRsize, 
					SystemConfig.branchPredictor.saturating_bits);
		else if(SystemConfig.branchPredictor.predictorMode == BP.PAp)
			this.branchPredictor = new PApPredictor(this, SystemConfig.branchPredictor.PCBits, 
					SystemConfig.branchPredictor.BHRsize, 
					SystemConfig.branchPredictor.saturating_bits);
	}
	
	public abstract void setInputToPipeline(GenericCircularQueue<Instruction>[] inpList);

	public void setExecutionComplete(boolean executionComplete) {
		this.executionComplete = executionComplete;
	}

	public boolean isExecutionComplete() {
		return executionComplete;
	}

	public void setCoreMemorySystem(CoreMemorySystem coreMemorySystem) {
		this.coreMemorySystem = coreMemorySystem;
	}

	public CoreMemorySystem getCoreMemorySystem() {
		return coreMemorySystem;
	}

	public void incrementInstructionMemStall(int i) {
		this.instructionMemStall += i;
		
	}

	public long getInstructionMemStall() {
		return instructionMemStall;
	}

	public Core getContainingCore() {
		return containingCore;
	}

	public BranchPredictor getBranchPredictor() {
		return branchPredictor;
	}
	
	public abstract PowerConfigNew calculateAndPrintPower(FileWriter outputFileWriter, String componentName) throws IOException;
	
}
