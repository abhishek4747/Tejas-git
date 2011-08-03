package pipeline.outoforder;

import generic.Core;
import generic.Operand;
import generic.OperandType;

public class OperandAvailabilityChecker {
	
	public static boolean[] isAvailable(Operand opnd, int phyReg1, int phyReg2, Core core)
	//phyReg2 required because if OperandType is memory, 2 physical registers may have to be specified
	{
		if(opnd == null)
		{
			return new boolean[]{true};
		}
		
		OperandType tempOpndType = opnd.getOperandType();
		
		if(tempOpndType == OperandType.immediate ||
				tempOpndType == OperandType.inValid)
		{
			return new boolean[]{true};
		}
		
		if(tempOpndType == OperandType.integerRegister ||
				tempOpndType == OperandType.floatRegister ||
				tempOpndType == OperandType.machineSpecificRegister)
		{
			if(tempOpndType == OperandType.machineSpecificRegister)
			{
				RegisterFile tempRF = core.getExecEngine().getMachineSpecificRegisterFile();
				if(tempRF.getValueValid(phyReg1) == true)
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
					tempRN = core.getExecEngine().getIntegerRenameTable();
				}
				else
				{
					tempRN = core.getExecEngine().getFloatingPointRenameTable();
				}
				
				if(tempRN.getValueValid(phyReg1) == true)
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
			 {OperandAvailabilityChecker.isAvailable(opnd.getMemoryLocationFirstOperand(), phyReg1, phyReg2, core)[0],
			  OperandAvailabilityChecker.isAvailable(opnd.getMemoryLocationSecondOperand(), phyReg1, phyReg2, core)[0]};
		}
		
		return new boolean[]{true};
	}

}
