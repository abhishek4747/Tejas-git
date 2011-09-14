package emulatorinterface.translator.visaHandler;

import java.util.LinkedList;


import emulatorinterface.DynamicInstructionBuffer;
import generic.Instruction;
import generic.InstructionTable;

public class Load implements VisaHandler 
{
	public int handle(int microOpIndex, InstructionTable instructionTable,
			Instruction microOp, DynamicInstructionBuffer dynamicInstructionBuffer) 
	{
		LinkedList<Long> memoryReadAddress;
		memoryReadAddress = dynamicInstructionBuffer.
				getmemoryReadAddress(microOp.getProgramCounter());
		
		microOp.getOperand1().setValue(memoryReadAddress.poll());
		
		return ++microOpIndex;
	}
}