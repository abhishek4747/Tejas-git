package pipeline;

import generic.GenericCircularQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.Operand;
import generic.Statistics;

import java.lang.reflect.Array;

import main.ArchitecturalComponent;
import main.Main;
import config.SimulationConfig;
import config.XMLParser;

public class PipelineTests {

	static String configFileName;
	static GenericCircularQueue<Instruction> inputToPipeline;
	static final int INSTRUCTION_THRESHOLD = 2000;

	public static void setUpBeforeClass(String configFile) {

		// Parse the command line arguments
		XMLParser.parse(configFile);

		// initialize object pools
		Main.initializeObjectPools();

		// initialize cores, memory, tokenBus
		ArchitecturalComponent.createChip();
		inputToPipeline = new GenericCircularQueue<Instruction>(
				Instruction.class, INSTRUCTION_THRESHOLD);
		GenericCircularQueue<Instruction>[] toBeSet = (GenericCircularQueue<Instruction>[]) Array
				.newInstance(GenericCircularQueue.class, 1);
		toBeSet[0] = inputToPipeline;
		ArchitecturalComponent.getCore(0).getPipelineInterface()
				.setInputToPipeline(toBeSet);
		ArchitecturalComponent.getCore(0).currentThreads = 1;
		ArchitecturalComponent.getCore(0).getExecEngine()
				.setExecutionBegun(true);

		// Initialize the statistics
		Statistics.initStatistics();
	}

	/*
	 * simulates a sequence of intALU instructions that have no data
	 * dependencies
	 */
	public static void minimumDataDependencies() {

		// generate instruction sequence
		Instruction newInst;
		int temp = 1;
		for (int i = 0; i < 100; i++) {
			temp++;
			if (temp % 16 == 0) {
				temp++;
			}

			newInst = Instruction.getIntALUInstruction(
					Operand.getIntegerRegister(0),
					Operand.getIntegerRegister(0),
					Operand.getIntegerRegister(temp % 16));
			newInst.setCISCProgramCounter(i);

			inputToPipeline.enqueue(newInst);
		}
		inputToPipeline.enqueue(Instruction.getInvalidInstruction());

		// simulate pipeline
		while (ArchitecturalComponent.getCores()[0].getPipelineInterface()
				.isExecutionComplete() == false) {
			ArchitecturalComponent.getCores()[0].getPipelineInterface()
					.oneCycleOperation();
			GlobalClock.incrementClock();
		}
	}

	/*
	 * simulates a sequence of intALU instructions, with (i+1)th instruction
	 * dependent on ith
	 */
	public static void maximumDataDependencies() {

		// generate instruction sequence
		Instruction newInst;
		for (int i = 0; i < 100; i++) {
			newInst = Instruction.getIntALUInstruction(
					Operand.getIntegerRegister(i % 16),
					Operand.getIntegerRegister(i % 16),
					Operand.getIntegerRegister((i + 1) % 16));

			inputToPipeline.enqueue(newInst);
		}
		inputToPipeline.enqueue(Instruction.getInvalidInstruction());

		// simulate pipeline
		while (ArchitecturalComponent.getCores()[0].getPipelineInterface()
				.isExecutionComplete() == false) {
			ArchitecturalComponent.getCores()[0].getPipelineInterface()
					.oneCycleOperation();
			GlobalClock.incrementClock();
		}
	}

	/*
	 * simulates a sequence of floatDiv instructions, with no data dependencies
	 */
	public static void structuralHazards() {

		// generate instruction sequence
		Instruction newInst;
		int temp = 1;
		for (int i = 0; i < 100; i++) {
			temp++;
			if (temp % 16 == 0) {
				temp++;
			}

			newInst = Instruction.getFloatingPointDivision(
					Operand.getIntegerRegister(0),
					Operand.getIntegerRegister(0),
					Operand.getIntegerRegister(temp % 16));

			inputToPipeline.enqueue(newInst);
		}
		inputToPipeline.enqueue(Instruction.getInvalidInstruction());

		// simulate pipeline
		while (ArchitecturalComponent.getCores()[0].getPipelineInterface()
				.isExecutionComplete() == false) {
			ArchitecturalComponent.getCores()[0].getPipelineInterface()
					.oneCycleOperation();
			GlobalClock.incrementClock();
		}
	}

	/*
	 * simulates a sequence of floatDiv instructions, all operating on R0, and
	 * writing to R0
	 */
	public static void renameTest() {

		// generate instruction sequence
		Instruction newInst;
		for (int i = 0; i < 100; i++) {
			newInst = Instruction.getFloatingPointDivision(
					Operand.getFloatRegister(0), Operand.getFloatRegister(0),
					Operand.getFloatRegister(0));

			inputToPipeline.enqueue(newInst);
		}
		inputToPipeline.enqueue(Instruction.getInvalidInstruction());

		// simulate pipeline
		while (ArchitecturalComponent.getCores()[0].getPipelineInterface()
				.isExecutionComplete() == false) {
			ArchitecturalComponent.getCores()[0].getPipelineInterface()
					.oneCycleOperation();
			GlobalClock.incrementClock();
		}
	}

	public static void immediateALUTest() {

		// generate instruction sequence
		Instruction newInst;
		for (int i = 0; i < 100; i++) {
			newInst = Instruction.getIntegerMultiplicationInstruction(
					Operand.getImmediateOperand(),
					Operand.getIntegerRegister(0),
					Operand.getIntegerRegister(1));
			newInst.setCISCProgramCounter(2 * i);
			inputToPipeline.enqueue(newInst);

			newInst = Instruction.getIntALUInstruction(
					Operand.getIntegerRegister(0),
					Operand.getImmediateOperand(),
					Operand.getIntegerRegister(1));
			newInst.setCISCProgramCounter(2 * i + 1);
			inputToPipeline.enqueue(newInst);
		}
		inputToPipeline.enqueue(Instruction.getInvalidInstruction());

		// simulate pipeline
		while (ArchitecturalComponent.getCores()[0].getPipelineInterface()
				.isExecutionComplete() == false) {
			ArchitecturalComponent.getCores()[0].getPipelineInterface()
					.oneCycleOperation();
			GlobalClock.incrementClock();
		}
	}

	public static void loadStoreTest() {
		Instruction inst;
		for (int i = 0; i < 100; i++) {
			inst = Instruction
					.getStoreInstruction(
							Operand.getMemoryOperand(
									Operand.getIntegerRegister(0),
									Operand.getIntegerRegister(1)),
							Operand.getFloatRegister(0));
			inputToPipeline.enqueue(inst);
			inst = Instruction
					.getLoadInstruction(
							Operand.getMemoryOperand(
									Operand.getIntegerRegister(0),
									Operand.getIntegerRegister(1)),
							Operand.getFloatRegister(0));
			inputToPipeline.enqueue(inst);
		}

		inputToPipeline.enqueue(Instruction.getInvalidInstruction());

		// simulate pipeline
		while (ArchitecturalComponent.getCores()[0].getPipelineInterface()
				.isExecutionComplete() == false) {
			ArchitecturalComponent.getCores()[0].getPipelineInterface()
					.oneCycleOperation();
			GlobalClock.incrementClock();
		}
	}

	public static void loadTest() {
		Instruction inst;
		for (int i = 0; i < 100; i++) {
			inst = Instruction
					.getLoadInstruction(
							Operand.getMemoryOperand(
									Operand.getIntegerRegister(0),
									Operand.getIntegerRegister(1)),
							Operand.getFloatRegister(0));
			inst.setCISCProgramCounter(i);
			inputToPipeline.enqueue(inst);
		}

		inputToPipeline.enqueue(Instruction.getInvalidInstruction());

		// simulate pipeline
		while (ArchitecturalComponent.getCores()[0].getPipelineInterface()
				.isExecutionComplete() == false) {
			ArchitecturalComponent.getCores()[0].getPipelineInterface()
					.oneCycleOperation();
			GlobalClock.incrementClock();
		}
	}

	public static void branchTest() {
		Instruction inst;
		int j = 0;
		for (int i = 0; i < 5; i++) {
			inst = Instruction
					.getStoreInstruction(
							Operand.getMemoryOperand(
									Operand.getIntegerRegister(0),
									Operand.getIntegerRegister(1)),
							Operand.getFloatRegister(0));
			inst.setCISCProgramCounter(j++);
			inputToPipeline.enqueue(inst);
		}

		inst = Instruction.getBranchInstruction(Operand.getImmediateOperand());
		inst.setCISCProgramCounter(j++);
		inst.setBranchTaken(false);
		inst.setBranchTargetAddress(2 * j);
		inputToPipeline.enqueue(inst);

		for (int i = j; i < j + 5; i++) {
			inst = Instruction
					.getLoadInstruction(
							Operand.getMemoryOperand(
									Operand.getIntegerRegister(0),
									Operand.getIntegerRegister(1)),
							Operand.getFloatRegister(0));
			inst.setCISCProgramCounter(i);
			inputToPipeline.enqueue(inst);
		}

		inputToPipeline.enqueue(Instruction.getInvalidInstruction());

		// simulate pipeline
		while (ArchitecturalComponent.getCores()[0].getPipelineInterface()
				.isExecutionComplete() == false) {
			ArchitecturalComponent.getCores()[0].getPipelineInterface()
					.oneCycleOperation();
			GlobalClock.incrementClock();
		}
	}

	public static void storeTest() {
		Instruction inst;
		for (int i = 0; i < 100; i++) {
			inst = Instruction
					.getStoreInstruction(
							Operand.getMemoryOperand(
									Operand.getIntegerRegister(0),
									Operand.getIntegerRegister(1)),
							Operand.getFloatRegister(0));
			inst.setCISCProgramCounter(i);
			inputToPipeline.enqueue(inst);
		}

		inputToPipeline.enqueue(Instruction.getInvalidInstruction());

		// simulate pipeline
		while (ArchitecturalComponent.getCores()[0].getPipelineInterface()
				.isExecutionComplete() == false) {
			ArchitecturalComponent.getCores()[0].getPipelineInterface()
					.oneCycleOperation();
			GlobalClock.incrementClock();
		}
	}

	public static void main(String[] arguments) {
		String configFile = arguments[0];
		int testType = Integer.parseInt(arguments[1]);
		String logfile = arguments[2];
		SimulationConfig.outputFileName = logfile;
		setUpBeforeClass(configFile);
		long startTime = System.currentTimeMillis();
		switch (testType) {
		case 0:
			minimumDataDependencies();
			break;

		case 1:
			maximumDataDependencies();
			break;

		case 2:
			structuralHazards();
			break;

		case 3:
			renameTest();
			break;

		case 4:
			loadTest();
			break;

		case 5:
			storeTest();
			break;

		case 6:
			loadStoreTest();
			break;

		case 7:
			immediateALUTest();
			break;

		case 8:
			branchTest();
			break;

		default:
			misc.Error.showErrorAndExit("unknown test type");
		}
		long endTime = System.currentTimeMillis();
		Statistics.printAllStatistics("Test "+(testType), startTime, endTime);

		System.out.println("\n\nTest completed !!");
		System.exit(0);
	}
}
