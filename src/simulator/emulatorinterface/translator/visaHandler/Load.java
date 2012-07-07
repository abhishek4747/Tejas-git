package emulatorinterface.translator.visaHandler;

import emulatorinterface.DynamicInstructionBuffer;
import generic.Instruction;
import generic.InstructionTable;

public class Load implements VisaHandler 
{
	public int handle(int microOpIndex, InstructionTable instructionTable,
			Instruction microOp, DynamicInstructionBuffer dynamicInstructionBuffer) 
	{
		long memoryReadAddress;
		memoryReadAddress = dynamicInstructionBuffer.
				getSingleLoadAddress(microOp.getProgramCounter());
		
		if(memoryReadAddress != -1)
		{
			microOp.getOperand1().setValue(memoryReadAddress);
			return ++microOpIndex;
		}
		else
		{
			return -1;
		}
	}
}