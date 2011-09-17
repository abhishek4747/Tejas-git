package emulatorinterface.translator.x86.instruction;

import generic.Instruction;
import generic.InstructionLinkedList;
import generic.Operand;

public class ConditionalSet implements InstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionLinkedList instructionLinkedList)
	{
		if((operand1.isIntegerRegisterOperand() || operand1.isMachineSpecificRegisterOperand()) && 
				operand2==null && operand3==null)
		{
			instructionLinkedList.appendInstruction(Instruction.getMoveInstruction(operand1, 
					Operand.getImmediateOperand()));
		}
		
		else if(operand1.isMemoryOperand())
		{
			instructionLinkedList.appendInstruction(Instruction.getStoreInstruction(operand1, 
					Operand.getImmediateOperand()));
		}
		
		else
		{
			misc.Error.invalidOperation("Conditional Set", operand1, operand2, operand3);
		}
	}
}