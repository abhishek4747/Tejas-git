package pipeline.outoforder;

import generic.GlobalClock;
import generic.NewEvent;
import generic.Core;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;
import generic.RequestType;

/**
 * handling of the event :
 * make IW entry
 * find out if operands are available
 * attempt to issue the instruction		
 */
public class RenameCompleteEvent extends NewEvent {
	
	ReorderBufferEntry reorderBufferEntry;
	Core core;
	
	public RenameCompleteEvent(Core core, ReorderBufferEntry reorderBufferEntry,
			long eventTime)
	{
		super(eventTime,
				null,
				null,
				core.getExecEngine().getReorderBuffer()
					.getROB().indexOf(reorderBufferEntry),
				RequestType.RENAME_COMPLETE);
		this.reorderBufferEntry = reorderBufferEntry;
		this.core = core;
	}

	@Override
	public NewEvent handleEvent() {
		
		//add to Instruction Window
		IWEntry newIWEntry = core.getExecEngine().getInstructionWindow().addToWindow(
				reorderBufferEntry);
		
		if(newIWEntry == null)
		{
			return handleIWFull();
		}
		
		long operandReadyTime = GlobalClock.getCurrentTime();
		long bothOperandsReadyTime = GlobalClock.getCurrentTime();
		
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
		return reorderBufferEntry.getAssociatedIWEntry().issueInstruction();

	}

	NewEvent handleIWFull()
	{
		System.out.println("IW full");
		
		//schedule new rename complete event
		return (
				new RenameCompleteEvent(
					core,
					reorderBufferEntry,
					GlobalClock.getCurrentTime() + 1
				));
	}
	
	long checkOperand1Availability(IWEntry newIWEntry)
	{
		Operand tempOpnd = reorderBufferEntry.getInstruction().getSourceOperand1();
		if(tempOpnd == null)
		{
			newIWEntry.setOperand1Available(true);
			return GlobalClock.getCurrentTime();
		}
		
		OperandType tempOpndType = tempOpnd.getOperandType();
		
		if(tempOpndType == OperandType.immediate ||
				tempOpndType == OperandType.inValid)
		{
			newIWEntry.setOperand1Available(true);
			return GlobalClock.getCurrentTime();
		}
		
		int tempOpndPhyReg1 = reorderBufferEntry.getOperand1PhyReg1();
		int tempOpndPhyReg2 = reorderBufferEntry.getOperand1PhyReg2();
		boolean[] opndAvailable = OperandAvailabilityChecker.isAvailable(tempOpnd, tempOpndPhyReg1, tempOpndPhyReg2, core);
		
		if(tempOpndType == OperandType.integerRegister ||
				tempOpndType == OperandType.floatRegister ||
				tempOpndType == OperandType.machineSpecificRegister)
		{
		
			if(tempOpndType == OperandType.machineSpecificRegister)
			{
				RegisterFile tempRF = core.getExecEngine().getMachineSpecificRegisterFile();
				if(opndAvailable[0] == true)
				{
					newIWEntry.setOperand1Available(true);
					if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg ||
							reorderBufferEntry.getInstruction().getDestinationOperand().getValue() == tempOpndPhyReg1)
					{
						tempRF.setValueValid(false, tempOpndPhyReg1);
						tempRF.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg1);
						//2nd operand may be the same register as 1st operand
						if(tempOpndType == reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType()
								&& tempOpndPhyReg1 == reorderBufferEntry.getOperand2PhyReg1())
						{
							newIWEntry.setOperand2Available(true);							
						}
					}
					return GlobalClock.getCurrentTime();
				}
				else
				{
					//assign operandReadyTime
					return tempRF.getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
				}
			}
			else if(tempOpndType == OperandType.integerRegister ||
					tempOpndType == OperandType.floatRegister)
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
				
				if(opndAvailable[0] == true)
				{
					newIWEntry.setOperand1Available(true);
					if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg)
					{
						tempRN.setValueValid(false, tempOpndPhyReg1);
						tempRN.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg1);
						//2nd operand may be the same register as 1st operand
						if(tempOpndType == reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType()
								&& tempOpndPhyReg1 == reorderBufferEntry.getOperand2PhyReg1())
						{
							newIWEntry.setOperand2Available(true);							
						}
					}
					return GlobalClock.getCurrentTime();
				}
				else
				{
					//assign operandReadyTime
					return tempRN.getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
				}
			}
		}
		
		else if(tempOpndType == OperandType.memory)
		{
			if(opndAvailable[0] == true && opndAvailable[1] == true)
			{
				newIWEntry.setOperand11Available(true);
				newIWEntry.setOperand12Available(true);
				newIWEntry.setOperand1Available(true);
				return GlobalClock.getCurrentTime();
			}
			else
			{
				newIWEntry.setOperand1Available(false);
				
				long readyAtTime1 = GlobalClock.getCurrentTime(), readyAtTime2 = GlobalClock.getCurrentTime();
				if(opndAvailable[0] == false)
				{
					newIWEntry.setOperand11Available(false);
					if(tempOpnd.getMemoryLocationFirstOperand().getOperandType() == OperandType.machineSpecificRegister)
					{
						readyAtTime1 = core.getExecEngine().getMachineSpecificRegisterFile().getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
					}
					else if(tempOpnd.getMemoryLocationFirstOperand().getOperandType() == OperandType.integerRegister)
					{
						readyAtTime1 = core.getExecEngine().getIntegerRenameTable().getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
					}
					else if(tempOpnd.getMemoryLocationFirstOperand().getOperandType() == OperandType.floatRegister)
					{
						readyAtTime1 = core.getExecEngine().getFloatingPointRenameTable().getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
					}
				}
				else
				{
					newIWEntry.setOperand11Available(true);
				}
				
				if(opndAvailable[1] == false)
				{
					newIWEntry.setOperand12Available(false);
					if(tempOpnd.getMemoryLocationSecondOperand().getOperandType() == OperandType.machineSpecificRegister)
					{
						readyAtTime2 = core.getExecEngine().getMachineSpecificRegisterFile().getProducerROBEntry(tempOpndPhyReg2).getReadyAtTime();
					}
					else if(tempOpnd.getMemoryLocationSecondOperand().getOperandType() == OperandType.integerRegister)
					{
						readyAtTime2 = core.getExecEngine().getIntegerRenameTable().getProducerROBEntry(tempOpndPhyReg2).getReadyAtTime();
					}
					else if(tempOpnd.getMemoryLocationSecondOperand().getOperandType() == OperandType.floatRegister)
					{
						readyAtTime2 = core.getExecEngine().getFloatingPointRenameTable().getProducerROBEntry(tempOpndPhyReg2).getReadyAtTime();
					}
				}
				else
				{
					newIWEntry.setOperand12Available(true);
				}
				
				if(readyAtTime1 > readyAtTime2)
				{
					return readyAtTime1;
				}
				else
				{
					return readyAtTime2;
				}
			}
		}
		
		return GlobalClock.getCurrentTime();
	}
	
	long checkOperand2Availability(IWEntry newIWEntry)
	{
		Operand tempOpnd = reorderBufferEntry.getInstruction().getSourceOperand2();
		if(tempOpnd == null)
		{
			newIWEntry.setOperand2Available(true);
			return GlobalClock.getCurrentTime();
		}
		
		OperandType tempOpndType = tempOpnd.getOperandType();
		
		if(tempOpndType == OperandType.immediate ||
				tempOpndType == OperandType.inValid)
		{
			newIWEntry.setOperand2Available(true);
			return GlobalClock.getCurrentTime();
		}
		
		int tempOpndPhyReg1 = reorderBufferEntry.getOperand2PhyReg1();
		int tempOpndPhyReg2 = reorderBufferEntry.getOperand2PhyReg2();
		boolean[] opndAvailable = OperandAvailabilityChecker.isAvailable(tempOpnd, tempOpndPhyReg1, tempOpndPhyReg2, core);
		
		if(tempOpndType == OperandType.integerRegister ||
				tempOpndType == OperandType.floatRegister ||
				tempOpndType == OperandType.machineSpecificRegister)
		{
		
			if(tempOpndType == OperandType.machineSpecificRegister)
			{
				RegisterFile tempRF = core.getExecEngine().getMachineSpecificRegisterFile();
				if(opndAvailable[0] == true)
				{
					newIWEntry.setOperand2Available(true);
					if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg ||
							reorderBufferEntry.getInstruction().getDestinationOperand().getValue() == tempOpndPhyReg1)
					{
						tempRF.setValueValid(false, tempOpndPhyReg1);
						tempRF.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg1);
					}
					return GlobalClock.getCurrentTime();
				}
				else
				{
					//assign operandReadyTime
					return tempRF.getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
				}
			}
			else if(tempOpndType == OperandType.integerRegister ||
					tempOpndType == OperandType.floatRegister)
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
				
				if(opndAvailable[0] == true)
				{
					newIWEntry.setOperand2Available(true);
					if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg)
					{
						tempRN.setValueValid(false, tempOpndPhyReg1);
						tempRN.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg1);
					}
					return GlobalClock.getCurrentTime();
				}
				else
				{
					//assign operandReadyTime
					return tempRN.getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
				}
			}
		}
		
		else if(tempOpndType == OperandType.memory)
		{
			if(opndAvailable[0] == true && opndAvailable[1] == true)
			{
				newIWEntry.setOperand21Available(true);
				newIWEntry.setOperand22Available(true);
				newIWEntry.setOperand2Available(true);
				return GlobalClock.getCurrentTime();
			}
			else
			{
				newIWEntry.setOperand2Available(false);
				
				long readyAtTime1 = GlobalClock.getCurrentTime(), readyAtTime2 = GlobalClock.getCurrentTime();
				if(opndAvailable[0] == false)
				{
					newIWEntry.setOperand21Available(false);
					if(tempOpnd.getMemoryLocationFirstOperand().getOperandType() == OperandType.machineSpecificRegister)
					{
						readyAtTime1 = core.getExecEngine().getMachineSpecificRegisterFile().getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
					}
					else if(tempOpnd.getMemoryLocationFirstOperand().getOperandType() == OperandType.integerRegister)
					{
						readyAtTime1 = core.getExecEngine().getIntegerRenameTable().getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
					}
					else if(tempOpnd.getMemoryLocationFirstOperand().getOperandType() == OperandType.floatRegister)
					{
						readyAtTime1 = core.getExecEngine().getFloatingPointRenameTable().getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
					}
				}
				else
				{
					newIWEntry.setOperand21Available(true);
				}
				
				if(opndAvailable[1] == false)
				{
					newIWEntry.setOperand22Available(false);
					if(tempOpnd.getMemoryLocationSecondOperand().getOperandType() == OperandType.machineSpecificRegister)
					{
						readyAtTime2 = core.getExecEngine().getMachineSpecificRegisterFile().getProducerROBEntry(tempOpndPhyReg2).getReadyAtTime();
					}
					else if(tempOpnd.getMemoryLocationSecondOperand().getOperandType() == OperandType.integerRegister)
					{
						readyAtTime2 = core.getExecEngine().getIntegerRenameTable().getProducerROBEntry(tempOpndPhyReg2).getReadyAtTime();
					}
					else if(tempOpnd.getMemoryLocationSecondOperand().getOperandType() == OperandType.floatRegister)
					{
						readyAtTime2 = core.getExecEngine().getFloatingPointRenameTable().getProducerROBEntry(tempOpndPhyReg2).getReadyAtTime();
					}
				}
				else
				{
					newIWEntry.setOperand22Available(true);
				}
				
				if(readyAtTime1 > readyAtTime2)
				{
					return readyAtTime1;
				}
				else
				{
					return readyAtTime2;
				}
			}
		}
		
		return GlobalClock.getCurrentTime();
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