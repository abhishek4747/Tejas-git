package pipeline.outoforder;

import generic.Core;
import generic.Instruction;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;

public class WakeUpLogic {
	
	/*
	 * given an operand type, and the physical register, of instruction that just completed execution
	 * all current entries in the ROB are scanned
	 * all those entries that have one or both of their source operands matching the given operand type and physical register
	 * sets the availability flags appropriately
	 * 
	 * default value for startIndex must be -1
	 * startIndex has significance in the case of a single cycle operation,
	 * when the wake up is initiated by the select logic
	 * here, only those ROB entries that appear after the initiating instruction in program order must be considered
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
		
		while(ROBEntries[i].isValid && ctr < ROB.MaxROBSize)
		{
			ctr++;
			
			ROBEntry = ROBEntries[i];
			
			if(ROBEntries[i].isRenameDone == false)
			{
				break;
			}
			
			if(ROBEntries[i].isIssued == true)
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
					ROBEntry.instruction.getDestinationOperand() != null &&
					ROBEntry.instruction.getDestinationOperand().getOperandType() == opndType
					||
					ROBEntry.instruction.getOperationType() == OperationType.xchg &&
					ROBEntry.instruction.getOperand1() != null &&
					ROBEntry.instruction.getOperand1().getOperandType() == opndType &&
					ROBEntry.operand1PhyReg1 == physicalRegister
					||
					ROBEntry.instruction.getOperationType() == OperationType.xchg &&
					ROBEntry.instruction.getOperand2() != null &&
					ROBEntry.instruction.getOperand2().getOperandType() == opndType &&
					ROBEntry.operand2PhyReg1 == physicalRegister)
			{
				break;
			}
			
			i = (i + 1) % ROB.MaxROBSize;
		}
		
		if(opndType == OperandType.integerRegister)
		{
			execEngine.getIntegerRenameTable().setValueValid(true, physicalRegister);
		}
		else if(opndType == OperandType.floatRegister)
		{
			execEngine.getFloatingPointRenameTable().setValueValid(true, physicalRegister);
		}
//		else if(opndType == OperandType.machineSpecificRegister)
//		{
//			execEngine.getMachineSpecificRegisterFile(threadID).setValueValid(true, physicalRegister);
//		} suspect bug : register file value valid should be set only at write back
		else if(opndType == OperandType.machineSpecificRegister)
		{
			for(int j = 0; j < core.getDecodeWidth(); j++)
			{
				if(execEngine.getRenameBuffer()[j] != null && execEngine.getRenameBuffer()[j].getInstruction().getThreadID() == threadID)
				{
					Operand tempOpnd = execEngine.getRenameBuffer()[j].getInstruction().getSourceOperand1();
					if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
					{
						if(physicalRegister == (int)tempOpnd.getValue())
						{
							execEngine.getRenameBuffer()[j].setOperand1Available(true);
						}
					}
					else if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.memory)
					{
						tempOpnd = execEngine.getRenameBuffer()[j].getInstruction().getSourceOperand1().getMemoryLocationFirstOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								execEngine.getRenameBuffer()[j].setOperand11Available(true);
								if(execEngine.getRenameBuffer()[j].isOperand12Available() == true)
								{
									execEngine.getRenameBuffer()[j].setOperand1Available(true);
								}
							}
						}
						tempOpnd = execEngine.getRenameBuffer()[j].getInstruction().getSourceOperand1().getMemoryLocationSecondOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								execEngine.getRenameBuffer()[j].setOperand12Available(true);
								if(execEngine.getRenameBuffer()[j].isOperand11Available() == true)
								{
									execEngine.getRenameBuffer()[j].setOperand1Available(true);
								}
							}
						}
					}
					
					tempOpnd = execEngine.getRenameBuffer()[j].getInstruction().getSourceOperand2();
					if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
					{
						if(physicalRegister == (int)tempOpnd.getValue())
						{
							execEngine.getRenameBuffer()[j].setOperand2Available(true);
						}
					}
					else if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.memory)
					{
						tempOpnd = execEngine.getRenameBuffer()[j].getInstruction().getSourceOperand2().getMemoryLocationFirstOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								execEngine.getRenameBuffer()[j].setOperand21Available(true);
								if(execEngine.getRenameBuffer()[j].isOperand22Available() == true)
								{
									execEngine.getRenameBuffer()[j].setOperand2Available(true);
								}
							}
						}
						tempOpnd = execEngine.getRenameBuffer()[j].getInstruction().getSourceOperand2().getMemoryLocationSecondOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								execEngine.getRenameBuffer()[j].setOperand22Available(true);
								if(execEngine.getRenameBuffer()[j].isOperand21Available() == true)
								{
									execEngine.getRenameBuffer()[j].setOperand2Available(true);
								}
							}
						}
					}
				}
			}
			
			for(int j = 0; j < core.getDecodeWidth(); j++)
			{
				if(execEngine.getDecodeBuffer()[j] != null && execEngine.getDecodeBuffer()[j].getInstruction().getThreadID() == threadID)
				{
					Operand tempOpnd = execEngine.getDecodeBuffer()[j].getInstruction().getSourceOperand1();
					if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
					{
						if(physicalRegister == (int)tempOpnd.getValue())
						{
							execEngine.getDecodeBuffer()[j].setOperand1Available(true);
						}
					}
					else if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.memory)
					{
						tempOpnd = execEngine.getDecodeBuffer()[j].getInstruction().getSourceOperand1().getMemoryLocationFirstOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								execEngine.getDecodeBuffer()[j].setOperand11Available(true);
								if(execEngine.getDecodeBuffer()[j].isOperand12Available() == true)
								{
									execEngine.getDecodeBuffer()[j].setOperand1Available(true);
								}
							}
						}
						tempOpnd = execEngine.getDecodeBuffer()[j].getInstruction().getSourceOperand1().getMemoryLocationSecondOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								execEngine.getDecodeBuffer()[j].setOperand12Available(true);
								if(execEngine.getDecodeBuffer()[j].isOperand11Available() == true)
								{
									execEngine.getDecodeBuffer()[j].setOperand1Available(true);
								}
							}
						}
					}
					
					tempOpnd = execEngine.getDecodeBuffer()[j].getInstruction().getSourceOperand2();
					if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
					{
						if(physicalRegister == (int)tempOpnd.getValue())
						{
							execEngine.getDecodeBuffer()[j].setOperand2Available(true);
						}
					}
					else if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.memory)
					{
						tempOpnd = execEngine.getDecodeBuffer()[j].getInstruction().getSourceOperand2().getMemoryLocationFirstOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								execEngine.getDecodeBuffer()[j].setOperand21Available(true);
								if(execEngine.getDecodeBuffer()[j].isOperand22Available() == true)
								{
									execEngine.getDecodeBuffer()[j].setOperand2Available(true);
								}
							}
						}
						tempOpnd = execEngine.getDecodeBuffer()[j].getInstruction().getSourceOperand2().getMemoryLocationSecondOperand();
						if(tempOpnd != null && tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
						{
							if(physicalRegister == (int)tempOpnd.getValue())
							{
								execEngine.getDecodeBuffer()[j].setOperand22Available(true);
								if(execEngine.getDecodeBuffer()[j].isOperand21Available() == true)
								{
									execEngine.getDecodeBuffer()[j].setOperand2Available(true);
								}
							}
						}
					}
				}
			}
		}
	}

}