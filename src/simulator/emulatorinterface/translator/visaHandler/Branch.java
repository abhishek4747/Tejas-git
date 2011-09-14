package emulatorinterface.translator.visaHandler;

import emulatorinterface.DynamicInstructionBuffer;
import generic.BranchInstr;
import generic.Instruction;

public class Branch implements VisaHandler 
{
	public long handle(Instruction microOp,
			DynamicInstructionBuffer dynamicInstructionBuffer) 
	{
		BranchInstr branchInstruction;
		branchInstruction = dynamicInstructionBuffer.getBranchPacket(microOp.getProgramCounter()); 
		
		microOp.setBranchTaken(branchInstruction.branchTaken);
		microOp.setBranchTargetAddress(branchInstruction.branchAddress);
		
		if(branchInstruction.branchTaken == true)
		{
			return branchInstruction.branchAddress;
		}
		else
		{
			return microOp.getProgramCounter();
		}
	}
}
