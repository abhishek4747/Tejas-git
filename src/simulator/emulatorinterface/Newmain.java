package emulatorinterface;

import java.util.Enumeration;
import memorysystem.Cache;
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
import generic.InstructionLinkedList;
import generic.EventQueue;
import generic.Statistics;

public class Newmain {
	public static long start, end;
	public static long instructionCount = 0;public static int handled=0;
	public static int notHandled=0;
	public static Object syncObject = new Object();
	public static Time_t mainMemoryLatency;
	public static Process process;

	public static void main(String[] arguments) throws Exception 
	{
		String executableArguments=" ";
		String executableFile = " ";
		
		start = System.currentTimeMillis();
		
		// check command line arguments
		checkCommandLineArguments(arguments);

		// Read the command line arguments
		SimulationConfig.outputFileName = arguments[0];
		executableFile = arguments[1];
		for(int i=1; i < arguments.length; i++)
		{
			executableArguments = executableArguments + " " + arguments[i];
		}

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

		//create event queue
		EventQueue[] eventQ = new EventQueue[1];	//TODO number of queues = number of java threads
														//number of java threads to be specified/determinable from config file
		for(int i = 0; i < 1; i++)
		{
			eventQ[i] = new EventQueue();
		}
		
		//create cores
		Core[] cores = initCores(eventQ[0]);
		eventQ[0].setCoresHandled(cores);
		
		// create PIN interface
		IPCBase ipcBase = new SharedMem(eventQ, cores);
		Process process = createPINinterface(ipcBase, executableArguments,
				dynamicInstructionBuffer);
		
		//connect pipe between instruction translator and pipeline
		cores[0].setIncomingInstructionLists(new InstructionLinkedList[]{ipcBase.getReaderThreads()[0].getInputToPipeline()});
		
		if (cores[0].isPipelineStatistical)
			cores[0].getStatisticalPipeline().getFetcher().setInputToPipeline(cores[0].getIncomingInstructionLists());
		else
			cores[0].getExecEngine().getFetcher().setInputToPipeline(cores[0].getIncomingInstructionLists());
		
		//Create the memory system
		MemorySystem.initializeMemSys(cores, eventQ);
		
		//different core components may work at different frequencies
		GlobalClock.systemTimingSetUp(cores, MemorySystem.getCacheList());
		
		//set up statistics module
		Statistics.initStatistics();
		// Call these functions at last
		// returns the number of instructions. and waits on a semaphore for
		// finishing of reader threads
		//FIXME : wait stopped for unexpected exit.
		@SuppressWarnings("unused")
		long icount = ipcBase.doExpectedWaitForSelf();
		ipcBase.doWaitForPIN(process);
		ipcBase.finish();
		
		reportStatistics();
		
		//set memory statistics for levels L2 and below
		for (Enumeration<String> cacheNameSet = MemorySystem.getCacheList().keys(); cacheNameSet.hasMoreElements(); /*Nothing*/)
		{
			String cacheName = cacheNameSet.nextElement();
			Cache cache = MemorySystem.getCacheList().get(cacheName);
			
			Statistics.setNoOfL2Requests(cache.noOfRequests);
			Statistics.setNoOfL2Hits(cache.hits);
			Statistics.setNoOfL2Misses(cache.misses);
		}

		// returns the number of instructions. and waits on a semaphore for
		// finishing of reader threads
		long icount = ipcBase.doExpectedWaitForSelf();
		
		end = System.currentTimeMillis();
		Statistics.setTime(end - start);
		
		//print statistics
		Statistics.openStream();
		Statistics.printSystemConfig();
		Statistics.printTranslatorStatistics();
		Statistics.printTimingStatistics();
		Statistics.printMemorySystemStatistics();
		Statistics.printSimulationTime();
		Statistics.closeStream();
		
		System.exit(0);
		System.exit(0);
	}

	private static void reportStatistics() 
	{
		//calculate and report the static and dynamic coverage
		double staticCoverage;
		double dynamicCoverage;
		
		staticCoverage= (double)(ObjParser.staticHandled*100)/
					(double)(ObjParser.staticHandled+ObjParser.staticNotHandled);

		dynamicCoverage= (double)(ObjParser.dynamicHandled*100)/
					(double)(ObjParser.dynamicHandled+ObjParser.dynamicNotHandled);
		
		System.out.print("\n\tStatic coverage = " + staticCoverage + " %");
		System.out.print("\n\tDynamic coverage = " + dynamicCoverage + " %\n");
		
		Statistics.setStaticCoverage(staticCoverage);
		Statistics.setDynamicCoverage(dynamicCoverage);
	}

	// TODO Must provide parameters to make from here
	private static void configureEmulator() {

	}

	private static Process createPINinterface(IPCBase ipcBase,
			String executableArguments,
			DynamicInstructionBuffer dynamicInstructionBuffer) 
	{

		// Creating command for PIN tool.
		String cmd;
		
		cmd = SimulationConfig.PinTool + "/pin" + " -injection child -t " 
						+ SimulationConfig.PinInstrumentor + " -map "
						+ SimulationConfig.MapEmuCores + " -- ";
		cmd += executableArguments;


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
		if (arguments.length < 2) {
			Error.showErrorAndExit("\n\tIllegal number of arguments !!\nUsage java Newmain <output-file> <benchmark-program and arguments>");
		}
	}
	
	//TODO read a config file
	//create specified number of cores
	//map threads to cores
	static Core[] initCores(EventQueue eventQ)
	{
		System.out.println("initializing cores...");
		
		Core[] cores = new Core[]{
								new Core(0,
										eventQ,
										1,
										1,
										null,
										new int[]{0})};
		
		return cores;
	}

	/**
	 * @author Moksh
	 * For real-time printing of the running time, when program exited on request
	 */
	public static void printSimulationTime(long time)
	{
		//print time taken by simulator
		long seconds = time/1000;
		long minutes = seconds/60;
		seconds = seconds%60;
			System.out.println("\n");
			System.out.println("[Simulator Time]\n");
			
			System.out.println("Time Taken\t=\t" + minutes + " : " + seconds + " minutes");
			System.out.println("\n");
	}
}