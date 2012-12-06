package emulatorinterface.translator.visaHandler;

import config.EmulatorConfig;
import emulatorinterface.DynamicInstructionBuffer;
import generic.BranchInstr;
import generic.Instruction;
import generic.InstructionTable;

public class Jump implements DynamicInstructionHandler 
{
	public int handle(int microOpIndex, InstructionTable instructionTable,
			Instruction microOp, DynamicInstructionBuffer dynamicInstructionBuffer) 
	{
		//BranchInstr branchInstruction;
		//branchInstruction = dynamicInstructionBuffer.getBranchPacket(microOp.getCISCProgramCounter());
		boolean branchTaken = dynamicInstructionBuffer.getBranchTaken(microOp.getCISCProgramCounter());
		long branchAddress = dynamicInstructionBuffer.getBranchAddress(microOp.getCISCProgramCounter());
		
		//if(branchInstruction != null)
		if(branchAddress != -1)
		{
			microOp.setBranchTaken(true);
			microOp.setBranchTargetAddress(branchAddress);
			
			if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_PIN) {
				return instructionTable.getMicroOpIndex(branchAddress);
			} else if (EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_QEMU) {
				return -2; // conventiontion for qemu
			} else {
				return -1;
			}
		}
		else
		{
			return -1;
		}
		
	}
}