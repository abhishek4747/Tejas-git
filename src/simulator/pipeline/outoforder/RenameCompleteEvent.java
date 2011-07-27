package pipeline.outoforder;

import generic.Event;
import generic.Core;
import generic.OperandType;
import generic.OperationType;

/**
 * handling of the event :
 * make IW entry
 * find out if operands are available
 * attempt to issue the instruction		
 */
public class RenameCompleteEvent extends Event {
	
	ReorderBufferEntry reorderBufferEntry;
	Core core;
	
	public RenameCompleteEvent(Core core, ReorderBufferEntry reorderBufferEntry,
			long eventTime)
	{
		super(eventTime,
				3,
				core.getExecEngine().getReorderBuffer()
					.getROB().indexOf(reorderBufferEntry) );
		this.reorderBufferEntry = reorderBufferEntry;
		this.core = core;
	}

	@Override
	public void handleEvent() {
		
		//add to Instruction Window
		IWEntry newIWEntry = core.getExecEngine().getInstructionWindow().addToWindow(
				reorderBufferEntry);
		
		if(newIWEntry == null)
		{
			handleIWFull();			
			return;
		}
		
		long operandReadyTime = core.getClock();
		long bothOperandsReadyTime = core.getClock();
		
		//operand 1
		operandReadyTime = checkOperand1Availability(newIWEntry);
		bothOperandsReadyTime = operandReadyTime;
		
		if(newIWEntry.isOperand1Available && (
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.load ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.store))
		{
			//TODO signal LSQ
		}		
		
		//operand 2
		operandReadyTime = checkOperand2Availability(newIWEntry);
		if(operandReadyTime > bothOperandsReadyTime)
		{
			bothOperandsReadyTime = operandReadyTime;
		}
		
		if(newIWEntry.isOperand2Available &&
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.store)
		{
			//TODO signal LSQ
		}
		
		//estimate and set, time of completion
		setTimeOfCompletion(bothOperandsReadyTime);
		
		//attempt to issue the instruction
		reorderBufferEntry.getAssociatedIWEntry().issueInstruction();

	}

	void handleIWFull()
	{
		System.out.println("IW full");
		
		//schedule new rename complete event
		core.getEventQueue().addEvent(
				new RenameCompleteEvent(
					core,
					reorderBufferEntry,
					core.getClock() + 1
				));
	}
	
	long checkOperand1Availability(IWEntry newIWEntry)
	{
		OperandType tempOpndType = reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType();
		
		if(tempOpndType != OperandType.integerRegister &&
				tempOpndType != OperandType.floatRegister &&
				tempOpndType != OperandType.machineSpecificRegister)
		{
			newIWEntry.setOperand1Available(true);
			return core.getClock();
		}
		else
		{
			int tempOpndPhyReg = reorderBufferEntry.getOperand1PhyReg();
			
			if(tempOpndType == OperandType.machineSpecificRegister)
			{
				RegisterFile tempRF = core.getExecEngine().getMachineSpecificRegisterFile();
				if(tempRF.getValueValid(tempOpndPhyReg) == true)
				{
					newIWEntry.setOperand1Available(true);
					if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg ||
							reorderBufferEntry.getInstruction().getDestinationOperand().getValue() == tempOpndPhyReg)
					{
						tempRF.setValueValid(false, tempOpndPhyReg);
						tempRF.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg);
						//2nd operand may be the same register as 1st operand
						if(tempOpndType == reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType()
								&& tempOpndPhyReg == reorderBufferEntry.getOperand2PhyReg())
						{
							newIWEntry.setOperand2Available(true);							
						}
					}
					return core.getClock();
				}
				else
				{
					//assign operandReadyTime
					return tempRF.getProducerROBEntry(tempOpndPhyReg).getReadyAtTime();
				}
			}
			else
			{
				RenameTable tempRN;
				if(tempOpndType	== OperandType.integerRegister)
				{
					tempRN = core.getExecEngine().getIntegerRenameTable();
				}
				else
				{
					tempRN = core.getExecEngine().getFloatingPointRenameTable();
				}
				
				if(tempRN.getValueValid(tempOpndPhyReg) == true)
				{
					newIWEntry.setOperand1Available(true);
					if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg)
					{
						tempRN.setValueValid(false, tempOpndPhyReg);
						tempRN.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg);
						//2nd operand may be the same register as 1st operand
						if(tempOpndType == reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType()
								&& tempOpndPhyReg == reorderBufferEntry.getOperand2PhyReg())
						{
							newIWEntry.setOperand2Available(true);							
						}
					}
					return core.getClock();
				}
				else
				{
					//assign operandReadyTime
					return tempRN.getProducerROBEntry(tempOpndPhyReg).getReadyAtTime();
				}
			}
		}
	}
	
	long checkOperand2Availability(IWEntry newIWEntry)
	{
		OperandType tempOpndType = reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType();
		
		if(tempOpndType != OperandType.integerRegister &&
				tempOpndType != OperandType.floatRegister &&
				tempOpndType != OperandType.machineSpecificRegister)
		{
			newIWEntry.setOperand2Available(true);
			return core.getClock();
		}
		else
		{
			int tempOpndPhyReg = reorderBufferEntry.getOperand2PhyReg();
			
			if(tempOpndType == OperandType.machineSpecificRegister)
			{
				RegisterFile tempRF = core.getExecEngine().getMachineSpecificRegisterFile();
				if(tempRF.getValueValid(tempOpndPhyReg) == true)
				{
					newIWEntry.setOperand2Available(true);
					if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg ||
							reorderBufferEntry.getInstruction().getDestinationOperand().getValue() == tempOpndPhyReg)
					{
						tempRF.setValueValid(false, tempOpndPhyReg);
						tempRF.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg);
					}
					return core.getClock();
				}
				else
				{
					//assign operandReadyTime
					return tempRF.getProducerROBEntry(tempOpndPhyReg).getReadyAtTime();
				}
			}
			else
			{
				RenameTable tempRN;
				if(tempOpndType	== OperandType.integerRegister)
				{
					tempRN = core.getExecEngine().getIntegerRenameTable();
				}
				else
				{
					tempRN = core.getExecEngine().getFloatingPointRenameTable();
				}
				
				if(tempRN.getValueValid(tempOpndPhyReg) == true)
				{
					newIWEntry.setOperand2Available(true);
					if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg)
					{
						tempRN.setValueValid(false, tempOpndPhyReg);
						tempRN.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg);
					}
					return core.getClock();
				}
				else
				{
					//assign operandReadyTime
					return tempRN.getProducerROBEntry(tempOpndPhyReg).getReadyAtTime();
				}
			}
		}
	}
	
	void setTimeOfCompletion(long bothOperandsReadyTime)
	{
		int opTime;
		OperationType opType = reorderBufferEntry.getInstruction().getOperationType();
		if(opType == OperationType.mov || opType == OperationType.xchg)
		{
			opTime = 1;
		}
		else	
		{	
			opTime = core.getLatency(
				OpTypeToFUTypeMapping.getFUType(
						reorderBufferEntry.getInstruction().getOperationType()).ordinal());
		}
		
		reorderBufferEntry.setReadyAtTime(
				bothOperandsReadyTime + opTime);
	}
	
}