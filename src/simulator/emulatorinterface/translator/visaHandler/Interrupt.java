package emulatorinterface.translator.visaHandler;

import emulatorinterface.DynamicInstructionBuffer;
import generic.Instruction;
import generic.InstructionTable;

public class Interrupt implements VisaHandler 
{
	public int handle(int microOpIndex, InstructionTable instructionTable,
			Instruction microOp,
			DynamicInstructionBuffer dynamicInstructionBuffer) 
	{
		return ++microOpIndex;
	}
}
