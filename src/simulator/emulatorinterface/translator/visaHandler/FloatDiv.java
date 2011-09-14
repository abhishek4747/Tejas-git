package emulatorinterface.translator.visaHandler;

import emulatorinterface.DynamicInstructionBuffer;
import generic.Instruction;

public class FloatDiv implements VisaHandler 
{
	public long handle(Instruction microOp,
			DynamicInstructionBuffer dynamicInstructionBuffer) 
	{
		//nothing to be done in such cases
		return microOp.getProgramCounter();
	}
}
