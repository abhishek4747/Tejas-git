package pipeline.outoforder;

import generic.Core;
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
			
			if(ROBEntry.isOperand1Available() == false)
			{
				if(ROBEntry.getInstruction().getSourceOperand1().getOperandType() == opndType
						&& ROBEntry.getOperand1PhyReg1() == physicalRegister)
				{
					ROBEntry.setOperand1Available(true);
					toWakeUp = true;
				}
				if(ROBEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.memory)
				{
					if(ROBEntry.getInstruction().getSourceOperand1().getMemoryLocationFirstOperand() != null &&
							ROBEntry.getInstruction().getSourceOperand1().getMemoryLocationFirstOperand().getOperandType() == opndType
							&& ROBEntry.getOperand1PhyReg1() == physicalRegister)
					{
						ROBEntry.setOperand11Available(true);
					}
					if(ROBEntry.getInstruction().getSourceOperand1().getMemoryLocationSecondOperand() != null &&
							ROBEntry.getInstruction().getSourceOperand1().getMemoryLocationSecondOperand().getOperandType() == opndType
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
				if(ROBEntry.getInstruction().getSourceOperand2().getOperandType() == opndType
						&& ROBEntry.getOperand2PhyReg1() == physicalRegister)
				{
					ROBEntry.setOperand2Available(true);
					toWakeUp = true;
				}
				if(ROBEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.memory)
				{
					if(ROBEntry.getInstruction().getSourceOperand2().getMemoryLocationFirstOperand() != null &&
							ROBEntry.getInstruction().getSourceOperand2().getMemoryLocationFirstOperand().getOperandType() == opndType
							&& ROBEntry.getOperand2PhyReg1() == physicalRegister)
					{
						ROBEntry.setOperand21Available(true);
					}
					if(ROBEntry.getInstruction().getSourceOperand2().getMemoryLocationSecondOperand() != null &&
							ROBEntry.getInstruction().getSourceOperand2().getMemoryLocationSecondOperand().getOperandType() == opndType
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