package pipeline.outoforder;

import config.SimulationConfig;
import generic.InstructionLinkedList;
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
		InstructionLinkedList inputToPipeline = core.getIncomingInstructions(threadID);
		
		for(int i = 0; i < core.getDecodeWidth(); i++)
		{
			if(core.getExecEngine().getReorderBuffer().isFull() == false
					//&& if head of instructionList is a load/store and LSQ is not full TODO
					&& core.getExecEngine().getInstructionWindow().isFull() == false
					&& core.getExecEngine().isStallDecode1() == false)
			{
				if(inputToPipeline.getListSize() == 0)
				{
					System.out.println("this shouldn't be happening");
					break;
				}
				
				newInstruction = inputToPipeline.peekInstructionAt(0);
				
				if((newInstruction.getOperationType() != OperationType.load &&
					newInstruction.getOperationType() != OperationType.store) ||
					(!this.core.getExecEngine().coreMemSys.getLsqueue().isFull()))
				{
					newInstruction = inputToPipeline.pollFirst();
					
					if(newInstruction != null)
					{
						if(newInstruction.getOperationType() == OperationType.inValid)
						{
							core.getExecEngine().setDecodePipeEmpty(threadID, true);
							break;
						}
						//to detach memory system
						/*if(newInstruction.getOperationType() == OperationType.load ||
								newInstruction.getOperationType() == OperationType.store)
						{
							i--;
							continue;
						}*/
						makeROBEntries(newInstruction);
						if(SimulationConfig.debugMode)
						{
							System.out.println("decoded : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : " + newInstruction);
						}
					}
					else
					{
						System.out.println("input to pipe is empty");
						break;
					}
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
				.getReorderBuffer().addInstructionToROB(newInstruction, threadID);
			
			//TODO if load or store, make entry in LSQ
			if(newInstruction.getOperationType() == OperationType.load ||
					newInstruction.getOperationType() == OperationType.store)
			{
				boolean isLoad;
				if (newInstruction.getOperationType() == OperationType.load)
					isLoad = true;
				else
					isLoad = false;
					
				newROBEntry.setLsqEntry(this.core.getExecEngine().coreMemSys.getLsqueue().addEntry(isLoad, 
									newROBEntry.getInstruction().getSourceOperand1().getValue(), newROBEntry));
			}
			
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
			reorderBufferEntry.setOperand1PhyReg1(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
			reorderBufferEntry.setOperand1PhyReg2(-1);
		}
		else if(tempOpnd.getOperandType() == OperandType.floatRegister)
		{
			reorderBufferEntry.setOperand1PhyReg1(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
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
					reorderBufferEntry.setOperand1PhyReg1(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(memLocOpnd1.getOperandType() == OperandType.floatRegister)
				{
					reorderBufferEntry.setOperand1PhyReg1(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
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
					reorderBufferEntry.setOperand1PhyReg2(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(memLocOpnd2.getOperandType() == OperandType.floatRegister)
				{
					reorderBufferEntry.setOperand1PhyReg2(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(memLocOpnd2.getOperandType() == OperandType.machineSpecificRegister)
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
			reorderBufferEntry.setOperand2PhyReg1(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
			reorderBufferEntry.setOperand2PhyReg2(-1);
		}
		else if(tempOpnd.getOperandType() == OperandType.floatRegister)
		{
			reorderBufferEntry.setOperand2PhyReg1(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
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
					reorderBufferEntry.setOperand2PhyReg1(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(memLocOpnd1.getOperandType() == OperandType.floatRegister)
				{
					reorderBufferEntry.setOperand2PhyReg1(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
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
					reorderBufferEntry.setOperand2PhyReg2(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(memLocOpnd2.getOperandType() == OperandType.floatRegister)
				{
					reorderBufferEntry.setOperand2PhyReg2(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(memLocOpnd2.getOperandType() == OperandType.machineSpecificRegister)
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
							GlobalClock.getCurrentTime() + core.getRenamingTime()*core.getStepSize()
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
							GlobalClock.getCurrentTime() + core.getRenamingTime()*core.getStepSize()
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
		RegisterFile tempRF = core.getExecEngine().getMachineSpecificRegisterFile(threadID);
		Operand tempOpnd = reorderBufferEntry.getInstruction().getDestinationOperand();
		
		int destPhyReg = (int) tempOpnd.getValue();
		reorderBufferEntry.setPhysicalDestinationRegister(destPhyReg);
		
		if(tempRF.getValueValid(destPhyReg) == true)
		{
			//destination MSR available
			
			tempRF.setProducerROBEntry(reorderBufferEntry, destPhyReg);
			tempRF.setValueValid(false, destPhyReg);
			
			this.eventQueue.addEvent(
					new RenameCompleteEvent(
							core,
							reorderBufferEntry,
							GlobalClock.getCurrentTime() + core.getRenamingTime()*core.getStepSize()
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
		
		int r = tempRN.allocatePhysicalRegister(threadID, (int) reorderBufferEntry.getInstruction().getDestinationOperand().getValue());
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
							GlobalClock.getCurrentTime() + core.getRenamingTime()*core.getStepSize()
							));
		}
		else
		{
			//look for a physical register in the next clock cycle
			//schedule a AllocateDestinationRegisterEvent at time current_clock+1
			this.eventQueue.addEvent(
					new AllocateDestinationRegisterEvent(
							reorderBufferEntry,
							tempRN,
							core,
							GlobalClock.getCurrentTime()+core.getStepSize()
							));
			//stall decode because physical register for destination was not allocated
			core.getExecEngine().setStallDecode1(true);
		}
	}
}