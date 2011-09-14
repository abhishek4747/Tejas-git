package emulatorinterface.translator.visaHandler;

import emulatorinterface.DynamicInstructionBuffer;
import generic.Instruction;

public class AcceleratedOp implements VisaHandler 
{
	public long handle(Instruction microOp,
			DynamicInstructionBuffer dynamicInstructionBuffer) 
	{
		return -1;
	}
}
