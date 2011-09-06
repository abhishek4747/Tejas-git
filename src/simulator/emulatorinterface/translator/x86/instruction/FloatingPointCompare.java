package emulatorinterface.translator.x86.instruction;

import emulatorinterface.DynamicInstruction;
import emulatorinterface.translator.x86.registers.Registers;
import generic.Instruction;
import generic.InstructionList;
import generic.Operand;

public class FloatingPointCompare implements InstructionHandler 
{
	public InstructionList handle(Operand operand1, Operand operand2,
			Operand operand3, DynamicInstruction dynamicInstruction) 
	{
		InstructionList microOps = new InstructionList();
	
		if(operand1==null && operand2==null && operand3==null)
		{
			//Both the operands are implicit i.e st0 and st1
			Operand st0 = Registers.getTopFPRegister();
			Operand st1 = Registers.getSecondTopFPRegister();
			
			microOps.appendInstruction(Instruction.getFloatingPointALU(st0, st1, null));
		}
		
		else if(operand1.isFloatRegisterOperand() && operand2==null && operand3==null)
		{
			//First implicit operand is implicitly st0 and second operand is passed as argument.
			Operand st0 = Registers.getTopFPRegister();
			
			microOps.appendInstruction(Instruction.getFloatingPointALU(st0, operand1, null));
		}
		
		else if(operand1.isFloatRegisterOperand() && operand2.isFloatRegisterOperand() && operand3==null)
		{
			//Both the operands are passed as operands to the command.
			microOps.appendInstruction(Instruction.getFloatingPointALU(operand1, operand2, null));
		}
		
		//TODO Fix me : single-operand=memory
		else
		{
			microOps.appendInstruction(Instruction.getNOPInstruction());
		}
		
/*
 		else
 
		{
			misc.Error.invalidOperation("Floating Point Compare for ip=" 
					+ dynamicInstruction.getInstructionPointer()
					, operand1, operand2, operand3);
		}
*/
		
		return microOps;
	}
}
