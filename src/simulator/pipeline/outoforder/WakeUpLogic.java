package pipeline.outoforder;

import generic.Core;
import generic.Instruction;
import generic.Operand;
import generic.OperandType;

import java.util.LinkedList;

public class WakeUpLogic {
	
	static LinkedList<IWEntry> tempList;
	static
	{
		tempList = new LinkedList<IWEntry>();
	}
	
	static public void wakeUpLogic(Core core, OperandType opndType, int physicalRegister)
	{
		boolean toWakeUp;
		ReorderBufferEntry ROBEntry;
		
		Instruction instruction;
		Operand opnd1;
		Operand opnd2;
		OperandType opnd1Type;
		OperandType opnd2Type;
		
		InstructionWindow instructionWindow = core.getExecEngine().getInstructionWindow();
		IWEntry[] IW = instructionWindow.getIW();
		
		while(tempList.size() > 0)
		{
			tempList.removeFirst();
		}
		
		int i = 0;
				
		while(i < instructionWindow.getMaxIWSize())
		{
			IWEntry IWentry = IW[i];
			if(IWentry.isValid() == false)
			{
				i++;
				continue;
			}
			ROBEntry = IWentry.getAssociatedROBEntry();
			toWakeUp = false;
			
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
					toWakeUp = true;
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
						toWakeUp = true;
					}
				}
			}
			
			if(ROBEntry.isOperand2Available() == false)
			{
				if(opnd2Type == opndType
						&& ROBEntry.getOperand2PhyReg1() == physicalRegister)
				{
					ROBEntry.setOperand2Available(true);
					toWakeUp = true;
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
						toWakeUp = true;
					}
				}
			}
			
			if(toWakeUp == true)
			{
				tempList.addLast(IWentry);
			}
			
			i++;
		}
		
		i = 0;
		while(i < tempList.size())
		{
			tempList.get(i).issueInstruction();
			i++;
		}
	}

}