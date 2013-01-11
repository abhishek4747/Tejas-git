package main;

import config.EmulatorConfig;
import emulatorinterface.RunnableThread;
import emulatorinterface.communication.CustomAsmCharPool;
import emulatorinterface.communication.IpcBase;
import generic.CustomInstructionPool;
import generic.CustomOperandPool;

public class CustomObjectPool {
	
	private static CustomOperandPool operandPool;
	private static CustomInstructionPool instructionPool;
	private static CustomAsmCharPool customAsmCharPool;
	
	public static void initCustomPools(int maxApplicationThreads, int staticInstructionPoolSize) {
		
		// Create Pools of Instructions, Operands and AddressCarryingEvents
		int runTimePool =  RunnableThread.INSTRUCTION_THRESHOLD * 3;// * maxApplicationThreads;
		int staticTimePool = staticInstructionPoolSize;
		
		int numInstructionsInPool = runTimePool + staticTimePool;
		
		/* custom pool */
		System.out.println("creating operand pool..");
		setOperandPool(new CustomOperandPool(numInstructionsInPool * 3));
		
		System.out.println("creating instruction pool..");
		setInstructionPool(new CustomInstructionPool(numInstructionsInPool));
		
		if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_QEMU) {
			System.out.println("creating custom asm-char pool. max threads = " + maxApplicationThreads);
			customAsmCharPool = new CustomAsmCharPool(maxApplicationThreads);
		}
	}

	public static CustomOperandPool getOperandPool() {
		return operandPool;
	}

	public static void setOperandPool(CustomOperandPool operandPool) {
		CustomObjectPool.operandPool = operandPool;
	}

	public static CustomInstructionPool getInstructionPool() {
		return instructionPool;
	}

	public static void setInstructionPool(CustomInstructionPool instructionPool) {
		CustomObjectPool.instructionPool = instructionPool;
	}

	public static CustomAsmCharPool getCustomAsmCharPool() {
		return customAsmCharPool;
	}

	public static void setCustomAsmCharPool(CustomAsmCharPool customAsmCharPool) {
		CustomObjectPool.customAsmCharPool = customAsmCharPool;
	}
}
