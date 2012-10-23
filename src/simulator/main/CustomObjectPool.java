package main;

import emulatorinterface.RunnableThread;
import emulatorinterface.communication.IpcBase;
import generic.CustomInstructionPool;
import generic.CustomOperandPool;

public class CustomObjectPool {
	
	private static CustomOperandPool operandPool;
	private static CustomInstructionPool instructionPool;
	
	public static void initPool() {
		// Create Pools of Instructions, Operands and AddressCarryingEvents
		int numInstructionsInPool = RunnableThread.INSTRUCTION_THRESHOLD*
				IpcBase.getEmuThreadsPerJavaThread()*2;
		
		/* custom pool */
		System.out.println("creating operand pool..");
		setOperandPool(new CustomOperandPool(numInstructionsInPool *3));
		
		System.out.println("creating instruction pool..");
		setInstructionPool(new CustomInstructionPool(numInstructionsInPool));
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

}
