package emulatorinterface;

import java.util.Enumeration;

import power.Counters;

import memorysystem.nuca.CBDNuca;
import memorysystem.nuca.DNuca;
import memorysystem.nuca.NucaCache;

import memorysystem.nuca.SNuca;
import memorysystem.Cache;
import memorysystem.MemorySystem;
import misc.Error;
import net.optical.TopLevelTokenBus;
import config.SimulationConfig;
import config.SystemConfig;
import config.XMLParser;
import emulatorinterface.communication.*;
import emulatorinterface.communication.shm.SharedMem;
import emulatorinterface.translator.x86.objparser.ObjParser;
import generic.Core;
import generic.CoreBcastBus;
import generic.CustomInstructionPool;
import generic.CustomOperandPool;
import generic.GlobalClock;
import generic.Instruction;
import generic.Operand;
import generic.Statistics;
import java.util.*;
public class Newmain {
	public static long start, end;
	public static long instructionCount = 0;
	public static int handled=0;
	public static int notHandled=0;
	public static Object syncObject = new Object();
	public static Process process;
	//public static GenericObjectPool<Operand> operandPool;
	//public static GenericObjectPool<Instruction> instructionPool;
	public static CustomOperandPool operandPool;
	public static CustomInstructionPool instructionPool;

	// the reader threads. Each thread reads from EMUTHREADS
	public static RunnableThread [] runners = new RunnableThread[IpcBase.MaxNumJavaThreads];
	public static boolean subsetSimulation = true; //test added
	public static String executableArguments=" ";
	public static String executableFile = " ";
	
	public static void main(String[] arguments) throws Exception 
	{
//		String executableArguments=" ";
//		String executableFile = " ";
		
		start = System.currentTimeMillis();
		// check command line arguments
		checkCommandLineArguments(arguments);

		// Read the command line arguments
		String configFileName = arguments[0];
		SimulationConfig.outputFileName = arguments[1];
		executableFile = arguments[2];
		for(int i=2; i < arguments.length; i++)
		{
			executableArguments = executableArguments + " " + arguments[i];
		}

		// Parse the command line arguments
		XMLParser.parse(configFileName);

		// Create a hash-table for the static representation of the executable
		ObjParser.buildStaticInstructionTable(executableFile);
		
		// Create Pools of Instructions and Operands
		int numInstructionsInPool = RunnableThread.INSTRUCTION_THRESHOLD*IpcBase.EmuThreadsPerJavaThread*2;
		
		/* "apache pool"
		System.out.println("creating operand pool..");
		operandPool = new GenericObjectPool<Operand>(new PoolableOperandFactory());
		operandPool.setMaxActive(numInstructionsInPool * 3);
		operandPool.setMaxIdle(numInstructionsInPool * 3);
		for(int i = 0; i < numInstructionsInPool * 3; i++)
		{
			operandPool.addObject();
		}
		
		System.out.println("creating instruction pool..");
		instructionPool = new GenericObjectPool<Instruction>(new PoolableInstructionFactory());
		instructionPool.setMaxActive(numInstructionsInPool);
		instructionPool.setMaxIdle(numInstructionsInPool);
		for(int i = 0; i < numInstructionsInPool; i++)
		{
			instructionPool.addObject();
		}
		
		System.out.println("number of operands in pool = " + operandPool.getNumIdle());
		System.out.println("number of instructions in pool = " + instructionPool.getNumIdle());
		*/
		/* custom pool */
		System.out.println("creating operand pool..");
		operandPool = new CustomOperandPool(numInstructionsInPool *3);
		System.out.println("creating instruction pool..");
		instructionPool = new CustomInstructionPool(numInstructionsInPool);
		
		
/*		// Create a new dynamic instruction buffer
		DynamicInstructionBuffer dynamicInstructionBuffer = new DynamicInstructionBuffer();
*/
		// configure the emulator
		configureEmulator();

		
		//create cores
		CoreBcastBus coreBcastBus = new CoreBcastBus();

		Core[] cores = initCores(coreBcastBus);
		// create PIN interface
		IpcBase ipcBase = new SharedMem();
		if (SimulationConfig.Mode!=0) {
			Process process = createPINinterface(ipcBase, executableArguments);
		}

		//Create the memory system
		TopLevelTokenBus tokenBus = new TopLevelTokenBus();
		MemorySystem.initializeMemSys(cores,tokenBus); //TODO mem sys need not know eventQ during initialisation
		
		//different core components may work at different frequencies
		GlobalClock.systemTimingSetUp(cores);
		
		//Initialize counters
//		Counters powerCounters[] = new Counters[SystemConfig.NoOfCores];
//		for(int i=0;i<SystemConfig.NoOfCores;i++){
//			powerCounters[i] = new Counters();
//		}
		// Create runnable threads. Each thread reads from EMUTHREADS
		//FIXME A single java thread can have multiple cores
		
		String name;
		for (int i=0; i<IpcBase.MaxNumJavaThreads; i++){
			name = "thread"+Integer.toString(i);
			if(SimulationConfig.Mode==0)
				runners[i] = new RunnableFromFile(name,i, ipcBase, cores,tokenBus);
			else if (SimulationConfig.Mode==1)
				runners[i] = new RunnableShm(name,i, ipcBase, cores,tokenBus);
			else System.out.println("\n\n This mode not implemented yet \n\n");
		}
		
		//set up statistics module
		Statistics.initStatistics();
		Statistics.setExecutable(executableFile);
		// Call these functions at last
		// returns the number of instructions. and waits on a semaphore for
		// finishing of reader threads
		//FIXME : wait stopped for unexpected exit.
		@SuppressWarnings("unused")
		long icount = ipcBase.doExpectedWaitForSelf();
		if (SimulationConfig.Mode!=0) ipcBase.doWaitForPIN(process);
		ipcBase.finish();
		reportStatistics();
		
		//set memory statistics for levels L2 and below
		for (Enumeration<String> cacheNameSet = MemorySystem.getCacheList().keys(); cacheNameSet.hasMoreElements(); /*Nothing*/)
		{
			String cacheName = cacheNameSet.nextElement();
			Cache cache = MemorySystem.getCacheList().get(cacheName);
			
			if (cache.getClass() == SNuca.class)
			{
				((NucaCache)cache).setStatistics();
				Statistics.nocTopology = ((NucaCache)cache).cacheBank[0][0].getRouter().topology.name();
				Statistics.nocRoutingAlgo = ((NucaCache)cache).cacheBank[0][0].getRouter().rAlgo.name();
				for(int i=0;i< ((NucaCache)cache).cacheRows;i++)
				{
					for(int j=0; j< ((NucaCache)cache).cacheColumns;j++)
					{
						Statistics.hopcount += ((NucaCache)cache).cacheBank[i][j].getRouter().hopCounters; 
					}
				}
				if(Statistics.nocTopology.equals("FATTREE") ||
						Statistics.nocTopology.equals("OMEGA") ||
						Statistics.nocTopology.equals("BUTTERFLY")) {
					for(int k = 0 ; k<((NucaCache)cache).noc.intermediateSwitch.size();k++){
						Statistics.hopcount += ((NucaCache)cache).noc.intermediateSwitch.get(k).hopCounters;
					}
				}
			}
			else if (cache.getClass() == DNuca.class)
			{
				((NucaCache)cache).setStatistics();
				Statistics.nocTopology = ((NucaCache)cache).cacheBank[0][0].getRouter().topology.name();
				Statistics.nocRoutingAlgo = ((NucaCache)cache).cacheBank[0][0].getRouter().rAlgo.name();
				for(int i=0;i< ((NucaCache)cache).cacheRows;i++)
				{
					for(int j=0; j< ((NucaCache)cache).cacheColumns;j++)
					{
						Statistics.hopcount += ((NucaCache)cache).cacheBank[i][j].getRouter().hopCounters; 
					}
				}
				if(Statistics.nocTopology.equals("FATTREE") ||
						Statistics.nocTopology.equals("OMEGA") ||
						Statistics.nocTopology.equals("BUTTERFLY")) {
					for(int k = 0 ; k<((NucaCache)cache).noc.intermediateSwitch.size();k++){
						Statistics.hopcount += ((NucaCache)cache).noc.intermediateSwitch.get(k).hopCounters;
					}
				}
			}
			else if (cache.getClass() == CBDNuca.class)
			{
				((NucaCache)cache).setStatistics();
				Statistics.nocTopology = ((NucaCache)cache).cacheBank[0][0].getRouter().topology.name();
				Statistics.nocRoutingAlgo = ((NucaCache)cache).cacheBank[0][0].getRouter().rAlgo.name();
				for(int i=0;i< ((NucaCache)cache).cacheRows;i++)
				{
					for(int j=0; j< ((NucaCache)cache).cacheColumns;j++)
					{
						Statistics.hopcount += ((NucaCache)cache).cacheBank[i][j].getRouter().hopCounters; 
					}
				}
				if(Statistics.nocTopology.equals("FATTREE") ||
						Statistics.nocTopology.equals("OMEGA") ||
						Statistics.nocTopology.equals("BUTTERFLY")) {
					for(int k = 0 ; k<((NucaCache)cache).noc.intermediateSwitch.size();k++){
						Statistics.hopcount += ((NucaCache)cache).noc.intermediateSwitch.get(k).hopCounters;
					}
				}
			}
			Statistics.setNoOfL2Requests(cache.noOfRequests);
			Statistics.setNoOfL2Hits(cache.hits);
			Statistics.setNoOfL2Misses(cache.misses);
			
		}
			

		end = System.currentTimeMillis();
		Statistics.setTime(end - start);
		
		//print statistics
		Statistics.openStream();
		Statistics.printSystemConfig();
		Statistics.printTranslatorStatistics();
		Statistics.printTimingStatistics();
		Statistics.printMemorySystemStatistics();
		Statistics.printSimulationTime();
		Statistics.printPowerStats();
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

	private static Process createPINinterface(IpcBase ipcBase,
			String executableArguments) 
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

		ipcBase.initIpc();
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
	static Core[] initCores(CoreBcastBus coreBBus)
	{
		System.out.println("initializing cores...");
		System.out.println("Initializing core broadcast bus...");
		
		Core[] cores = new Core[IpcBase.EmuThreadsPerJavaThread];
		for (int i=0; i<IpcBase.EmuThreadsPerJavaThread; i++) {
			cores[i] = new Core(i,
							1,
							1,
							null,
							new int[]{0});
			cores[i].setCoreBcastBus(coreBBus);
		}
		
		coreBBus.setEventQueue(cores[0].eventQueue);
		
		for(int i=0 ; i < cores.length ; i++){
			coreBBus.addToCoreList(cores[i]);
		}
		
		//TODO wont work in case of multiple runnable threads
		for(int i = SystemConfig.NoOfCores; i<IpcBase.EmuThreadsPerJavaThread; i++)
		{
			if (cores[i].isPipelineStatistical)
				cores[i].getStatisticalPipeline().setExecutionComplete(true);
			else if (cores[i].isPipelineInorder)
				cores[i].getExecutionEngineIn().setExecutionComplete(true);
			else if (cores[i].isPipelineMultiIssueInorder)
				cores[i].getExecutionEngineIn().setExecutionComplete(true);
			else
				cores[i].getExecEngine().setExecutionComplete(true);
		}
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