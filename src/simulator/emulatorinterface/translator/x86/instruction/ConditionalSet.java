package emulatorinterface.translator.x86.instruction;

import generic.Instruction;
import generic.MicroOpsList;
import generic.Operand;

public class ConditionalSet implements InstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			MicroOpsList microOpsList)
	{
		if((operand1.isIntegerRegisterOperand() || operand1.isMachineSpecificRegisterOperand()) && 
				operand2==null && operand3==null)
		{
			microOpsList.appendInstruction(Instruction.getMoveInstruction(operand1, 
					Operand.getImmediateOperand()));
		}
		
		else if(operand1.isMemoryOperand())
		{
			microOpsList.appendInstruction(Instruction.getStoreInstruction(operand2, 
					Operand.getImmediateOperand()));
		}
		
		else
		{
			misc.Error.invalidOperation("Conditional Set", operand1, operand2, operand3);
		}
	}
}