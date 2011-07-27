package emulatorinterface.translator.x86.instruction;

import emulatorinterface.DynamicInstruction;
import emulatorinterface.translator.x86.registers.Registers;
import generic.InstructionList;
import generic.Operand;

public class FloatingPointConditionalMove implements InstructionHandler 
{
	public InstructionList handle(Operand operand1, Operand operand2,
			Operand operand3, DynamicInstruction dynamicInstruction) 
	{
		Error : handled on inspection
		
		InstructionList microOps = new InstructionList();
	
		if(operand1.isFloatRegisterOperand() && operand2==null
				&& operand3==null)
		{
			Operand st0 = Registers.getTopFPRegister();
		
		}
		
		else if(operand1.isFloatRegisterOperand() && operand2.isFloatRegisterOperand()
				&& operand3==null)
		{
			
		}
		
		else
		{
			misc.Error.invalidOperation("Floating Point Conditional Move", operand1, operand2, operand3);
		}
		return null;
	}
}
