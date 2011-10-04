package pipeline.outoforder;

import config.SimulationConfig;
import memorysystem.LSQAddressReadyEvent;
import generic.Core;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperationType;
import generic.ExecutionCompleteEvent;
import generic.RequestType;

/**
 * represents an entry in the instruction window
 */
public class IWEntry {
	
	Core core;
	Instruction instruction;
	ReorderBufferEntry associatedROBEntry;
	
	ExecutionEngine execEngine;
	InstructionWindow instructionWindow;
	OperationType opType;
	
	boolean isValid;
	
	int pos;

	public IWEntry(Core core, int pos,
			ExecutionEngine execEngine, InstructionWindow instructionWindow)
	{
		this.core = core;
		this.pos = pos;
		isValid = false;
		
		this.execEngine = execEngine;
		this.instructionWindow = instructionWindow;
	}
	
	/*
	public IWEntry(Core core, Instruction instruction, ReorderBufferEntry ROBEntry)
	{
		this.core = core;
		this.instruction = instruction;
		associatedROBEntry = ROBEntry;
	}
	*/
	
	//	issueInstruction()
	//----------------------
	//check if
	//1) first operand available
	//2) second operand available
	//if so,
	//		if FU available
	//			issue the instruction
	//				1) set issued = true
	//				2) schedule ExecutionCompleteEvent
	//		else
	//			schedule an FUAvailableEvent
	public void issueInstruction()
	{
		if(associatedROBEntry.getIssued() == true)
		{
			System.out.println("already issued!");
		}
		
		if(associatedROBEntry.isOperand1Available && associatedROBEntry.isOperand2Available)
		{
			
			if(opType == OperationType.mov ||
					opType == OperationType.xchg)
			{
				//no FU required
				issueMovXchg();
			}
			
			else if(opType == OperationType.load ||
					opType == OperationType.store)
			{
				issueLoadStore();
			}
			
			else
			{
				issueOthers();
			}
		}
	}
	
	void issueMovXchg()
	{
		//no FU required
		
		associatedROBEntry.setIssued(true);
		associatedROBEntry.setReadyAtTime(GlobalClock.getCurrentTime() + core.getStepSize());
		
		//remove IW entry
		instructionWindow.removeFromWindow(this);
		
		core.getEventQueue().addEvent(
				new ExecutionCompleteEvent(
							this.getAssociatedROBEntry(),
							associatedROBEntry.getFUInstance(),
							core,
							GlobalClock.getCurrentTime() + core.getStepSize() ) );
		

		if(SimulationConfig.debugMode)
		{
			System.out.println("issue : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + associatedROBEntry.getInstruction());
		}
		
		
	}
	
	void issueLoadStore()
	{
		if(instruction.getSourceOperand2() != null)
		{
			
			associatedROBEntry.setIssued(true);
			if(opType == OperationType.store)
			{
				associatedROBEntry.setExecuted(true);
				associatedROBEntry.setWriteBackDone1(true);
				associatedROBEntry.setWriteBackDone2(true);
			}
			associatedROBEntry.setFUInstance(0);
			associatedROBEntry.setReadyAtTime(GlobalClock.getCurrentTime() + core.getLatency(
					OpTypeToFUTypeMapping.getFUType(opType).ordinal())*core.getStepSize());
			
			//remove IW entry
			instructionWindow.removeFromWindow(this);
			//TODO add event to indicate address ready
			core.getExecEngine().coreMemSys.getLsqueue().getPort().put(new LSQAddressReadyEvent(core.getExecEngine().coreMemSys.getLsqueue().getLatencyDelay(), 
														null, //Requesting Element
														core.getExecEngine().coreMemSys.getLsqueue(), 
														0, //tieBreaker,
														RequestType.TLB_ADDRESS_READY,
														associatedROBEntry.getLsqEntry()));

		}
		else
		{
			associatedROBEntry.setIssued(true);
			if(opType == OperationType.store)
			{
				associatedROBEntry.setExecuted(true);
				associatedROBEntry.setWriteBackDone1(true);
				associatedROBEntry.setWriteBackDone2(true);
			}
			
			instructionWindow.removeFromWindow(this);
			//TODO add event to indicate address ready
			core.getExecEngine().coreMemSys.getLsqueue().getPort().put(new LSQAddressReadyEvent(execEngine.coreMemSys.getLsqueue().getLatencyDelay(), 
														null, //Requesting Element
														core.getExecEngine().coreMemSys.getLsqueue(), 
														0, //tieBreaker,
														RequestType.TLB_ADDRESS_READY,
														associatedROBEntry.getLsqEntry()));
		}

		if(SimulationConfig.debugMode)
		{
			System.out.println("issue : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + associatedROBEntry.getInstruction());
		}
	}
	
	void issueOthers()
	{
		long FURequest = 0;	//will be <= 0 if an FU was obtained
		//will be > 0 otherwise, indicating how long before
		//	an FU of the type will be available

		FURequest = execEngine.getFunctionalUnitSet().requestFU(
			OpTypeToFUTypeMapping.getFUType(opType),
			GlobalClock.getCurrentTime(),
			core.getStepSize() );
		
		if(FURequest <= 0)
		{
			associatedROBEntry.setIssued(true);
			associatedROBEntry.setFUInstance((int) ((-1) * FURequest));
			associatedROBEntry.setReadyAtTime(GlobalClock.getCurrentTime() + core.getLatency(
					OpTypeToFUTypeMapping.getFUType(opType).ordinal())*core.getStepSize());
			
			//remove IW entry
			instructionWindow.removeFromWindow(this);
			
			core.getEventQueue().addEvent(
				new ExecutionCompleteEvent(
						this.getAssociatedROBEntry(),
						associatedROBEntry.getFUInstance(),
						core,
						GlobalClock.getCurrentTime() + core.getLatency(
								OpTypeToFUTypeMapping.getFUType(opType).ordinal()) * core.getStepSize()
						) );

			if(SimulationConfig.debugMode)
			{
				System.out.println("issue : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + associatedROBEntry.getInstruction());
			}
		}
		
		else
		{
			
			associatedROBEntry.setReadyAtTime(FURequest + core.getLatency(
			OpTypeToFUTypeMapping.getFUType(opType).ordinal()) * core.getStepSize());
			
			core.getEventQueue().addEvent(
					new FunctionalUnitAvailableEvent(
						this.core,
						this.getAssociatedROBEntry(),
						FURequest ) );
		}
	}

	
	public ReorderBufferEntry getAssociatedROBEntry() {
		return associatedROBEntry;
	}
	public void setAssociatedROBEntry(ReorderBufferEntry associatedROBEntry) {
		this.associatedROBEntry = associatedROBEntry;
	}
	
	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public Instruction getInstruction() {
		return instruction;
	}

	public void setInstruction(Instruction instruction) {
		this.instruction = instruction;
		opType = instruction.getOperationType();
	}

}