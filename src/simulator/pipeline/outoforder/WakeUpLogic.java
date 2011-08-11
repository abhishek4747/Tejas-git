package pipeline.outoforder;

import generic.Core;
import generic.OperandType;

import java.util.LinkedList;

public class WakeUpLogic {
	
	static public void wakeUpLogic(Core core, OperandType opndType, int physicalRegister)
	{
		LinkedList<IWEntry> IW = core.getExecEngine().getInstructionWindow().getIW();
		LinkedList<IWEntry> tempList = new LinkedList<IWEntry>();
		int i = 0;
		boolean toWakeUp;
		ReorderBufferEntry ROBEntry;
				
		while(i < IW.size())
		{
			IWEntry IWentry = IW.get(i);
			ROBEntry = IWentry.getAssociatedROBEntry();
			toWakeUp = false;
			
			if(IWentry.isOperand1Available() == false)
			{
				if(ROBEntry.getInstruction().getSourceOperand1().getOperandType() == opndType
						&& ROBEntry.getOperand1PhyReg1() == physicalRegister)
				{
					IWentry.setOperand1Available(true);
					toWakeUp = true;
				}
				if(ROBEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.memory)
				{
					if(ROBEntry.getInstruction().getSourceOperand1().getMemoryLocationFirstOperand() != null &&
							ROBEntry.getInstruction().getSourceOperand1().getMemoryLocationFirstOperand().getOperandType() == opndType
							&& ROBEntry.getOperand1PhyReg1() == physicalRegister)
					{
						IWentry.setOperand11Available(true);
					}
					if(ROBEntry.getInstruction().getSourceOperand1().getMemoryLocationSecondOperand() != null &&
							ROBEntry.getInstruction().getSourceOperand1().getMemoryLocationSecondOperand().getOperandType() == opndType
							&& ROBEntry.getOperand1PhyReg2() == physicalRegister)
					{
						IWentry.setOperand12Available(true);
					}
					if(IWentry.isOperand11Available() && IWentry.isOperand12Available())
					{
						IWentry.setOperand1Available(true);
						toWakeUp = true;
					}
				}
			}
			
			if(IWentry.isOperand2Available() == false)
			{
				if(ROBEntry.getInstruction().getSourceOperand2().getOperandType() == opndType
						&& ROBEntry.getOperand2PhyReg1() == physicalRegister)
				{
					IWentry.setOperand2Available(true);
					toWakeUp = true;
				}
				if(ROBEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.memory)
				{
					if(ROBEntry.getInstruction().getSourceOperand2().getMemoryLocationFirstOperand() != null &&
							ROBEntry.getInstruction().getSourceOperand2().getMemoryLocationFirstOperand().getOperandType() == opndType
							&& ROBEntry.getOperand2PhyReg1() == physicalRegister)
					{
						IWentry.setOperand21Available(true);
					}
					if(ROBEntry.getInstruction().getSourceOperand2().getMemoryLocationSecondOperand() != null &&
							ROBEntry.getInstruction().getSourceOperand2().getMemoryLocationSecondOperand().getOperandType() == opndType
							&& ROBEntry.getOperand2PhyReg2() == physicalRegister)
					{
						IWentry.setOperand22Available(true);
					}
					if(IWentry.isOperand21Available() && IWentry.isOperand22Available())
					{
						IWentry.setOperand2Available(true);
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
