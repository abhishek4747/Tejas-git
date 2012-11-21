package main;


import java.io.File;
import java.io.IOException;

import misc.Error;
import misc.ShutDownHook;
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
	
	private static Emulator emulator;
	
	// the reader threads. Each thread reads from EMUTHREADS
	public static RunnableThread [] runners = new RunnableThread[IpcBase.MaxNumJavaThreads];
	
	private static long startTime, endTime;

	public static void main(String[] arguments)
	{
		startTime = System.currentTimeMillis();
		
		//register shut down hook
		Runtime.getRuntime().addShutdownHook(new ShutDownHook());

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
		
		// Initialize the statistics
		Statistics.initStatistics();

		// Create a hash-table for the static representation of the executable
		ObjParser.buildStaticInstructionTable(executableFile);
		
		// Initialise pool of operands and instructions
		CustomObjectPool.initPool();
		
		// Configure the emulator
		configureEmulator();

		// initialize cores, memory, tokenBus
		initializeArchitecturalComponents();
		
		//find pid
		int pid = getMyPID();
				
		System.out.println("Newmain : pid = " + pid);
		
		// create PIN interface
		IpcBase ipcBase = new SharedMem(pid);
		if (SimulationConfig.Mode!=0) {
			createPINinterface(ipcBase, executableArguments, ((SharedMem)ipcBase).idToShmGet);
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
		
		ipcBase.waitForJavaThreads();
		if (SimulationConfig.Mode!=0) {
			emulator.waitForEmulator();
		}
		
		ipcBase.finish();

		endTime = System.currentTimeMillis();
		Statistics.printAllStatistics(executableFile, startTime, endTime);
		
		System.out.println("\n\nSimulation completed !!");
				
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

	private static void createPINinterface(IpcBase ipcBase,
			String executableArguments, int pid) 
	{

		// Creating command for PIN tool.
		String cmd;
		
		System.out.println("subset sim size = "  + 
				SimulationConfig.subsetSimSize + "\t" + 
				SimulationConfig.subsetSimulation);
		
		cmd = SimulationConfig.PinTool + "/pin" +
						" -t " + SimulationConfig.PinInstrumentor +
						" -map " + SimulationConfig.MapEmuCores +
						" -numIgn " + SimulationConfig.NumInsToIgnore +
						" -numSim " + SimulationConfig.subsetSimSize +
						" -id " + pid +
						" -- ";
		cmd += executableArguments;
		
		// System.out.println("cmd = " + cmd);

		emulator = new Emulator();
		emulator.startEmulator(cmd);

		ipcBase.initIpc();
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
	
	public static long getStartTime() {
		return startTime;
	}

	public static long getEndTime() {
		return endTime;
	}
	
	public static void setStartTime(long startTime) {
		Main.startTime = startTime;
	}

	public static void setEndTime(long endTime) {
		Main.endTime = endTime;
	}

	public static Emulator getEmulator() {
		return emulator;
	}
	
	private static int getMyPID() {
		int pid = -1;
		try {
			pid = Integer.parseInt( ( new File("/proc/self")).getCanonicalFile().getName() );
		} catch (NumberFormatException e) {
			e.printStackTrace();
			misc.Error.showErrorAndExit("Eror in obtaining pid of java process");
		} catch (IOException e) {
			e.printStackTrace();
			misc.Error.showErrorAndExit("Eror in obtaining pid of java process");
		}
		
		return pid;
	}
}