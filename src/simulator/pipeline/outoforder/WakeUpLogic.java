package pipeline.outoforder;

import generic.Core;
import generic.Instruction;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;

public class WakeUpLogic {
	
	/*
	 * given an operand type, and the physical register, of instruction that just completed execution
	 * all current entries in the IW are scanned
	 * all those entries that have one or both of their source operands matching the given operand type and physical register
	 * sets the availability flags appropriately
	 * 
	 * startIndex
	 * only those ROB entries that appear after the initiating instruction in program order must be considered
	 * startIndex must be set to index of the completing instruction in the ROB
	 * 
	 * threadID is applicable only if opndType is machine specific register
	 */
	
	static public void wakeUpLogic(Core core, OperandType opndType, int physicalRegister, int threadID, int startIndex)
	{
		OutOrderExecutionEngine execEngine = (OutOrderExecutionEngine)core.getExecEngine();
		ReorderBufferEntry ROBEntry;
		ReorderBuffer ROB = execEngine.getReorderBuffer();
		ReorderBufferEntry[] ROBEntries = ROB.getROB();
		
		Instruction instruction;
		Operand opnd1;
		Operand opnd2;
		OperandType opnd1Type;
		OperandType opnd2Type;
		
		int i, ctr = 0;
		if(startIndex != -1)
		{
			i = startIndex;
		}
		else
		{
			i = ROB.head;
		}
		
		while(ROBEntries[i].isValid() && ctr < ROB.MaxROBSize)
		{
			ctr++;
			
			ROBEntry = ROBEntries[i];
			
			if(ROBEntries[i].isRenameDone() == false)
			{
				break;
			}
			
			if(ROBEntries[i].getIssued() == true
					|| ROBEntries[i].getAssociatedIWEntry() == null)
			{
				i = (i + 1) % ROB.MaxROBSize;
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
			
			if(ROBEntry.getPhysicalDestinationRegister() == physicalRegister &&
					ROBEntry.getInstruction().getDestinationOperand() != null &&
					ROBEntry.getInstruction().getDestinationOperand().getOperandType() == opndType
					||
					ROBEntry.getInstruction().getOperationType() == OperationType.xchg &&
					ROBEntry.getInstruction().getSourceOperand1() != null &&
					ROBEntry.getInstruction().getSourceOperand1().getOperandType() == opndType &&
					ROBEntry.getOperand1PhyReg1() == physicalRegister
					||
					ROBEntry.getInstruction().getOperationType() == OperationType.xchg &&
					ROBEntry.getInstruction().getSourceOperand2() != null &&
					ROBEntry.getInstruction().getSourceOperand2().getOperandType() == opndType &&
					ROBEntry.getOperand2PhyReg1() == physicalRegister)
			{
				//this particular instruction also writes to the same register as the one that initiated
				//the wake-up. all subsequent consumers of this register should not be woken up.
				break;
			}
			
			i = (i + 1) % ROB.MaxROBSize;
		}
		
		/*
		 * aiding decoded instructions that are not yet in the IW.
		 * 
		 * if this is not done, an instruction being renamed in this or the previous cycle
		 * 	1) does not get it's source operands from the RF as write-back of the producer hasn't completed
		 * 	2) misses the wake-up signal as it is not yet in the IW
		 * 	thus, staying forever in the IW.
		 * 
		 * the below code is part of the solution, the remainder is at the write-back stage
		 * 
		 */
		if(opndType == OperandType.integerRegister)
		{
			execEngine.getIntegerRenameTable().setValueValid(true, physicalRegister);
		}
		else if(opndType == OperandType.floatRegister)
		{
			execEngine.getFloatingPointRenameTable().setValueValid(true, physicalRegister);
		}
		else if(opndType == OperandType.machineSpecificRegister)
		{
			for(int j = 0; j < core.getDecodeWidth(); j++)
			{
				ReorderBufferEntry jthEntry = execEngine.getRenameBuffer().peek(j);
				if(jthEntry != null && jthEntry.getInstruction().getThreadID() == threadID)
				{
					Operand tempOpnd = jthEntry.getInstruction().getSourceOperand1();
					if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
					{
						if(physicalRegister == (int)tempOpnd.getValue())
						{
							jthEntry.setOperand1Available(true);
						}
					}
					else if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.memory)
					{
						tempOpnd = jthEntry.getInstruction().getSourceOperand1().getMemoryLocationFirstOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								jthEntry.setOperand11Available(true);
								if(jthEntry.isOperand12Available() == true)
								{
									jthEntry.setOperand1Available(true);
								}
							}
						}
						tempOpnd = jthEntry.getInstruction().getSourceOperand1().getMemoryLocationSecondOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								jthEntry.setOperand12Available(true);
								if(jthEntry.isOperand11Available() == true)
								{
									jthEntry.setOperand1Available(true);
								}
							}
						}
					}
					
					tempOpnd = jthEntry.getInstruction().getSourceOperand2();
					if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
					{
						if(physicalRegister == (int)tempOpnd.getValue())
						{
							jthEntry.setOperand2Available(true);
						}
					}
					else if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.memory)
					{
						tempOpnd = jthEntry.getInstruction().getSourceOperand2().getMemoryLocationFirstOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								jthEntry.setOperand21Available(true);
								if(jthEntry.isOperand22Available() == true)
								{
									jthEntry.setOperand2Available(true);
								}
							}
						}
						tempOpnd = jthEntry.getInstruction().getSourceOperand2().getMemoryLocationSecondOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								jthEntry.setOperand22Available(true);
								if(jthEntry.isOperand21Available() == true)
								{
									jthEntry.setOperand2Available(true);
								}
							}
						}
					}
				}
			}
			
			for(int j = 0; j < core.getDecodeWidth(); j++)
			{
				ReorderBufferEntry jthEntry = execEngine.getDecodeBuffer().peek(j);
				if(jthEntry != null && jthEntry.getInstruction().getThreadID() == threadID)
				{
					Operand tempOpnd = jthEntry.getInstruction().getSourceOperand1();
					if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
					{
						if(physicalRegister == (int)tempOpnd.getValue())
						{
							jthEntry.setOperand1Available(true);
						}
					}
					else if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.memory)
					{
						tempOpnd = jthEntry.getInstruction().getSourceOperand1().getMemoryLocationFirstOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								jthEntry.setOperand11Available(true);
								if(jthEntry.isOperand12Available() == true)
								{
									jthEntry.setOperand1Available(true);
								}
							}
						}
						tempOpnd = jthEntry.getInstruction().getSourceOperand1().getMemoryLocationSecondOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								jthEntry.setOperand12Available(true);
								if(jthEntry.isOperand11Available() == true)
								{
									jthEntry.setOperand1Available(true);
								}
							}
						}
					}
					
					tempOpnd = jthEntry.getInstruction().getSourceOperand2();
					if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
					{
						if(physicalRegister == (int)tempOpnd.getValue())
						{
							jthEntry.setOperand2Available(true);
						}
					}
					else if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.memory)
					{
						tempOpnd = jthEntry.getInstruction().getSourceOperand2().getMemoryLocationFirstOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								jthEntry.setOperand21Available(true);
								if(jthEntry.isOperand22Available() == true)
								{
									jthEntry.setOperand2Available(true);
								}
							}
						}
						tempOpnd = jthEntry.getInstruction().getSourceOperand2().getMemoryLocationSecondOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								jthEntry.setOperand22Available(true);
								if(jthEntry.isOperand21Available() == true)
								{
									jthEntry.setOperand2Available(true);
								}
							}
						}
					}
				}
			}
		}
	}

}