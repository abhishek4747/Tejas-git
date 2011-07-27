package pipeline.outoforder;

import emulatorinterface.translator.x86.objparser.ObjParser;
import generic.Event;
import generic.Core;
import generic.Instruction;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;

/**
 * scheduled at the end of decode time
 * ROB entries are made
 * renaming is performed
 * RenameCompleteEvent is scheduled
 * note - decode complete event represents decode-width number of instructions
 */

public class DecodeCompleteEvent extends Event {
	
	Core core;
	
	public DecodeCompleteEvent(Core core, long eventTime)
	{
		super(eventTime, 1, 0);
		this.core = core;
	}

	@Override
	public void handleEvent() {
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
					&& core.getExecEngine().isStallDecode() == false)
			{
				newInstruction = ObjParser.instructionList.pollFirst();
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
		int archReg = (int) reorderBufferEntry.getInstruction().getSourceOperand1().getValue();
		if(reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.integerRegister)
		{
			reorderBufferEntry.setOperand1PhyReg(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(archReg));
		}
		else if(reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.floatRegister)
		{
			reorderBufferEntry.setOperand1PhyReg(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(archReg));
		}
		else if(reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.machineSpecificRegister)
		{
			reorderBufferEntry.setOperand1PhyReg(archReg);
		}
		else
		{
			reorderBufferEntry.setOperand1PhyReg(-1);
		}
	}
	
	private void processOperand2(ReorderBufferEntry reorderBufferEntry)
	{
		int archReg = (int) reorderBufferEntry.getInstruction().getSourceOperand2().getValue();
		if(reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.integerRegister)
		{
			reorderBufferEntry.setOperand2PhyReg(core.getExecEngine().getIntegerRenameTable().getPhysicalRegister(archReg));
		}
		else if(reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.floatRegister)
		{
			reorderBufferEntry.setOperand2PhyReg(core.getExecEngine().getFloatingPointRenameTable().getPhysicalRegister(archReg));
		}
		else if(reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.machineSpecificRegister)
		{
			reorderBufferEntry.setOperand2PhyReg(archReg);
		}
		else
		{
			reorderBufferEntry.setOperand2PhyReg(-1);
		}
	}
	
	private void processDestOperand(ReorderBufferEntry reorderBufferEntry)
	{
		OperandType tempOpndType = reorderBufferEntry.getInstruction().
									getDestinationOperand().getOperandType(); 
		
		if(tempOpndType != OperandType.integerRegister &&
				tempOpndType != OperandType.floatRegister &&
				tempOpndType != OperandType.machineSpecificRegister)
		{
			core.getEventQueue().addEvent(
					new RenameCompleteEvent(
							core,
							reorderBufferEntry,
							core.getClock() + core.getRenamingTime()
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
			core.getEventQueue().addEvent(
					new RenameCompleteEvent(
							core,
							reorderBufferEntry,
							core.getClock() + core.getRenamingTime()
							));
		}
		
		else
		{
			//schedule AllocateDestinationRegisterEvent
			long regReadyTime = tempRF.getProducerROBEntry((int) tempOpnd.getValue()).getReadyAtTime();
			core.getEventQueue().addEvent(
					new AllocateDestinationRegisterEvent(
							reorderBufferEntry,
							null,
							core,
							regReadyTime
							));
			//stall decode because physical register for destination was not allocated
			core.getExecEngine().setStallDecode(true);
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
			core.getEventQueue().addEvent(
					new RenameCompleteEvent(
							core,
							reorderBufferEntry,
							core.getClock() + core.getRenamingTime()
							));
		}
		else
		{
			//look for a physical register in the next clock cycle
			//schedule a FindPhysicalRegisterEvent at time current_clock+1
			core.getEventQueue().addEvent(
					new AllocateDestinationRegisterEvent(
							reorderBufferEntry,
							tempRN,
							core,
							core.getClock()+1
							));
			//stall decode because physical register for destination was not allocated
			core.getExecEngine().setStallDecode(true);
		}
	}
}