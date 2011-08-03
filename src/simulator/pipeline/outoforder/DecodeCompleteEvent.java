package pipeline.outoforder;

import generic.GlobalClock;
import generic.NewEvent;
import generic.Core;
import generic.Instruction;
import generic.NewEventQueue;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;
import generic.RequestType;
import generic.Time_t;

/**
 * scheduled at the end of decode time
 * ROB entries are made
 * renaming is performed
 * RenameCompleteEvent is scheduled
 * note - decode complete event represents decode-width number of instructions
 */

public class DecodeCompleteEvent extends NewEvent {
	
	Core core;
	int threadID;
	NewEventQueue eventQueue;
	
	public DecodeCompleteEvent(Core core, int threadID, long eventTime)
	{
		super(new Time_t(eventTime),
				null,
				null,
				0,
				RequestType.DECODE_COMPLETE);
		this.core = core;
		this.threadID = threadID;
	}

	@Override
	public void handleEvent(NewEventQueue newEventQueue)
	{
		this.eventQueue = newEventQueue;
		readDecodePipe();
	}
	
	public void readDecodePipe()
	{
		Instruction newInstruction;
		for(int i = 0; i < core.getDecodeWidth(); i++)
		{
			if(core.getExecEngine().getReorderBuffer().isFull() == false
					//&& if head of instructionList is a load/store and LSQ is not full TODO
					&& core.getExecEngine().getInstructionWindow().isFull() == false
					&& core.getExecEngine().isStallDecode1() == false)
			{
				newInstruction = core.getIncomingInstructions(threadID).pollFirst();
				if(newInstruction != null)
				{
					makeROBEntries(newInstruction);
				}
				else
				{
					core.getExecEngine().setDecodePipeEmpty(true);
					break;
				}
			}
		}
	}
	
	public void makeROBEntries(Instruction newInstruction)
	{
		if(newInstruction != null &&
				newInstruction.getOperationType() != OperationType.nop &&
				newInstruction.getOperationType() != OperationType.inValid)
		{			
			ReorderBufferEntry newROBEntry = core.getExecEngine()
				.getReorderBuffer().addInstructionToROB(newInstruction);
			
			//TODO if load or store, make entry in LSQ
			
			//perform renaming			
			processOperand1(newROBEntry);
			processOperand2(newROBEntry);			
			processDestOperand(newROBEntry);
		}
	}

	private void processOperand1(ReorderBufferEntry reorderBufferEntry)
	{
		Operand tempOpnd = reorderBufferEntry.getInstruction().getSourceOperand1();
		if(tempOpnd == null)
		{
			reorderBufferEntry.setOperand1PhyReg1(-1);
			reorderBufferEntry.setOperand1PhyReg2(-1);
			return;
		}
		
		int archReg = (int) tempOpnd.getValue();
		if(tempOpnd.getOperandType() == OperandType.integerRegister)
		{
			reorderBufferEntry.setOperand1PhyReg1(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(archReg));
			reorderBufferEntry.setOperand1PhyReg2(-1);
		}
		else if(tempOpnd.getOperandType() == OperandType.floatRegister)
		{
			reorderBufferEntry.setOperand1PhyReg1(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(archReg));
			reorderBufferEntry.setOperand1PhyReg2(-1);
		}
		else if(tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
		{
			reorderBufferEntry.setOperand1PhyReg1(archReg);
			reorderBufferEntry.setOperand1PhyReg2(-1);
		}
		else if(tempOpnd.getOperandType() == OperandType.memory)
		{
			Operand memLocOpnd1 = tempOpnd.getMemoryLocationFirstOperand();
			Operand memLocOpnd2 = tempOpnd.getMemoryLocationSecondOperand();
			
			//processing memoryLocationFirstOperand
			if(memLocOpnd1 == null)
			{
				reorderBufferEntry.setOperand1PhyReg1(-1);
			}
			else
			{
				archReg = (int)memLocOpnd1.getValue();
				if(memLocOpnd1.getOperandType() == OperandType.integerRegister)
				{
					reorderBufferEntry.setOperand1PhyReg1(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(archReg));
				}
				else if(memLocOpnd1.getOperandType() == OperandType.floatRegister)
				{
					reorderBufferEntry.setOperand1PhyReg1(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(archReg));
				}
				else if(memLocOpnd1.getOperandType() == OperandType.machineSpecificRegister)
				{
					reorderBufferEntry.setOperand1PhyReg1(archReg);
				}
				else
				{
					reorderBufferEntry.setOperand1PhyReg1(-1);
				}
			}
			
			//processing memoryLocationSecondOperand
			if(memLocOpnd2 == null)
			{
				reorderBufferEntry.setOperand1PhyReg2(-1);
			}
			else
			{
				archReg = (int)memLocOpnd2.getValue();
				if(memLocOpnd2.getOperandType() == OperandType.integerRegister)
				{
					reorderBufferEntry.setOperand1PhyReg2(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(archReg));
				}
				else if(memLocOpnd2.getOperandType() == OperandType.floatRegister)
				{
					reorderBufferEntry.setOperand1PhyReg2(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(archReg));
				}
				else if(memLocOpnd1.getOperandType() == OperandType.machineSpecificRegister)
				{
					reorderBufferEntry.setOperand1PhyReg2(archReg);
				}
				else
				{
					reorderBufferEntry.setOperand1PhyReg2(-1);
				}
			}
		}
		else
		{
			reorderBufferEntry.setOperand1PhyReg1(-1);
			reorderBufferEntry.setOperand1PhyReg2(-1);
		}
	}
	
	private void processOperand2(ReorderBufferEntry reorderBufferEntry)
	{
		Operand tempOpnd = reorderBufferEntry.getInstruction().getSourceOperand2();
		if(tempOpnd == null)
		{
			reorderBufferEntry.setOperand2PhyReg1(-1);
			reorderBufferEntry.setOperand2PhyReg2(-1);
			return;
		}
		
		int archReg = (int) tempOpnd.getValue();
		if(tempOpnd.getOperandType() == OperandType.integerRegister)
		{
			reorderBufferEntry.setOperand2PhyReg1(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(archReg));
			reorderBufferEntry.setOperand2PhyReg2(-1);
		}
		else if(tempOpnd.getOperandType() == OperandType.floatRegister)
		{
			reorderBufferEntry.setOperand2PhyReg1(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(archReg));
			reorderBufferEntry.setOperand2PhyReg2(-1);
		}
		else if(tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
		{
			reorderBufferEntry.setOperand2PhyReg1(archReg);
			reorderBufferEntry.setOperand2PhyReg2(-1);
		}
		else if(tempOpnd.getOperandType() == OperandType.memory)
		{
			Operand memLocOpnd1 = tempOpnd.getMemoryLocationFirstOperand();
			Operand memLocOpnd2 = tempOpnd.getMemoryLocationSecondOperand();
			
			//processing memoryLocationFirstOperand
			if(memLocOpnd1 == null)
			{
				reorderBufferEntry.setOperand2PhyReg1(-1);
			}
			else
			{
				archReg = (int)memLocOpnd1.getValue();
				if(memLocOpnd1.getOperandType() == OperandType.integerRegister)
				{
					reorderBufferEntry.setOperand2PhyReg1(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(archReg));
				}
				else if(memLocOpnd1.getOperandType() == OperandType.floatRegister)
				{
					reorderBufferEntry.setOperand2PhyReg1(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(archReg));
				}
				else if(memLocOpnd1.getOperandType() == OperandType.machineSpecificRegister)
				{
					reorderBufferEntry.setOperand2PhyReg1(archReg);
				}
				else
				{
					reorderBufferEntry.setOperand2PhyReg1(-1);
				}
			}
			
			//processing memoryLocationSecondOperand
			if(memLocOpnd2 == null)
			{
				reorderBufferEntry.setOperand2PhyReg2(-1);
			}
			else
			{
				archReg = (int)memLocOpnd2.getValue();
				if(memLocOpnd2.getOperandType() == OperandType.integerRegister)
				{
					reorderBufferEntry.setOperand2PhyReg2(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(archReg));
				}
				else if(memLocOpnd2.getOperandType() == OperandType.floatRegister)
				{
					reorderBufferEntry.setOperand2PhyReg2(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(archReg));
				}
				else if(memLocOpnd1.getOperandType() == OperandType.machineSpecificRegister)
				{
					reorderBufferEntry.setOperand2PhyReg2(archReg);
				}
				else
				{
					reorderBufferEntry.setOperand2PhyReg2(-1);
				}
			}
		}
		else
		{
			reorderBufferEntry.setOperand2PhyReg1(-1);
			reorderBufferEntry.setOperand2PhyReg2(-1);
		}
	}
	
	private void processDestOperand(ReorderBufferEntry reorderBufferEntry)
	{
		Operand tempOpnd = reorderBufferEntry.getInstruction().getDestinationOperand();
		if(tempOpnd == null)
		{
			this.eventQueue.addEvent(
					new RenameCompleteEvent(
							core,
							reorderBufferEntry,
							GlobalClock.getCurrentTime() + core.getRenamingTime()
							));
			return;
		}
		
		OperandType tempOpndType = tempOpnd.getOperandType(); 
		
		if(tempOpndType != OperandType.integerRegister &&
				tempOpndType != OperandType.floatRegister &&
				tempOpndType != OperandType.machineSpecificRegister)
		{
			this.eventQueue.addEvent(
					new RenameCompleteEvent(
							core,
							reorderBufferEntry,
							GlobalClock.getCurrentTime() + core.getRenamingTime()
							));
		}		
		else
		{
			if(tempOpndType == OperandType.machineSpecificRegister)
			{
				handleMSR(reorderBufferEntry);				
			}			
			else
			{
				handleIntFloat(reorderBufferEntry);				
			}
		}
	}
	
	void handleMSR(ReorderBufferEntry reorderBufferEntry)
	{
		RegisterFile tempRF = core.getExecEngine().getMachineSpecificRegisterFile();
		Operand tempOpnd = reorderBufferEntry.getInstruction().getDestinationOperand();
		
		reorderBufferEntry.setPhysicalDestinationRegister((int) tempOpnd.getValue());
		
		if(tempRF.getValueValid((int) tempOpnd.getValue()) == true)
		{
			//destination MSR available
			this.eventQueue.addEvent(
					new RenameCompleteEvent(
							core,
							reorderBufferEntry,
							GlobalClock.getCurrentTime() + core.getRenamingTime()
							));
		}
		
		else
		{
			//schedule AllocateDestinationRegisterEvent
			long regReadyTime = tempRF.getProducerROBEntry((int) tempOpnd.getValue()).getReadyAtTime();
			this.eventQueue.addEvent(
					new AllocateDestinationRegisterEvent(
							reorderBufferEntry,
							null,
							core,
							regReadyTime
							));
			//stall decode because physical register for destination was not allocated
			core.getExecEngine().setStallDecode1(true);
		}
	}
	
	void handleIntFloat(ReorderBufferEntry reorderBufferEntry)
	{
		RenameTable tempRN;
		OperandType tempOpndType = reorderBufferEntry.getInstruction().
									getDestinationOperand().getOperandType();
		if(tempOpndType == OperandType.integerRegister)
		{
			tempRN = core.getExecEngine().getIntegerRenameTable();
		}
		else
		{
			tempRN = core.getExecEngine().getFloatingPointRenameTable();
		}
		
		int r = tempRN.allocatePhysicalRegister((int) reorderBufferEntry.getInstruction().getDestinationOperand().getValue());
		if(r >= 0)
		{
			//physical register found
			reorderBufferEntry.setPhysicalDestinationRegister(r);
			tempRN.setValueValid(false, r);
			tempRN.setProducerROBEntry(reorderBufferEntry, r);
			this.eventQueue.addEvent(
					new RenameCompleteEvent(
							core,
							reorderBufferEntry,
							GlobalClock.getCurrentTime() + core.getRenamingTime()
							));
		}
		else
		{
			//look for a physical register in the next clock cycle
			//schedule a FindPhysicalRegisterEvent at time current_clock+1
			this.eventQueue.addEvent(
					new AllocateDestinationRegisterEvent(
							reorderBufferEntry,
							tempRN,
							core,
							GlobalClock.getCurrentTime()+1
							));
			//stall decode because physical register for destination was not allocated
			core.getExecEngine().setStallDecode1(true);
		}
	}
}