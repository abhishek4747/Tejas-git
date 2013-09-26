package pipeline.outoforder;

import generic.Core;
import generic.Operand;
import generic.OperandType;

public class OperandAvailabilityChecker {
	
	public static boolean[] isAvailable(ReorderBufferEntry reorderBufferEntry,
										Operand opnd,
										int phyReg1,
										int phyReg2,
										Core core)
	//phyReg2 required because if OperandType is memory, 2 physical registers may have to be specified
	{
		if(opnd == null)
		{
			return new boolean[]{true};
		}
		
		OutOrderExecutionEngine execEngine = (OutOrderExecutionEngine) core.getExecEngine();
		OperandType tempOpndType = opnd.getOperandType();
		int threadID = reorderBufferEntry.getThreadID();
		
		if(tempOpndType == OperandType.immediate ||
				tempOpndType == OperandType.inValid)
		{
			return new boolean[]{true};
		}
		
		if(tempOpndType == OperandType.integerRegister ||
				tempOpndType == OperandType.floatRegister ||
				tempOpndType == OperandType.machineSpecificRegister)
		{
			//Increment counters for power calculations
			if(tempOpndType==OperandType.integerRegister)
				core.powerCounters.incrementIntegerRegfileAccess(1);
			else
				core.powerCounters.incrementFloatRegfileAccess(1);

			if(tempOpndType == OperandType.machineSpecificRegister)
			{
				RegisterFile tempRF = execEngine.getMachineSpecificRegisterFile(threadID);
				if(tempRF.getValueValid(phyReg1) == true ||
						tempRF.getProducerROBEntry(phyReg1) == reorderBufferEntry/* ||
						execEngine.getReorderBuffer().indexOf(tempRF.getProducerROBEntry(phyReg1))
						> execEngine.getReorderBuffer().indexOf(reorderBufferEntry)*/)
				{
					return new boolean[]{true};
				}
				else
				{
					return new boolean[]{false};
				}
			}
			else
			{
				RenameTable tempRN;
				if(tempOpndType	== OperandType.integerRegister)
				{
					tempRN = execEngine.getIntegerRenameTable();
				}
				else
				{
					tempRN = execEngine.getFloatingPointRenameTable();
				}
				
				if(tempRN.getValueValid(phyReg1) == true ||
						tempRN.getProducerROBEntry(phyReg1) == reorderBufferEntry)
				{
					return new boolean[]{true};
				}
				else
				{
					return new boolean[]{false};
				}
			}
		}
		
		if(tempOpndType == OperandType.memory)
		{
			return new boolean[]
			 {OperandAvailabilityChecker.isAvailable(reorderBufferEntry, opnd.getMemoryLocationFirstOperand(), phyReg1, phyReg2, core)[0],
			  OperandAvailabilityChecker.isAvailable(reorderBufferEntry, opnd.getMemoryLocationSecondOperand(), phyReg2, phyReg1, core)[0]};
		}
		
		return new boolean[]{true};
	}

}