package emulatorinterface;

import pipeline.outoforder.BootPipelineEvent;
import memorysystem.MemorySystem;
import misc.Error;
import config.SimulationConfig;
import config.XMLParser;
import emulatorinterface.DynamicInstructionBuffer;
import emulatorinterface.communication.*;
import emulatorinterface.communication.shm.SharedMem;
import emulatorinterface.translator.x86.objparser.ObjParser;
import generic.Core;
import generic.GlobalClock;
import generic.InstructionList;
import generic.InstructionTable;
import generic.NewEventQueue;
import generic.Statistics;

public class Newmain {
	
	public static int handled=0;
	public static int notHandled=0;
	public static String executableFile;
	//public static Object syncObject = new Object();
	//public static Object syncObject2 = new Object();

	public static void main(String[] arguments) throws Exception 
	{
		// check command line arguments
		checkCommandLineArguments(arguments);

		// Read the command line arguments
		executableFile = arguments[0];

		// Parse the command line arguments
		XMLParser.parse();

		// Create a hash-table for the static representation of the executable
		InstructionTable instructionTable;
		instructionTable = ObjParser
				.buildStaticInstructionTable(executableFile);

		// Create a new dynamic instruction buffer
		DynamicInstructionBuffer dynamicInstructionBuffer = new DynamicInstructionBuffer();

		// configure the emulator
		configureEmulator();
		
		// create PIN interface
		IPCBase ipcBase = new SharedMem(instructionTable);
		Process process = createPINinterface(ipcBase, arguments,
				dynamicInstructionBuffer);

		
		
	
		
		
		//create event queue
		NewEventQueue eventQ = new NewEventQueue();
		
		//create cores
		Core[] cores = initCores(eventQ, ipcBase);
		
		//Create the memory system
		MemorySystem.initializeMemSys(cores);
		
		//different core components may work at different frequencies
		GlobalClock.systemTimingSetUp(cores, MemorySystem.getCacheList());
		
		//set up statistics module
		Statistics.initStatistics();
		
		
		
		//commence pipeline
		eventQ.addEvent(new BootPipelineEvent(cores, ipcBase, eventQ, 0));
		
		//Thread.sleep(10000);
		//System.out.println("finished sleeping..");
		//while(core.getExecEngine().isExecutionComplete() == false)
		while(eventQ.isEmpty() == false)
		{
			eventQ.processEvents();
			
			GlobalClock.incrementClock();
		}
		
		//synchronized(Newmain.syncObject2)
		{
			//Newmain.syncObject2.notify();
		}
		
		
		
		
		

		
		
		
		
		
		
		// returns the number of instructions. and waits on a semaphore for
		// finishing of reader threads
		long icount = ipcBase.doExpectedWaitForSelf();
		
		/*
		 
		//TODO currently simulating single core
		//number of cores to be read from configuration file
		Core core = new Core(dynamicInstructionBuffer, 0);
		//set up initial events in the queue
		eventQ.addEvent(new PerformDecodeEvent(0, core));
		eventQ.addEvent(new PerformCommitsEvent(0, core));


		*/

		// Call these functions at last
		ipcBase.doWaitForPIN(process);
		ipcBase.finish();
		
		// Display coverage
		double coverage = (double)(handled*100)/(double)(handled+notHandled);
		System.out.print("\n\tDynamic coverage =  " + coverage + " %\n");
		
		//print statistics
		Statistics.openStream();
		Statistics.printSystemConfig();
		Statistics.printTranslatorStatistics();
		Statistics.printTimingStatistics();
		Statistics.printMemorySystemStatistics();
		Statistics.closeStream();
	}

	// TODO Must provide parameters to make from here
	private static void configureEmulator() {

	}

	private static Process createPINinterface(IPCBase ipcBase,
			String[] arguments,
			DynamicInstructionBuffer dynamicInstructionBuffer) {

		// Creating command for PIN tool.
		String cmd = SimulationConfig.PinTool + "/pin" + " -injection child -t " 
		+ SimulationConfig.PinInstrumentor + " -map "
		+ SimulationConfig.MapEmuCores + " --";
		
		for(int i = 0; i < arguments.length; i++)
		{
			cmd += " ";
			cmd += arguments[i];
		}

		Process process = null;
		try {
			process = ipcBase.startPIN(cmd);
		} catch (Exception e) {
			misc.Error
					.showErrorAndExit("\n\tUnable to run the required program using PIN !!");
		}

		if (process == null)
			misc.Error
					.showErrorAndExit("\n\tCorrect path for pin or tool or executable not specified !!");

		ipcBase.createReaders(dynamicInstructionBuffer);

		return process;
	}

	// checks if the command line arguments are in required format and number
	private static void checkCommandLineArguments(String arguments[]) {
		if (arguments.length < 1) {
			Error.showErrorAndExit("\n\tIllegal number of arguments !!");
		}
	}
	
	//TODO read a config file
	//create specified number of cores
	//map threads to cores
	static Core[] initCores(NewEventQueue eventQ, IPCBase ipcBase)
	{
		System.out.println("initializing cores...");
		
		Core[] cores = new Core[]{
								new Core(0,
										eventQ,
										1,
										new InstructionList[]{ipcBase.getReaderThreads()[0].getInputToPipeline()},
										new int[]{0})};
		
		return cores;
	}
}