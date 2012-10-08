package main;

import java.io.File;
import java.io.IOException;
import misc.Error;
import config.EmulatorConfig;
import config.SimulationConfig;
import config.XMLParser;
import emulatorinterface.RunnableFromFile;
import emulatorinterface.RunnableThread;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.network.Network;
import emulatorinterface.communication.shm.SharedMem;
import emulatorinterface.translator.x86.objparser.ObjParser;
import generic.Statistics;


public class Main {
	
	private static Emulator emulator;
	
	// the reader threads. Each thread reads from EMUTHREADS
	public static RunnableThread [] runners = new RunnableThread[IpcBase.MaxNumJavaThreads];
	
	public static void main(String[] arguments)
	{
		checkCommandLineArguments(arguments);

		// Read the command line arguments
		String configFileName = arguments[0];
		SimulationConfig.outputFileName = arguments[1];
		
		String emulatorArguments=" ";
		String emulatorFile = " ";
		emulatorFile = arguments[2];
		for(int i=2; i < arguments.length; i++) {
			emulatorArguments = emulatorArguments + " " + arguments[i];
		}

		// Parse the command line arguments
		XMLParser.parse(configFileName);
		
		// Initialize the statistics
		Statistics.initStatistics();

		// Create a hash-table for the static representation of the executable
		if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_PIN) {
			ObjParser.buildStaticInstructionTable(emulatorFile);
		}
		
		// Initialise pool of operands and instructions
		CustomObjectPool.initCustomPools(IpcBase.MaxNumJavaThreads*IpcBase.EmuThreadsPerJavaThread);
		
		// initialize cores, memory, tokenBus
		initializeArchitecturalComponents();
		
		//find pid
		int pid = getMyPID();
				
		System.out.println("Newmain : pid = " + pid);

		// Start communication channel before starting emulator
		IpcBase ipcBase = startCommunicationChannel(pid);
		
		// start emulator
		startEmulator(emulatorArguments, pid);

		//different core components may work at different frequencies
		
		//Initialize counters
//		Counters powerCounters[] = new Counters[SystemConfig.NoOfCores];
//		for(int i=0;i<SystemConfig.NoOfCores;i++){
//			powerCounters[i] = new Counters();
//		}
		// Create runnable threads. Each thread reads from EMUTHREADS
		//FIXME A single java thread can have multiple cores
		
		long startTime, endTime;
		startTime = System.currentTimeMillis();
		
		String name;
		for (int i=0; i<IpcBase.MaxNumJavaThreads; i++) {
			
			name = "thread"+Integer.toString(i);
			
			if(EmulatorConfig.CommunicationType==EmulatorConfig.COMMUNICATION_FILE) {
				runners[i] = new RunnableFromFile(name,i, ipcBase, ArchitecturalComponent.getCores(), 
						ArchitecturalComponent.getTokenBus());
			} else {
				runners[i] = new RunnableThread(name,i, ipcBase, ArchitecturalComponent.getCores(), 
						ArchitecturalComponent.getTokenBus());
			}
		}
		
		ipcBase.waitForJavaThreads();
		if(EmulatorConfig.CommunicationType!=EmulatorConfig.COMMUNICATION_FILE) {
			emulator.waitForEmulator();
		}
		
		if(EmulatorConfig.CommunicationType!=EmulatorConfig.COMMUNICATION_FILE) {
			ipcBase.finish();
		}

		endTime = System.currentTimeMillis();
		Statistics.printAllStatistics(emulatorFile, startTime, endTime);
		
		System.out.println("\n\nSimulation completed !!");
				
		System.exit(0);
	}

	private static IpcBase startCommunicationChannel(int pid) {
		IpcBase ipcBase = null;
		if(EmulatorConfig.CommunicationType==EmulatorConfig.COMMUNICATION_FILE) {
			// ipc is not required for file
			ipcBase = null;
		} else if(EmulatorConfig.CommunicationType==EmulatorConfig.COMMUNICATION_SHM) {
			ipcBase = new SharedMem(pid);
 		} else if(EmulatorConfig.CommunicationType==EmulatorConfig.COMMUNICATION_NETWORK) {
 			ipcBase = new Network(IpcBase.MaxNumJavaThreads*IpcBase.EmuThreadsPerJavaThread);
 		} else {
 			ipcBase = null;
 			misc.Error.showErrorAndExit("Incorrect coomunication type : " + EmulatorConfig.CommunicationType);
 		}
		
		return ipcBase;
	}

	private static void startEmulator(String executableArguments, int pid) {
		if(EmulatorConfig.CommunicationType==EmulatorConfig.COMMUNICATION_FILE) {
			// The emulator is not needed when we are reading from a file
			emulator = null;
		} else {
			if (EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_PIN) {
				emulator = new Emulator(EmulatorConfig.PinTool, EmulatorConfig.PinInstrumentor, 
						executableArguments, pid);
			} else if (EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_QEMU) {
				emulator = new Emulator(EmulatorConfig.QemuTool, pid);
			} else {
				emulator = null;
				misc.Error.showErrorAndExit("Invalid emulator type : " + EmulatorConfig.EmulatorType);
			}
		}
	}

	private static void initializeArchitecturalComponents() {
		ArchitecturalComponent.setCoreBcastBus(ArchitecturalComponent.initCoreBcastBus());
		ArchitecturalComponent.setCores(ArchitecturalComponent.initCores(ArchitecturalComponent.getCoreBcastBus()));
		ArchitecturalComponent.setTokenBus(ArchitecturalComponent.initTokenBus());
		ArchitecturalComponent.initMemorySystem(ArchitecturalComponent.getCores(),
				ArchitecturalComponent.getTokenBus());
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