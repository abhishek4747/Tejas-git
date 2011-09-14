package emulatorinterface.translator.visaHandler;

import emulatorinterface.DynamicInstructionBuffer;
import generic.Instruction;

public interface VisaHandler 
{
	// This function will return the next instruction.
	// It takes an Instruction and a DynamicInstructionBuffer, changes microOp appropriately.
	// It will raise an error and terminate if it is not able to get an expected value, from
	// dynamicInstructionBuffer.
	public long handle(Instruction microOp, DynamicInstructionBuffer dynamicInstructionBuffer);
}
