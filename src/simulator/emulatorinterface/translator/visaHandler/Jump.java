package emulatorinterface.translator.visaHandler;

import emulatorinterface.DynamicInstructionBuffer;
import generic.BranchInstr;
import generic.Instruction;

public class Jump implements VisaHandler 
{
	public long handle(Instruction microOp,
			DynamicInstructionBuffer dynamicInstructionBuffer) 
	{
		BranchInstr branchInstruction;
		branchInstruction = dynamicInstructionBuffer.getBranchPacket(microOp.getProgramCounter()); 
		
		microOp.setBranchTaken(true);
		microOp.setBranchTargetAddress(branchInstruction.branchAddress);
		
		return branchInstruction.branchAddress;
	}
}