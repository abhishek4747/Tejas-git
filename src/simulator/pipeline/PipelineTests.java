package pipeline;

import java.lang.reflect.Array;
import main.ArchitecturalComponent;
import main.Main;
import config.XMLParser;
import generic.GenericCircularQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.Operand;
import generic.Statistics;

public class PipelineTests {
	
	static String configFileName;
	static GenericCircularQueue<Instruction> inputToPipeline;
	static final int INSTRUCTION_THRESHOLD = 2000;

	public static void setUpBeforeClass() {
		
		// Parse the command line arguments
		XMLParser.parse("/home/raj/workspace2/Tejas/src/simulator/config/config.xml");
		
		//initialize object pools
		Main.initializeObjectPools();
		
		// initialize cores, memory, tokenBus
		Main.initializeArchitecturalComponents();		
		inputToPipeline = new GenericCircularQueue<Instruction>(
											Instruction.class, INSTRUCTION_THRESHOLD);
		GenericCircularQueue<Instruction>[] toBeSet = (GenericCircularQueue<Instruction>[])
														Array.newInstance(GenericCircularQueue.class, 1);
		toBeSet[0] = inputToPipeline;
		ArchitecturalComponent.getCores()[0].getPipelineInterface().setInputToPipeline(toBeSet);
		ArchitecturalComponent.getCores()[0].currentThreads = 1;
		
		// Initialize the statistics
		Statistics.initStatistics();
	}

	/*
	 * simulates a sequence of intALU instructions that have no data dependencies
	 */
	public static void minimumDataDependencies() {
		
		setUpBeforeClass();
		
		//generate instruction sequence
		Instruction newInst;
		int temp = 1;
		for(int i = 0; i < 100; i++)
		{
			temp++;
			if(temp%16 == 0)
			{
				temp++;
			}
			
			newInst = Instruction.getIntALUInstruction(
										Operand.getIntegerRegister(0),
										Operand.getIntegerRegister(0),
										Operand.getIntegerRegister(temp%16));
			
			inputToPipeline.enqueue(newInst);
		}		
		inputToPipeline.enqueue(Instruction.getInvalidInstruction());
		
		//simulate pipeline
		while(ArchitecturalComponent.getCores()[0].getPipelineInterface().isExecutionComplete() == false)
		{
			ArchitecturalComponent.getCores()[0].getPipelineInterface().oneCycleOperation();
			GlobalClock.incrementClock();
		}
	}

	/*
	 * simulates a sequence of intALU instructions, with (i+1)th instruction dependent on ith
	 */
	public static void maximumDataDependencies() {
		
		setUpBeforeClass();
		
		//generate instruction sequence
		Instruction newInst;
		for(int i = 0; i < 100; i++)
		{
			newInst = Instruction.getIntALUInstruction(
										Operand.getIntegerRegister(i%16),
										Operand.getIntegerRegister(i%16),
										Operand.getIntegerRegister((i+1)%16));
			
			inputToPipeline.enqueue(newInst);
		}		
		inputToPipeline.enqueue(Instruction.getInvalidInstruction());
		
		//simulate pipeline
		while(ArchitecturalComponent.getCores()[0].getPipelineInterface().isExecutionComplete() == false)
		{
			ArchitecturalComponent.getCores()[0].getPipelineInterface().oneCycleOperation();
			GlobalClock.incrementClock();
		}
	}

	/*
	 * simulates a sequence of floatDiv instructions, with no data dependencies
	 */
	public static void structuralHazards() {
		
		setUpBeforeClass();
		
		//generate instruction sequence
		Instruction newInst;
		int temp = 1;
		for(int i = 0; i < 100; i++)
		{
			temp++;
			if(temp%16 == 0)
			{
				temp++;
			}
			
			newInst = Instruction.getFloatingPointDivision(
										Operand.getIntegerRegister(0),
										Operand.getIntegerRegister(0),
										Operand.getIntegerRegister(temp%16));
			
			inputToPipeline.enqueue(newInst);
		}		
		inputToPipeline.enqueue(Instruction.getInvalidInstruction());
		
		//simulate pipeline
		while(ArchitecturalComponent.getCores()[0].getPipelineInterface().isExecutionComplete() == false)
		{
			ArchitecturalComponent.getCores()[0].getPipelineInterface().oneCycleOperation();
			GlobalClock.incrementClock();
		}
	}

	/*
	 * simulates a sequence of floatDiv instructions, all operating on R0, and writing to R0
	 */
	public static void renameTest() {
		
		setUpBeforeClass();
		
		//generate instruction sequence
		Instruction newInst;
		for(int i = 0; i < 100; i++)
		{
			newInst = Instruction.getFloatingPointDivision(
										Operand.getIntegerRegister(0),
										Operand.getIntegerRegister(0),
										Operand.getIntegerRegister(0));
			
			inputToPipeline.enqueue(newInst);
		}		
		inputToPipeline.enqueue(Instruction.getInvalidInstruction());
		
		//simulate pipeline
		while(ArchitecturalComponent.getCores()[0].getPipelineInterface().isExecutionComplete() == false)
		{
			ArchitecturalComponent.getCores()[0].getPipelineInterface().oneCycleOperation();
			GlobalClock.incrementClock();
		}
	}
	
	public static void main(String[] arguments)
	{
		int testType = Integer.parseInt(arguments[0]);
		
		switch(testType)
		{
			case 0 :	minimumDataDependencies();
						break;
						
			case 1 :	maximumDataDependencies();
						break;
						
			case 2 :	structuralHazards();
						break;
						
			case 3 :	renameTest();
						break;
			
			default :	misc.Error.showErrorAndExit("unknown test type");
		}
	}

}
