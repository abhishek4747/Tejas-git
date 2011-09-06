package pipeline.outoforder;

import memorysystem.LSQAddressReadyEvent;
import generic.Core;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperationType;
import generic.ExecutionCompleteEvent;
import generic.PortRequestEvent;
import generic.RequestType;

/**
 * represents an entry in the instruction window
 */
public class IWEntry {
	
	Core core;
	Instruction instruction;
	ReorderBufferEntry associatedROBEntry;
	
	boolean isValid;
	
	int pos;

	public IWEntry(Core core, int pos)
	{
		this.core = core;
		this.pos = pos;
		isValid = false;
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
			
			if(instruction.getOperationType() == OperationType.mov ||
					instruction.getOperationType() == OperationType.xchg)
			{
				//no FU required
				issueMovXchg();
			}
			
			else if(associatedROBEntry.getInstruction().getOperationType() == OperationType.load ||
					associatedROBEntry.getInstruction().getOperationType() == OperationType.store)
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
		core.getExecEngine().getInstructionWindow().removeFromWindow(this);
		
		core.getEventQueue().addEvent(
				new ExecutionCompleteEvent(
							this.getAssociatedROBEntry(),
							associatedROBEntry.getFUInstance(),
							core,
							GlobalClock.getCurrentTime() + core.getStepSize() ) );
		
		
	}
	
	void issueLoadStore()
	{
		associatedROBEntry.setIssued(true);
		if(associatedROBEntry.getInstruction().getOperationType() == OperationType.store)
		{
			associatedROBEntry.setExecuted(true);
			associatedROBEntry.setWriteBackDone1(true);
			associatedROBEntry.setWriteBackDone2(true);
		}
		
		core.getExecEngine().getInstructionWindow().removeFromWindow(this);
		//TODO add event to indicate address ready
		core.getEventQueue().addEvent(new PortRequestEvent(0, //tieBreaker, 
				1, //noOfSlots,
				new LSQAddressReadyEvent(core.getExecEngine().coreMemSys.getLsqueue().getLatencyDelay(), 
													null, //Requesting Element
													core.getExecEngine().coreMemSys.getLsqueue(), 
													0, //tieBreaker,
													RequestType.TLB_ADDRESS_READY,
													associatedROBEntry.getLsqEntry())));
	}
	
	void issueOthers()
	{
		long FURequest = 0;	//will be <= 0 if an FU was obtained
		//will be > 0 otherwise, indicating how long before
		//	an FU of the type will be available

		FURequest = core.getExecEngine().getFunctionalUnitSet().requestFU(
			OpTypeToFUTypeMapping.getFUType(instruction.getOperationType()),
			GlobalClock.getCurrentTime(),
			core.getStepSize() );
		
		if(FURequest <= 0)
		{
			associatedROBEntry.setIssued(true);
			associatedROBEntry.setFUInstance((int) ((-1) * FURequest));
			associatedROBEntry.setReadyAtTime(GlobalClock.getCurrentTime() + core.getLatency(
					OpTypeToFUTypeMapping.getFUType(instruction.getOperationType()).ordinal())*core.getStepSize());
			
			//remove IW entry
			core.getExecEngine().getInstructionWindow().removeFromWindow(this);
			
			core.getEventQueue().addEvent(
				new ExecutionCompleteEvent(
						this.getAssociatedROBEntry(),
						associatedROBEntry.getFUInstance(),
						core,
						GlobalClock.getCurrentTime() + core.getLatency(
								OpTypeToFUTypeMapping.getFUType(instruction.getOperationType()).ordinal()) * core.getStepSize()
						) );
		}
		
		else
		{
			
			associatedROBEntry.setReadyAtTime(FURequest + core.getLatency(
			OpTypeToFUTypeMapping.getFUType(instruction.getOperationType()).ordinal()) * core.getStepSize());
			
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
	}

}