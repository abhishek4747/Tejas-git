package emulatorinterface.translator.visaHandler;

import config.EmulatorConfig;
import emulatorinterface.DynamicInstructionBuffer;
import generic.Instruction;
import generic.InstructionTable;

public class Branch implements DynamicInstructionHandler 
{
	public int handle(int microOpIndex, InstructionTable instructionTable,
			Instruction microOp, DynamicInstructionBuffer dynamicInstructionBuffer)
	{
		boolean branchTaken = dynamicInstructionBuffer.getBranchTaken(microOp.getCISCProgramCounter());
		long branchAddress = dynamicInstructionBuffer.getBranchAddress(microOp.getCISCProgramCounter());
		//BranchInstr branchInstruction;
		//branchInstruction = dynamicInstructionBuffer.getBranchPacket(microOp.getCISCProgramCounter()); 

		//if(branchInstruction != null)
		if(branchAddress!=-1) // branchAddress = -1 indicates there was no branch packet
		{
			microOp.setBranchTaken(branchTaken);
			microOp.setBranchTargetAddress(branchAddress);
			
			if(branchTaken == true)
			{
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
				if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_PIN) {
					return ++microOpIndex;
				} else if (EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_QEMU) {
					return -2; // conventiontion for qemu
				} else {
					return -1;
				}
			}
		}
		else
		{
			return -1;
		}
	}
}
