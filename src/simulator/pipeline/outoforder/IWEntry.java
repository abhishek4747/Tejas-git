package pipeline.outoforder;

import generic.Core;
import generic.Instruction;
import generic.OperationType;
import generic.ExecutionCompleteEvent;

/**
 * represents an entry in the instruction window
 */
public class IWEntry {
	
	Core core;
	Instruction instruction;
	boolean isOperand1Available;
	boolean isOperand2Available;
	ReorderBufferEntry associatedROBEntry;
	
	public IWEntry(Core core, Instruction instruction, ReorderBufferEntry ROBEntry)
	{
		this.core = core;
		this.instruction = instruction;
		associatedROBEntry = ROBEntry;
		isOperand1Available = false;
		isOperand2Available = false;
	}
	
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
			return;
		}
		
		if(isOperand1Available && isOperand2Available)
		{
			
			if(instruction.getOperationType() == OperationType.mov ||
					instruction.getOperationType() == OperationType.xchg)
			{
				//no FU required
				issueMovXchg();
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
		associatedROBEntry.setReadyAtTime(core.getClock() + 1);
		
		core.getEventQueue().addEvent(
					new ExecutionCompleteEvent(
							this.getAssociatedROBEntry(),
							associatedROBEntry.getFUInstance(),
							core,
							core.getClock() + 1 ) );
		
		//remove IW entry
		core.getExecEngine().getInstructionWindow().removeFromWindow(this);
	}
	
	void issueOthers()
	{
		long FURequest = 0;	//will be <= 0 if an FU was obtained
		//will be > 0 otherwise, indicating how long before
		//	an FU of the type will be available

		FURequest = core.getExecEngine().getFunctionalUnitSet().requestFU(
		OpTypeToFUTypeMapping.getFUType(instruction.getOperationType()),
		core.getClock());
		
		if(FURequest <= 0)
		{
			associatedROBEntry.setIssued(true);
			associatedROBEntry.setFUInstance((int) ((-1) * FURequest));
			associatedROBEntry.setReadyAtTime(core.getClock() + core.getLatency(
			OpTypeToFUTypeMapping.getFUType(instruction.getOperationType()).ordinal()));
			
			core.getEventQueue().addEvent(
				new ExecutionCompleteEvent(
						this.getAssociatedROBEntry(),
						associatedROBEntry.getFUInstance(),
						core,
						core.getClock() + core.getLatency(
								OpTypeToFUTypeMapping.getFUType(instruction.getOperationType()).ordinal())
						) );
			
			//remove IW entry
			core.getExecEngine().getInstructionWindow().removeFromWindow(this);
		}
		
		else
		{
			core.getEventQueue().addEvent(
			new FunctionalUnitAvailableEvent(
					this.core,
					this.getAssociatedROBEntry(),
					FURequest ) );
			
			associatedROBEntry.setReadyAtTime(FURequest + core.getLatency(
			OpTypeToFUTypeMapping.getFUType(instruction.getOperationType()).ordinal()));
		}
	}

	
	public ReorderBufferEntry getAssociatedROBEntry() {
		return associatedROBEntry;
	}
	public void setAssociatedROBEntry(ReorderBufferEntry associatedROBEntry) {
		this.associatedROBEntry = associatedROBEntry;
	}
	public boolean isOperand1Available() {
		return isOperand1Available;
	}
	public void setOperand1Available(boolean isOperand1Available) {
		this.isOperand1Available = isOperand1Available;
	}
	public boolean isOperand2Available() {
		return isOperand2Available;
	}
	public void setOperand2Available(boolean isOperand2Available) {
		this.isOperand2Available = isOperand2Available;
	}

}