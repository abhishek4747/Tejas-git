package emulatorinterface.translator.x86.instruction;

import emulatorinterface.translator.x86.registers.Registers;
import generic.Instruction;
import generic.Operand;
import generic.InstructionArrayList;

public class FloatingPointStoreControlWord implements InstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionArrayList instructionArrayList)
	{
		if(operand1.isMemoryOperand() && 
				operand2==null && operand3==null)
		{
			Operand floatingPointControlWord;
			floatingPointControlWord=Registers.getFloatingPointControlWord();
			
			// control-word to memory
			instructionArrayList.appendInstruction(Instruction.getStoreInstruction(operand1,
					floatingPointControlWord));
		}
		
		else
		{
			misc.Error.invalidOperation("Floating Point Store Control Word", operand1, operand2, operand3);
		}
	}
}