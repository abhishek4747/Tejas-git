package emulatorinterface.translator.x86.instruction;

import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.x86.registers.TempRegister;
import generic.Instruction;
import generic.Operand;
import generic.InstructionArrayList;

public class ConditionalSet implements InstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionArrayList instructionArrayList,
			TempRegister tempRegisterNum)
					throws InvalidInstructionException
	{
		if((operand1.isIntegerRegisterOperand() || operand1.isMachineSpecificRegisterOperand()) && 
				operand2==null && operand3==null)
		{
			instructionArrayList.appendInstruction(Instruction.getMoveInstruction(operand1, 
					Operand.getImmediateOperand()));
		}
		
		else if(operand1.isMemoryOperand())
		{
			instructionArrayList.appendInstruction(Instruction.getStoreInstruction(operand1, 
					Operand.getImmediateOperand()));
		}
		
		else
		{
			misc.Error.invalidOperation("Conditional Set", operand1, operand2, operand3);
		}
	}
}