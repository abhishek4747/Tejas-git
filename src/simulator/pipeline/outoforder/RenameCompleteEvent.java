package pipeline.outoforder;

import config.SimulationConfig;
import generic.GlobalClock;
import generic.Instruction;
import generic.NewEvent;
import generic.Core;
import generic.NewEventQueue;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;
import generic.RequestType;
import generic.Time_t;

/**
 * handling of the event :
 * make IW entry
 * find out if operands are available
 * attempt to issue the instruction		
 */
public class RenameCompleteEvent extends NewEvent {
	
	ReorderBufferEntry reorderBufferEntry;
	int threadID;
	Core core;
	NewEventQueue eventQueue;
	
	int ctr = 0;
	
	ExecutionEngine execEngine;
	Instruction instruction;
	OperationType opType;
	
	public RenameCompleteEvent(Core core, ReorderBufferEntry reorderBufferEntry,
			long eventTime)
	{
		super(new Time_t(eventTime),
				null,
				null,
				core.getExecEngine().getReorderBuffer().indexOf(reorderBufferEntry),
				RequestType.RENAME_COMPLETE);
		this.reorderBufferEntry = reorderBufferEntry;
		this.threadID = reorderBufferEntry.getThreadID();
		this.core = core;
		
		execEngine = core.getExecEngine();
		instruction = reorderBufferEntry.getInstruction();
		opType = instruction.getOperationType();
	}

	@Override
	public void handleEvent(NewEventQueue newEventQueue) {
		

		if(SimulationConfig.debugMode)
		{
			System.out.println("rename : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + reorderBufferEntry.getInstruction());
		}
		
		this.eventQueue = newEventQueue;
		
		//add to Instruction Window
		IWEntry newIWEntry = execEngine.getInstructionWindow().addToWindow(
				reorderBufferEntry);
		
		if(newIWEntry == null)
		{
			handleIWFull();
			return;
		}
		
		long operandReadyTime = GlobalClock.getCurrentTime();
		long bothOperandsReadyTime = GlobalClock.getCurrentTime();
		
		//operand 1
		operandReadyTime = checkOperand1Availability();
		bothOperandsReadyTime = operandReadyTime;
		
		//operand 2
		if(reorderBufferEntry.isOperand2Available() == false)
		{
			operandReadyTime = checkOperand2Availability();
		}
		if(operandReadyTime > bothOperandsReadyTime)
		{
			bothOperandsReadyTime = operandReadyTime;
		}
		
		//estimate and set, time of completion
		setTimeOfCompletion(bothOperandsReadyTime);
		
		//set rename done flag
		reorderBufferEntry.setRenameDone(true);
		
		//attempt to issue the instruction
		reorderBufferEntry.getAssociatedIWEntry().issueInstruction();

	}

	void handleIWFull()
	{
		System.out.println("IW full");
		this.getEventTime().setTime(GlobalClock.getCurrentTime() + core.getStepSize());
		this.eventQueue.addEvent(this);
	}
	
	long checkOperand1Availability()
	{
		Operand tempOpnd = instruction.getSourceOperand1();
		if(tempOpnd == null)
		{
			reorderBufferEntry.setOperand1Available(true);
			return GlobalClock.getCurrentTime();
		}
		
		OperandType tempOpndType = tempOpnd.getOperandType();
		
		if(tempOpndType == OperandType.immediate ||
				tempOpndType == OperandType.inValid)
		{
			reorderBufferEntry.setOperand1Available(true);
			return GlobalClock.getCurrentTime();
		}
		
		int tempOpndPhyReg1 = reorderBufferEntry.getOperand1PhyReg1();
		int tempOpndPhyReg2 = reorderBufferEntry.getOperand1PhyReg2();
		boolean[] opndAvailable = OperandAvailabilityChecker.isAvailable(reorderBufferEntry, tempOpnd, tempOpndPhyReg1, tempOpndPhyReg2, core);
		
		if(tempOpndType == OperandType.integerRegister ||
				tempOpndType == OperandType.floatRegister ||
				tempOpndType == OperandType.machineSpecificRegister)
		{
		
			if(tempOpndType == OperandType.machineSpecificRegister)
			{
				RegisterFile tempRF = execEngine.getMachineSpecificRegisterFile(threadID);
				if(opndAvailable[0] == true)
				{
					reorderBufferEntry.setOperand1Available(true);
					if(opType == OperationType.xchg)
					{
						tempRF.setValueValid(false, tempOpndPhyReg1);
						tempRF.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg1);
						//2nd operand may be the same register as 1st operand
						if(tempOpndType == instruction.getSourceOperand2().getOperandType()
								&& tempOpndPhyReg1 == reorderBufferEntry.getOperand2PhyReg1())
						{
							reorderBufferEntry.setOperand2Available(true);							
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
					tempRN = execEngine.getIntegerRenameTable();
				}
				else
				{
					tempRN = execEngine.getFloatingPointRenameTable();
				}
				
				if(opndAvailable[0] == true)
				{
					reorderBufferEntry.setOperand1Available(true);
					if(opType == OperationType.xchg)
					{
						tempRN.setValueValid(false, tempOpndPhyReg1);
						tempRN.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg1);
						//2nd operand may be the same register as 1st operand
						if(tempOpndType == reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType()
								&& tempOpndPhyReg1 == reorderBufferEntry.getOperand2PhyReg1())
						{
							reorderBufferEntry.setOperand2Available(true);							
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
				reorderBufferEntry.setOperand11Available(true);
				reorderBufferEntry.setOperand12Available(true);
				reorderBufferEntry.setOperand1Available(true);
				return GlobalClock.getCurrentTime();
			}
			else
			{
				reorderBufferEntry.setOperand1Available(false);
				
				long readyAtTime1 = GlobalClock.getCurrentTime(), readyAtTime2 = GlobalClock.getCurrentTime();
				if(opndAvailable[0] == true)
				{
					reorderBufferEntry.setOperand11Available(true);
				}
				else
				{
					reorderBufferEntry.setOperand11Available(false);
					tempOpndType = tempOpnd.getMemoryLocationFirstOperand().getOperandType();
					if(tempOpndType == OperandType.machineSpecificRegister)
					{
						readyAtTime1 = execEngine.getMachineSpecificRegisterFile(threadID).getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
					}
					else if(tempOpndType == OperandType.integerRegister)
					{
						readyAtTime1 =execEngine.getIntegerRenameTable().getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
					}
					else if(tempOpndType == OperandType.floatRegister)
					{
						readyAtTime1 = execEngine.getFloatingPointRenameTable().getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
					}
				}
				
				if(opndAvailable[1] == true)
				{
					reorderBufferEntry.setOperand12Available(true);
				}
				else
				{
					reorderBufferEntry.setOperand12Available(false);
					tempOpndType = tempOpnd.getMemoryLocationSecondOperand().getOperandType();
					if(tempOpndType == OperandType.machineSpecificRegister)
					{
						readyAtTime2 = execEngine.getMachineSpecificRegisterFile(threadID).getProducerROBEntry(tempOpndPhyReg2).getReadyAtTime();
					}
					else if(tempOpndType == OperandType.integerRegister)
					{
						readyAtTime2 = execEngine.getIntegerRenameTable().getProducerROBEntry(tempOpndPhyReg2).getReadyAtTime();
					}
					else if(tempOpndType == OperandType.floatRegister)
					{
						readyAtTime2 = execEngine.getFloatingPointRenameTable().getProducerROBEntry(tempOpndPhyReg2).getReadyAtTime();
					}
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
	
	long checkOperand2Availability()
	{
		Operand tempOpnd = reorderBufferEntry.getInstruction().getSourceOperand2();
		if(tempOpnd == null)
		{
			reorderBufferEntry.setOperand2Available(true);
			return GlobalClock.getCurrentTime();
		}
		
		OperandType tempOpndType = tempOpnd.getOperandType();
		
		if(tempOpndType == OperandType.immediate ||
				tempOpndType == OperandType.inValid)
		{
			reorderBufferEntry.setOperand2Available(true);
			return GlobalClock.getCurrentTime();
		}
		
		int tempOpndPhyReg1 = reorderBufferEntry.getOperand2PhyReg1();
		int tempOpndPhyReg2 = reorderBufferEntry.getOperand2PhyReg2();
		boolean[] opndAvailable = OperandAvailabilityChecker.isAvailable(reorderBufferEntry, tempOpnd, tempOpndPhyReg1, tempOpndPhyReg2, core);
		
		if(tempOpndType == OperandType.integerRegister ||
				tempOpndType == OperandType.floatRegister ||
				tempOpndType == OperandType.machineSpecificRegister)
		{
		
			if(tempOpndType == OperandType.machineSpecificRegister)
			{
				RegisterFile tempRF = execEngine.getMachineSpecificRegisterFile(threadID);
				if(opndAvailable[0] == true)
				{
					reorderBufferEntry.setOperand2Available(true);
					if(opType == OperationType.xchg)
							//|| reorderBufferEntry.getInstruction().getDestinationOperand() != null &&
							//reorderBufferEntry.getInstruction().getDestinationOperand().getValue() == tempOpndPhyReg1)
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
					tempRN = execEngine.getIntegerRenameTable();
				}
				else
				{
					tempRN = execEngine.getFloatingPointRenameTable();
				}
				
				if(opndAvailable[0] == true)
				{
					reorderBufferEntry.setOperand2Available(true);
					if(opType == OperationType.xchg)
					{
						tempRN.setValueValid(false, tempOpndPhyReg1);
						tempRN.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg1);
					}
					return GlobalClock.getCurrentTime();
				}
				else
				{
					//assign operandReadyTime
					
					if(tempRN.getProducerROBEntry(tempOpndPhyReg1) == null)
					{
						System.out.println(reorderBufferEntry);
						System.out.println(tempOpndPhyReg1);
					}
					
					return tempRN.getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
				}
			}
		}
		
		else if(tempOpndType == OperandType.memory)
		{
			if(opndAvailable[0] == true && opndAvailable[1] == true)
			{
				reorderBufferEntry.setOperand21Available(true);
				reorderBufferEntry.setOperand22Available(true);
				reorderBufferEntry.setOperand2Available(true);
				return GlobalClock.getCurrentTime();
			}
			else
			{
				reorderBufferEntry.setOperand2Available(false);
				
				long readyAtTime1 = GlobalClock.getCurrentTime(), readyAtTime2 = GlobalClock.getCurrentTime();
				if(opndAvailable[0] == true)
				{
					reorderBufferEntry.setOperand21Available(true);
				}
				else
				{
					reorderBufferEntry.setOperand21Available(false);
					tempOpndType = tempOpnd.getMemoryLocationFirstOperand().getOperandType();
					if(tempOpndType == OperandType.machineSpecificRegister)
					{
						readyAtTime1 = execEngine.getMachineSpecificRegisterFile(threadID).getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
					}
					else if(tempOpndType == OperandType.integerRegister)
					{
						readyAtTime1 = execEngine.getIntegerRenameTable().getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
					}
					else if(tempOpndType == OperandType.floatRegister)
					{
						readyAtTime1 = execEngine.getFloatingPointRenameTable().getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
					}
				}
				
				if(opndAvailable[1] == false)
				{
					reorderBufferEntry.setOperand22Available(true);
				}
				else
				{
					reorderBufferEntry.setOperand22Available(false);
					tempOpndType = tempOpnd.getMemoryLocationSecondOperand().getOperandType();
					if(tempOpndType == OperandType.machineSpecificRegister)
					{
						readyAtTime2 = execEngine.getMachineSpecificRegisterFile(threadID).getProducerROBEntry(tempOpndPhyReg2).getReadyAtTime();
					}
					else if(tempOpndType == OperandType.integerRegister)
					{
						readyAtTime2 = execEngine.getIntegerRenameTable().getProducerROBEntry(tempOpndPhyReg2).getReadyAtTime();
					}
					else if(tempOpndType == OperandType.floatRegister)
					{
						readyAtTime2 = execEngine.getFloatingPointRenameTable().getProducerROBEntry(tempOpndPhyReg2).getReadyAtTime();
					}
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
				bothOperandsReadyTime + opTime*core.getStepSize());
	}
	
}