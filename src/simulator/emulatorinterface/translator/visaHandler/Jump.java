package emulatorinterface.translator.visaHandler;

import emulatorinterface.DynamicInstructionBuffer;
import generic.BranchInstr;
import generic.Instruction;
import generic.InstructionTable;

public class Jump implements VisaHandler 
{
	public int handle(int microOpIndex, InstructionTable instructionTable,
			Instruction microOp, DynamicInstructionBuffer dynamicInstructionBuffer) 
	{
		BranchInstr branchInstruction;
		branchInstruction = dynamicInstructionBuffer.getBranchPacket(microOp.getProgramCounter()); 
		
		microOp.setBranchTaken(true);
		microOp.setBranchTargetAddress(branchInstruction.branchAddress);
		
		return instructionTable.getMicroOpIndex(branchInstruction.branchAddress);
	}
}