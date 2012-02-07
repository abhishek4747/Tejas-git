package emulatorinterface.translator.x86.instruction;

import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.x86.registers.Registers;
import generic.Instruction;
import generic.Operand;
import generic.InstructionArrayList;

public class Leave implements InstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionArrayList instructionArrayList) 
					throws InvalidInstructionException
	{
		if(operand1==null && operand2==null && operand3==null)
		{
			Operand stackPointer = Registers.getStackPointer();
			Operand basePointer = Registers.getBasePointer();
			
			//stack-pointer=base-pointer
			instructionArrayList.appendInstruction(Instruction.getMoveInstruction(stackPointer, basePointer));
			
			//pop top of stack to base-pointer
			(new Pop()).handle(instructionPointer, basePointer, null, null, instructionArrayList);
		}
		
		else
		{
			misc.Error.invalidOperation("Leave", operand1, operand2, operand3);
		}
	}
}