package emulatorinterface.translator.x86.instruction;

import emulatorinterface.DynamicInstruction;
import generic.InstructionList;
import generic.Operand;

public class ConditionalSet implements InstructionHandler 
{
	public InstructionList handle(Operand operand1, Operand operand2,
			Operand operand3, DynamicInstruction dynamicInstruction) 
	{
		InstructionList microOps = new InstructionList();

		if((operand1.isIntegerRegisterOperand() || operand1.isMachineSpecificRegisterOperand()) &&
				operand2==null && operand3==null)
		{
			Error : Case basis
		}
		else
		{
			misc.Error.invalidOperation("Conditional Set", operand1, operand2, operand3);
		}
		
		return microOps;
	}
}
