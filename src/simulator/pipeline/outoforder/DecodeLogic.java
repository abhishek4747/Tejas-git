package pipeline.outoforder;

import config.SimulationConfig;
import pipeline.perfect.DecodeCompleteEventPerfect;
import generic.Core;
import generic.GlobalClock;
import generic.Instruction;
import generic.InstructionLinkedList;
import generic.EventQueue;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;
import generic.SimulationElement;
import generic.Time_t;

/**
 * schedule the completion of decode of instructions that are read from the fetch buffer
 * in the current clock cycle
 */

public class DecodeLogic extends SimulationElement {

	//the containing core
	private Core core;
	int threadID;
	EventQueue eventQueue;
	
	ExecutionEngine execEngine;
	
	public DecodeLogic(Core containingCore, ExecutionEngine execEngine)
	{
		super(1, new Time_t(-1), new Time_t(-1), -1);
		core = containingCore;
		threadID = 0;
		eventQueue = core.getEventQueue();
		
		this.execEngine = execEngine;
	}
	
	public void scheduleDecodeCompletion()
	{
		//decode completion of decodeWidth number of instructions scheduled
		
		if(execEngine.isStallDecode1() == false &&
				execEngine.isStallDecode2() == false)
		{
			int ctr = 0;
			while(execEngine.isDecodePipeEmpty(threadID) == true
					&& ctr < core.getNo_of_threads())
			{
				ctr++;
			}
			
			if(ctr == core.getNo_of_threads())
			{
				execEngine.setAllPipesEmpty(true);
			}
			else
			{
				if (core.perfectPipeline == false)
					readDecodePipe();
				else
					core.getEventQueue().addEvent(
							new DecodeCompleteEventPerfect(
									core,
									threadID,
									GlobalClock.getCurrentTime()
									));
			}
		}
		
		if(execEngine.isAllPipesEmpty() == false)
		{
			threadID = (threadID + 1)%core.getNo_of_threads();			
		}
		
		
	}
	

	
	public void readDecodePipe()
	{
		Instruction newInstruction;		
		InstructionLinkedList inputToPipeline = core.getIncomingInstructions(threadID);
		
		int decodeWidth = core.getDecodeWidth();
		
		for(int i = 0; i < decodeWidth; i++)
		{
			if(execEngine.getReorderBuffer().isFull() == false
					&& execEngine.getInstructionWindow().isFull() == false
					&& execEngine.isStallDecode1() == false)
			{
				if(inputToPipeline.getListSize() == 0)
				{
					System.out.println("this shouldn't be happening");
					break;
				}
				
				newInstruction = inputToPipeline.peekInstructionAt(0);
				OperationType tempOpType = newInstruction.getOperationType();
				
				if((tempOpType != OperationType.load &&
					tempOpType != OperationType.store) ||
					(!this.execEngine.coreMemSys.getLsqueue().isFull()))
				{
					newInstruction = inputToPipeline.pollFirst();
					
					if(newInstruction != null)
					{
						if(tempOpType == OperationType.inValid)
						{
							execEngine.setDecodePipeEmpty(threadID, true);
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
		OperationType tempOpType = null;
		if(newInstruction != null)
		{
			tempOpType = newInstruction.getOperationType();
		}
		
		if(newInstruction != null &&
				tempOpType != OperationType.nop &&
				tempOpType != OperationType.inValid)
		{			
			ReorderBufferEntry newROBEntry = execEngine.getReorderBuffer()
											.addInstructionToROB(newInstruction, threadID);
			
			//if load or store, make entry in LSQ
			if(tempOpType == OperationType.load ||
					tempOpType == OperationType.store)
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

		OperandType tempOpndType = tempOpnd.getOperandType();
		int archReg = (int) tempOpnd.getValue();
		if(tempOpndType == OperandType.integerRegister)
		{
			reorderBufferEntry.setOperand1PhyReg1(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
			reorderBufferEntry.setOperand1PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.floatRegister)
		{
			reorderBufferEntry.setOperand1PhyReg1(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
			reorderBufferEntry.setOperand1PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.machineSpecificRegister)
		{
			reorderBufferEntry.setOperand1PhyReg1(archReg);
			reorderBufferEntry.setOperand1PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.memory)
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
				tempOpndType = memLocOpnd1.getOperandType();
				
				if(tempOpndType == OperandType.integerRegister)
				{
					reorderBufferEntry.setOperand1PhyReg1(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.floatRegister)
				{
					reorderBufferEntry.setOperand1PhyReg1(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.machineSpecificRegister)
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
				tempOpndType = memLocOpnd2.getOperandType();
				
				if(tempOpndType == OperandType.integerRegister)
				{
					reorderBufferEntry.setOperand1PhyReg2(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.floatRegister)
				{
					reorderBufferEntry.setOperand1PhyReg2(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.machineSpecificRegister)
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

		OperandType tempOpndType = tempOpnd.getOperandType();
		int archReg = (int) tempOpnd.getValue();
		if(tempOpndType == OperandType.integerRegister)
		{
			reorderBufferEntry.setOperand2PhyReg1(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
			reorderBufferEntry.setOperand2PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.floatRegister)
		{
			reorderBufferEntry.setOperand2PhyReg1(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
			reorderBufferEntry.setOperand2PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.machineSpecificRegister)
		{
			reorderBufferEntry.setOperand2PhyReg1(archReg);
			reorderBufferEntry.setOperand2PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.memory)
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
				tempOpndType = memLocOpnd1.getOperandType();
				
				if(tempOpndType == OperandType.integerRegister)
				{
					reorderBufferEntry.setOperand2PhyReg1(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.floatRegister)
				{
					reorderBufferEntry.setOperand2PhyReg1(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.machineSpecificRegister)
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
				tempOpndType = memLocOpnd2.getOperandType();
				
				if(tempOpndType == OperandType.integerRegister)
				{
					reorderBufferEntry.setOperand2PhyReg2(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.floatRegister)
				{
					reorderBufferEntry.setOperand2PhyReg2(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.machineSpecificRegister)
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
		RegisterFile tempRF = execEngine.getMachineSpecificRegisterFile(threadID);
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
			execEngine.setStallDecode1(true);
		}
	}
	
	void handleIntFloat(ReorderBufferEntry reorderBufferEntry)
	{
		RenameTable tempRN;
		OperandType tempOpndType = reorderBufferEntry.getInstruction().
									getDestinationOperand().getOperandType();
		if(tempOpndType == OperandType.integerRegister)
		{
			tempRN = execEngine.getIntegerRenameTable();
		}
		else
		{
			tempRN = execEngine.getFloatingPointRenameTable();
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
			execEngine.setStallDecode1(true);
		}
	}
	
}