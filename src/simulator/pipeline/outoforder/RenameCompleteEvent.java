package pipeline.outoforder;

import generic.GlobalClock;
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
	}

	@Override
	public void handleEvent(NewEventQueue newEventQueue) {
		
		this.eventQueue = newEventQueue;
		
		//add to Instruction Window
		IWEntry newIWEntry = core.getExecEngine().getInstructionWindow().addToWindow(
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
		
		if(reorderBufferEntry.isOperand1Available && (
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.load ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.store))
		{
			//TODO signal LSQ
		}		
		
		//operand 2
		if(reorderBufferEntry.isOperand2Available() == false)
		{
			operandReadyTime = checkOperand2Availability();
		}
		if(operandReadyTime > bothOperandsReadyTime)
		{
			bothOperandsReadyTime = operandReadyTime;
		}
		
		if(reorderBufferEntry.isOperand2Available &&
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.store)
		{
			//TODO signal LSQ
		}
		
		//estimate and set, time of completion
		setTimeOfCompletion(bothOperandsReadyTime);
		
		/*
		//set valueValid of corresponding register file/rename table entry to false
		//set producerROBEntry of corresponding register file/rename table entry to reorderBufferEntry
		if(reorderBufferEntry.getInstruction().getDestinationOperand() != null)
		{
			RegisterFile tempRF = null;
			RenameTable tempRN = null; 
			if(reorderBufferEntry.getInstruction().getDestinationOperand().getOperandType() == OperandType.machineSpecificRegister)
			{
				tempRF = core.getExecEngine().getMachineSpecificRegisterFile();
			}
			else if(reorderBufferEntry.getInstruction().getDestinationOperand().getOperandType() == OperandType.integerRegister)
			{
				tempRN = core.getExecEngine().getIntegerRenameTable();
			}
			else if(reorderBufferEntry.getInstruction().getDestinationOperand().getOperandType() == OperandType.floatRegister)
			{
				tempRN = core.getExecEngine().getFloatingPointRenameTable();
			}
			
			int phyDestReg = (int) reorderBufferEntry.getPhysicalDestinationRegister();
			
			if(tempRF != null)
			{
				tempRF.setValueValid(false, phyDestReg);
				tempRF.setProducerROBEntry(reorderBufferEntry, phyDestReg);
			}
			
			if(tempRN != null)
			{
				tempRN.setValueValid(false, phyDestReg);
				tempRN.setProducerROBEntry(reorderBufferEntry, phyDestReg);
			}
		}
		*/
		
		//set rename done flag
		reorderBufferEntry.setRenameDone(true);
		/*
		if(core.getCoreMode() == CoreMode.CheckerSMT)
		{
			System.out.println("rename complete\n" + reorderBufferEntry);
		}*/
		
		//attempt to issue the instruction
		reorderBufferEntry.getAssociatedIWEntry().issueInstruction();

	}

	void handleIWFull()
	{
		System.out.println("IW full");
		
		//schedule new rename complete event
		/*this.eventQueue.addEvent(
				new RenameCompleteEvent(
					core,
					reorderBufferEntry,
					GlobalClock.getCurrentTime() + 1
				));
		*/
		//this.setEventTime(new Time_t(GlobalClock.getCurrentTime() + 1));
		this.getEventTime().setTime(GlobalClock.getCurrentTime() + core.getStepSize());
		this.eventQueue.addEvent(this);
	}
	
	long checkOperand1Availability()
	{
		Operand tempOpnd = reorderBufferEntry.getInstruction().getSourceOperand1();
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
				RegisterFile tempRF = core.getExecEngine().getMachineSpecificRegisterFile(threadID);
				if(opndAvailable[0] == true)
				{
					reorderBufferEntry.setOperand1Available(true);
					if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg)
							//|| reorderBufferEntry.getInstruction().getDestinationOperand() != null &&
							//reorderBufferEntry.getInstruction().getDestinationOperand().getValue() == tempOpndPhyReg1)
					{
						tempRF.setValueValid(false, tempOpndPhyReg1);
						tempRF.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg1);
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
					reorderBufferEntry.setOperand1Available(true);
					if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg)
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
					if(tempOpnd.getMemoryLocationFirstOperand().getOperandType() == OperandType.machineSpecificRegister)
					{
						readyAtTime1 = core.getExecEngine().getMachineSpecificRegisterFile(threadID).getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
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
				
				if(opndAvailable[1] == true)
				{
					reorderBufferEntry.setOperand12Available(true);
				}
				else
				{
					reorderBufferEntry.setOperand12Available(false);
					if(tempOpnd.getMemoryLocationSecondOperand().getOperandType() == OperandType.machineSpecificRegister)
					{
						readyAtTime2 = core.getExecEngine().getMachineSpecificRegisterFile(threadID).getProducerROBEntry(tempOpndPhyReg2).getReadyAtTime();
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
				RegisterFile tempRF = core.getExecEngine().getMachineSpecificRegisterFile(threadID);
				if(opndAvailable[0] == true)
				{
					reorderBufferEntry.setOperand2Available(true);
					if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg)
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
					tempRN = core.getExecEngine().getIntegerRenameTable();
				}
				else
				{
					tempRN = core.getExecEngine().getFloatingPointRenameTable();
				}
				
				if(opndAvailable[0] == true)
				{
					reorderBufferEntry.setOperand2Available(true);
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
					if(tempOpnd.getMemoryLocationFirstOperand().getOperandType() == OperandType.machineSpecificRegister)
					{
						readyAtTime1 = core.getExecEngine().getMachineSpecificRegisterFile(threadID).getProducerROBEntry(tempOpndPhyReg1).getReadyAtTime();
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
				
				if(opndAvailable[1] == false)
				{
					reorderBufferEntry.setOperand22Available(true);
				}
				else
				{
					reorderBufferEntry.setOperand22Available(false);
					if(tempOpnd.getMemoryLocationSecondOperand().getOperandType() == OperandType.machineSpecificRegister)
					{
						readyAtTime2 = core.getExecEngine().getMachineSpecificRegisterFile(threadID).getProducerROBEntry(tempOpndPhyReg2).getReadyAtTime();
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
				bothOperandsReadyTime + opTime*core.getStepSize());
	}
	
}