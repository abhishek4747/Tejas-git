package main;


import java.io.File;
import misc.Error;
import config.SimulationConfig;
import config.XMLParser;
import emulatorinterface.RunnableFromFile;
import emulatorinterface.RunnableShm;
import emulatorinterface.RunnableThread;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.shm.SharedMem;
import emulatorinterface.translator.x86.objparser.ObjParser;
import generic.Statistics;

public class Main {
	
	//public static Object syncObject = new Object();
	public static Process process;

	// the reader threads. Each thread reads from EMUTHREADS
	public static RunnableThread [] runners = new RunnableThread[IpcBase.MaxNumJavaThreads];
		
	public static void main(String[] arguments) throws Exception 
	{
		long startTime, endTime;

		startTime = System.currentTimeMillis();

		checkCommandLineArguments(arguments);

		// Read the command line arguments
		String configFileName = arguments[0];
		SimulationConfig.outputFileName = arguments[1];
		
		String executableArguments=" ";
		String executableFile = " ";
		executableFile = arguments[2];
		for(int i=2; i < arguments.length; i++) {
			executableArguments = executableArguments + " " + arguments[i];
		}

		// Parse the command line arguments
		XMLParser.parse(configFileName);

		// Create a hash-table for the static representation of the executable
		ObjParser.buildStaticInstructionTable(executableFile);
		
		// Initialise pool of operands and instructions
		CustomObjectPool.initPool();
		
		// Configure the emulator
		configureEmulator();

		// initialize cores, memory, tokenBus
		initializeArchitecturalComponents();
		
		//find pid
		int pid = Integer.parseInt( ( new File("/proc/self")).getCanonicalFile().getName() );
		System.out.println("Newmain : pid = " + pid);
		
		// create PIN interface
		IpcBase ipcBase = new SharedMem(pid);
		if (SimulationConfig.Mode!=0) {
			process = createPINinterface(ipcBase, executableArguments, pid);
		}


		
		//different core components may work at different frequencies
		
		
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
			if(SimulationConfig.Mode==0) {
				runners[i] = new RunnableFromFile(name,i, ipcBase, ArchitecturalComponent.getCores(), 
						ArchitecturalComponent.getTokenBus());
			} else if (SimulationConfig.Mode==1) {
				runners[i] = new RunnableShm(name,i, ipcBase, ArchitecturalComponent.getCores(), 
						ArchitecturalComponent.getTokenBus());
			} else {
				System.out.println("\n\n This mode not implemented yet \n\n");
			}
		}
		
		// Call these functions at last
		// returns the number of instructions. and waits on a semaphore for
		// finishing of reader threads
		//FIXME : wait stopped for unexpected exit.
		@SuppressWarnings("unused")
		long icount = ipcBase.doExpectedWaitForSelf();
		if (SimulationConfig.Mode!=0) ipcBase.doWaitForPIN(process);
		ipcBase.finish();

		endTime = System.currentTimeMillis();
		Statistics.printAllStatistics(executableFile, startTime, endTime);
				
		System.exit(0);
	}

	private static void initializeArchitecturalComponents() {
		ArchitecturalComponent.setCoreBcastBus(ArchitecturalComponent.initCoreBcastBus());
		ArchitecturalComponent.setCores(ArchitecturalComponent.initCores(ArchitecturalComponent.getCoreBcastBus()));
		ArchitecturalComponent.setTokenBus(ArchitecturalComponent.initTokenBus());
		ArchitecturalComponent.initMemorySystem(ArchitecturalComponent.getCores(),
				ArchitecturalComponent.getTokenBus());
	}



	// TODO Must provide parameters to make from here
	private static void configureEmulator() {

	}

	private static Process createPINinterface(IpcBase ipcBase,
			String executableArguments, int pid) 
	{

		// Creating command for PIN tool.
		String cmd;
		
		System.out.println("subset sim size = "  + SimulationConfig.subsetSimSize + "\t" + SimulationConfig.subsetSimulation);
		
		cmd = SimulationConfig.PinTool + "/pin" +
						" -t " + SimulationConfig.PinInstrumentor +
						" -map " + SimulationConfig.MapEmuCores +
						" -numIgn " + SimulationConfig.NumInsToIgnore +
						" -numSim " + SimulationConfig.subsetSimSize +
						" -id " + pid +
						" -- ";
		cmd += executableArguments;
		
		// System.out.println("cmd = " + cmd);

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
			Error.showErrorAndExit("\n\tIllegal number of arguments !!\n" +
					"Usage java main <config-file> <output-file> <benchmark-program and arguments>");
		}
	}
	
	/*
	 * debug helper functions
	 */

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