package emulatorinterface.translator.x86.instruction;

import emulatorinterface.translator.x86.registers.Registers;
import generic.Instruction;
import generic.MicroOpsList;
import generic.Operand;

public class Leave implements InstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			MicroOpsList microOpsList)
	{
		if(operand1==null && operand2==null && operand3==null)
		{
			Operand stackPointer = Registers.getStackPointer();
			Operand basePointer = Registers.getBasePointer();
			
			//stack-pointer=base-pointer
			microOpsList.appendInstruction(Instruction.getMoveInstruction(stackPointer, basePointer));
			
			//pop top of stack to base-pointer
			(new Pop()).handle(instructionPointer, basePointer, null, null, microOpsList);
		}
		
		else
		{
			misc.Error.invalidOperation("Leave", operand1, operand2, operand3);
		}
	}
}