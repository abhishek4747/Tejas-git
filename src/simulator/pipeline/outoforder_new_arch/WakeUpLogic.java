package pipeline.outoforder_new_arch;

import generic.Core;
import generic.Instruction;
import generic.Operand;
import generic.OperandType;

public class WakeUpLogic {
	
	//tempList not required now
	/*static LinkedList<IWEntry> tempList;
	static
	{
		tempList = new LinkedList<IWEntry>();
	}*/
	
	/*
	 * given an operand type, and the physical register, of instruction I
	 * all current entries in the IW are scanned
	 * all those entries that have one or both of their source operands matching the given operand type and physical register
	 * sets the availability flags appropriately
	 * 
	 * default value for startIndex must be -1
	 * startIndex has significance in the case of a single cycle operation,
	 * when the wake up is initiated by the select logic
	 * here, only those IW entries that appear after the initiating instruction in program order must be considered
	 * startIndex must be set to index of the completing instruction in the ROB
	 * 
	 * threadID is applicable only if opndType is machine specific register
	 */
	static public void wakeUpLogic(Core core, OperandType opndType, int physicalRegister, int threadID, int startIndex)
	{
		ReorderBufferEntry ROBEntry;
		ReorderBuffer ROB = core.getExecEngine().getReorderBuffer();
		
		Instruction instruction;
		Operand opnd1;
		Operand opnd2;
		OperandType opnd1Type;
		OperandType opnd2Type;
		
		InstructionWindow instructionWindow = core.getExecEngine().getInstructionWindow();
		IWEntry[] IW = instructionWindow.getIW();
		
		/*while(tempList.size() > 0)
		{
			tempList.removeFirst();
		}*/
		
		int i;
		
		for(i = 0; i < instructionWindow.maxIWSize; i++)
		{
			IWEntry IWentry = IW[i];
			if(IWentry.isValid() == false)
			{
				continue;
			}
			
			ROBEntry = IWentry.getAssociatedROBEntry();
			if(ROB.indexOf(ROBEntry) < startIndex)
			{
				//this instruction precedes completing instruction in program order
				//so don't wake up
				continue;
			}
			
			instruction = ROBEntry.getInstruction();
			opnd1 = instruction.getSourceOperand1();
			opnd2 = instruction.getSourceOperand2();
			if(opnd1 != null)
				opnd1Type = opnd1.getOperandType();
			else
				opnd1Type = null;
			if(opnd2 != null)
				opnd2Type = opnd2.getOperandType();
			else
				opnd2Type = null;
			
			if(ROBEntry.isOperand1Available() == false)
			{
				if(opnd1Type == opndType
						&& ROBEntry.getOperand1PhyReg1() == physicalRegister)
				{
					ROBEntry.setOperand1Available(true);
				}
				if(opnd1Type == OperandType.memory)
				{
					if(opnd1.getMemoryLocationFirstOperand() != null &&
							opnd1.getMemoryLocationFirstOperand().getOperandType() == opndType
							&& ROBEntry.getOperand1PhyReg1() == physicalRegister)
					{
						ROBEntry.setOperand11Available(true);
					}
					if(opnd1.getMemoryLocationSecondOperand() != null &&
							opnd1.getMemoryLocationSecondOperand().getOperandType() == opndType
							&& ROBEntry.getOperand1PhyReg2() == physicalRegister)
					{
						ROBEntry.setOperand12Available(true);
					}
					if(ROBEntry.isOperand11Available() && ROBEntry.isOperand12Available())
					{
						ROBEntry.setOperand1Available(true);
					}
				}
			}
			
			if(ROBEntry.isOperand2Available() == false)
			{
				if(opnd2Type == opndType
						&& ROBEntry.getOperand2PhyReg1() == physicalRegister)
				{
					ROBEntry.setOperand2Available(true);
				}
				if(opnd2Type == OperandType.memory)
				{
					if(opnd2.getMemoryLocationFirstOperand() != null &&
							opnd2.getMemoryLocationFirstOperand().getOperandType() == opndType
							&& ROBEntry.getOperand2PhyReg1() == physicalRegister)
					{
						ROBEntry.setOperand21Available(true);
					}
					if(opnd2.getMemoryLocationSecondOperand() != null &&
							opnd2.getMemoryLocationSecondOperand().getOperandType() == opndType
							&& ROBEntry.getOperand2PhyReg2() == physicalRegister)
					{
						ROBEntry.setOperand22Available(true);
					}
					if(ROBEntry.isOperand21Available() && ROBEntry.isOperand22Available())
					{
						ROBEntry.setOperand2Available(true);
					}
				}
			}
			
			/*if(toWakeUp == true)
			{
				tempList.addLast(IWentry);
			}*/
		}
	
		/*i = 0;
		while(i < tempList.size())
		{
			tempList.get(i).issueInstruction();
			i++;
		}*/
		
		if(opndType == OperandType.integerRegister)
		{
			core.getExecEngine().getIntegerRenameTable().setValueValid(true, physicalRegister);
		}
		else if(opndType == OperandType.floatRegister)
		{
			core.getExecEngine().getFloatingPointRenameTable().setValueValid(true, physicalRegister);
		}
		else if(opndType == OperandType.machineSpecificRegister)
		{
			core.getExecEngine().getMachineSpecificRegisterFile(threadID).setValueValid(true, physicalRegister);
		}
	}

}