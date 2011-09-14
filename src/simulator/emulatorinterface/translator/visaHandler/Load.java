package emulatorinterface.translator.visaHandler;

import java.util.LinkedList;


import emulatorinterface.DynamicInstructionBuffer;
import generic.Instruction;

public class Load implements VisaHandler 
{
	public long handle(Instruction microOp,
			DynamicInstructionBuffer dynamicInstructionBuffer) 
	{
		LinkedList<Long> memoryReadAddress;
		memoryReadAddress = dynamicInstructionBuffer.
				getmemoryReadAddress(microOp.getProgramCounter());
		
		microOp.getOperand1().setValue(memoryReadAddress.poll());
		
		return microOp.getProgramCounter();
	}
}
