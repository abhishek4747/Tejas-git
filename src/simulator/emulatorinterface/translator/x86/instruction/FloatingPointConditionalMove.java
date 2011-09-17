package emulatorinterface.translator.x86.instruction;

import generic.InstructionLinkedList;
import generic.Operand;

public class FloatingPointConditionalMove implements InstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionLinkedList instructionLinkedList)
	{
		//TODO Must do something !!
		
		if(operand1.isFloatRegisterOperand() && operand2==null
				&& operand3==null)
		{
		
		}
		
		else if(operand1.isFloatRegisterOperand() && operand2.isFloatRegisterOperand()
				&& operand3==null)
		{
			
		}
		
		else
		{
			misc.Error.invalidOperation("Floating Point Conditional Move", operand1, operand2, operand3);
		}
	}
}
