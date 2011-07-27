package emulatorinterface.translator.x86.instruction;

import emulatorinterface.DynamicInstruction;
import emulatorinterface.translator.x86.registers.Registers;
import generic.Instruction;
import generic.InstructionList;
import generic.Operand;

public class Leave implements InstructionHandler 
{
	public InstructionList handle(Operand operand1, Operand operand2,
			Operand operand3, DynamicInstruction dynamicInstruction) 
	{
		InstructionList microOps = new InstructionList();
		
		Error : case basis
		
		if(operand1==null && operand2==null && operand3==null)
		{
			Operand stackPointer = Registers.getStackPointer();
			Operand basePointer = Registers.getBasePointer();
			
			//stack-pointer=base-pointer
			microOps.appendInstruction(Instruction.getMoveInstruction(stackPointer, basePointer));
			
			//pop top of stack to base-pointer
			microOps.appendInstruction((new Pop()).handle(basePointer, null, null, dynamicInstruction));
		}
		
		else
		{
			misc.Error.invalidOperation("Leave", operand1, operand2, operand3);
		}
		
		return microOps;
	}
}
