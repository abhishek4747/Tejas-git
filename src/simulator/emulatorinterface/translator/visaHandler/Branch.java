package emulatorinterface.translator.visaHandler;

import emulatorinterface.DynamicInstructionBuffer;
import generic.BranchInstr;
import generic.Instruction;
import generic.InstructionTable;

public class Branch implements VisaHandler 
{
	public int handle(int microOpIndex, InstructionTable instructionTable,
			Instruction microOp, DynamicInstructionBuffer dynamicInstructionBuffer)
	{
		BranchInstr branchInstruction;
		branchInstruction = dynamicInstructionBuffer.getBranchPacket(microOp.getProgramCounter()); 
		
		microOp.setBranchTaken(branchInstruction.branchTaken);
		microOp.setBranchTargetAddress(branchInstruction.branchAddress);
		
		if(branchInstruction.branchTaken == true)
		{
			return instructionTable.getMicroOpIndex(branchInstruction.branchAddress);
		}
		else
		{
			return ++microOpIndex;
		}
	}
}
