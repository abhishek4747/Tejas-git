package emulatorinterface.translator.x86.instruction;

import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.x86.registers.Registers;
import emulatorinterface.translator.x86.registers.TempRegister;
import generic.Instruction;
import generic.Operand;
import generic.InstructionArrayList;

public class FloatingPointLoadControlWord implements InstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionArrayList instructionArrayList,
			TempRegister tempRegisterNum)
					throws InvalidInstructionException
	{
		if(operand1.isMemoryOperand() && 
				operand2==null && operand3==null)
		{
			Operand floatingPointControlWord;
			floatingPointControlWord=Registers.getFloatingPointControlWord();
			
			// memory to control-word
			instructionArrayList.appendInstruction(Instruction.getLoadInstruction(operand1,
					floatingPointControlWord));
		}
		
		else
		{
			misc.Error.invalidOperation("Floating Point Load Control Word", operand1, operand2, operand3);
		}
	}
}
