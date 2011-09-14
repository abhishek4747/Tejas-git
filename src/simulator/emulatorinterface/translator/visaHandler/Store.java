package emulatorinterface.translator.visaHandler;

import emulatorinterface.DynamicInstructionBuffer;
import generic.Instruction;

import java.util.LinkedList;

public class Store implements VisaHandler 
{
	public long handle(Instruction microOp,
			DynamicInstructionBuffer dynamicInstructionBuffer) 
	{
		LinkedList<Long> memoryWriteAddress;
		memoryWriteAddress = dynamicInstructionBuffer.
				getmemoryWriteAddress(microOp.getProgramCounter());
		
		microOp.getOperand1().setValue(memoryWriteAddress.poll());
		
		return microOp.getProgramCounter();
	}
}