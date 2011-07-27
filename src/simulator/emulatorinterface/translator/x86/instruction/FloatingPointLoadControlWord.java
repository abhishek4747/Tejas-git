package emulatorinterface.translator.x86.instruction;

import emulatorinterface.DynamicInstruction;
import emulatorinterface.translator.x86.registers.Registers;
import generic.Instruction;
import generic.InstructionList;
import generic.Operand;

public class FloatingPointLoadControlWord implements InstructionHandler 
{
	public InstructionList handle(Operand operand1, Operand operand2,
			Operand operand3, DynamicInstruction dynamicInstruction) 
	{
		InstructionList microOps = new InstructionList();
		
		Error : case basis
		
		if(operand1.isMemoryOperand() && 
				operand2==null && operand3==null)
		{
			Operand floatingPointControlWord;
			floatingPointControlWord=Registers.getFloatingPointControlWord();
			
			// memory to control-word
			operand1.setMemoryAddress(dynamicInstruction.getMemoryReadAddress().firstElement());
			microOps.appendInstruction(Instruction.getLoadInstruction(operand1,
					floatingPointControlWord));
		}
		else
		{
			misc.Error.invalidOperation("Floating Point Load Control Word", operand1, operand2, operand3);
		}
		
		return microOps;
	}
}
