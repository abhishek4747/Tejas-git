package pipeline;

import static org.junit.Assert.*;

import java.lang.reflect.Array;

import main.ArchitecturalComponent;
import main.CustomObjectPool;
import main.Main;

import org.junit.BeforeClass;
import org.junit.Test;

import config.EmulatorConfig;
import config.SimulationConfig;
import config.XMLParser;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.translator.x86.objparser.ObjParser;
import generic.GenericCircularQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;
import generic.Statistics;

public class PipelineTests {
	
	static String configFileName;
	static GenericCircularQueue<Instruction> inputToPipeline;
	static final int INSTRUCTION_THRESHOLD = 2000;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
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
	@Test
	public void minimumDataDependencies() {
		
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
	@Test
	public void maximumDataDependencies() {
		
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
	@Test
	public void structuralHazards() {
		
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

}
