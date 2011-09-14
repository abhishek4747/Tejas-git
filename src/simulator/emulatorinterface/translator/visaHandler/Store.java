package emulatorinterface.translator.visaHandler;

import emulatorinterface.DynamicInstructionBuffer;
import generic.Instruction;
import generic.InstructionTable;

import java.util.LinkedList;

public class Store implements VisaHandler 
{
	public int handle(int microOpIndex, InstructionTable instructionTable,
			Instruction microOp, DynamicInstructionBuffer dynamicInstructionBuffer) 
	{
		LinkedList<Long> memoryWriteAddress;
		memoryWriteAddress = dynamicInstructionBuffer.
				getmemoryWriteAddress(microOp.getProgramCounter());
		
		microOp.getOperand1().setValue(memoryWriteAddress.poll());
		
		return ++microOpIndex;
	}
}