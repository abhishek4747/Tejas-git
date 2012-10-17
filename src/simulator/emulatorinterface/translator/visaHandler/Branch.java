package emulatorinterface.translator.visaHandler;

import config.EmulatorConfig;
import emulatorinterface.DynamicInstructionBuffer;
import generic.BranchInstr;
import generic.Instruction;
import generic.InstructionTable;

public class Branch implements DynamicInstructionHandler 
{
	public int handle(int microOpIndex, InstructionTable instructionTable,
			Instruction microOp, DynamicInstructionBuffer dynamicInstructionBuffer)
	{
		BranchInstr branchInstruction;
		branchInstruction = dynamicInstructionBuffer.getBranchPacket(microOp.getCISCProgramCounter()); 

		if(branchInstruction != null)
		{
			microOp.setBranchTaken(branchInstruction.branchTaken);
			microOp.setBranchTargetAddress(branchInstruction.branchAddress);
			
			if(branchInstruction.branchTaken == true)
			{
				if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_PIN) {
					return instructionTable.getMicroOpIndex(branchInstruction.branchAddress);
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
