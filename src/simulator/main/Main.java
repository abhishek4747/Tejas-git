package main;

import java.io.File;
import java.io.IOException;

import memorysystem.nuca.DNuca;
import memorysystem.nuca.DNucaBank;
import memorysystem.nuca.NucaCache;
import memorysystem.nuca.SNuca;
import misc.Error;
import misc.ShutDownHook;
import config.CommunicationType;
import config.EmulatorConfig;
import config.EmulatorType;
import config.SimulationConfig;
import config.SystemConfig;
import config.XMLParser;
import emulatorinterface.RunnableThread;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.filePacket.FilePacket;
import emulatorinterface.communication.network.Network;
import emulatorinterface.communication.shm.SharedMem;
import emulatorinterface.translator.x86.objparser.ObjParser;
import generic.BenchmarkThreadMapping;
import generic.Operand;
import generic.PinPointsProcessing;
import generic.Statistics;


public class Main {
	
	private static Emulator emulator;
	
	// the reader threads. Each thread reads from EMUTHREADS
	public static RunnableThread [] runners;
	public static volatile boolean statFileWritten = false;
	
	private static  String emulatorFile = " ";
	
	public static int pid;
	public static IpcBase ipcBase;

	public static String executableAndArguments[];
	public static String benchmarkArguments = " ";
	public static long startTime, endTime;
	public static boolean xmlParsingCompleted = false;
	
	public static BenchmarkThreadMapping benchmarkThreadMapping;
	
	public static void main(String[] arguments)
	{
		//different core components may work at different frequencies
		startTime = System.currentTimeMillis();
		
		//register shut down hook
		Runtime.getRuntime().addShutdownHook(new ShutDownHook());
		
		checkCommandLineArguments(arguments);
		setEmulatorFile(arguments[2]);

		// Read the command line arguments
		String configFileName = arguments[0];
		SimulationConfig.outputFileName = arguments[1];
		
		executableAndArguments = new String[arguments.length-2];
		
		// read the command line arguments for the benchmark (not emulator) here.
		for(int i=2; i < arguments.length; i++) {
			executableAndArguments[i-2] = arguments[i]; 
			benchmarkArguments = benchmarkArguments + " " + arguments[i];
		}
		
		// Parse the command line arguments
		XMLParser.parse(configFileName);
		xmlParsingCompleted = true;
		
		// Initialize the statistics
		Statistics.initStatistics();
		
		PinPointsProcessing.initialize();
		
		initializeObjectPools();
		
		// Create a hash-table for the static representation of the executable
		if(EmulatorConfig.emulatorType==EmulatorType.pin) {
			ObjParser.buildStaticInstructionTable(getEmulatorFile());
		} else if(EmulatorConfig.emulatorType==EmulatorType.none) {
			ObjParser.initializeThreadMicroOpsList(SystemConfig.numEmuThreadsPerJavaThread);
		}
		
		ObjParser.initializeDynamicInstructionBuffer(SystemConfig.numEmuThreadsPerJavaThread*SystemConfig.numEmuThreadsPerJavaThread);
		ObjParser.initializeControlMicroOps();
		
		// initialize cores, memory, tokenBus
		initializeArchitecturalComponents();
		
		//find pid
		getMyPID();
				
		System.out.println("Newmain : pid = " + pid);

		// Start communication channel before starting emulator
		// PS : communication channel must be started before starting the emulator
		ipcBase = startCommunicationChannel(pid);
		benchmarkThreadMapping = new BenchmarkThreadMapping(getExecutableAndArguments());
		
		runners = new RunnableThread[SystemConfig.maxNumJavaThreads];
		
		String emulatorArguments = constructEmulatorArguments(benchmarkArguments);
				
		// start emulator
		startEmulator(emulatorArguments, pid, ipcBase);

		String name;
		for (int i=0; i<SystemConfig.maxNumJavaThreads; i++) {
			
			name = "thread"+Integer.toString(i);
			
			runners[i] = new RunnableThread(name,i, ipcBase, ArchitecturalComponent.getCores(), 
				ArchitecturalComponent.getTokenBus());
		}
		
		ipcBase.waitForJavaThreads();
		if(emulator!=null) {
			emulator.forceKill();
		}
		
		ipcBase.finish();

		endTime = System.currentTimeMillis();
		
		PinPointsProcessing.windup();
		
		Statistics.printAllStatistics(getEmulatorFile(), startTime, endTime);
		statFileWritten = true;
		
		System.out.println("\n\nSimulation completed !!");
		System.exit(0);
	}

	public static void initializeObjectPools() {
		
		int numStaticInstructions = 0;
		
		if(EmulatorConfig.emulatorType == EmulatorType.pin) {
			// approximately 3 micro-operations are required per cisc instruction
			numStaticInstructions = ObjParser.noOfLines(getEmulatorFile()) * 3;
		} else {
			
		}
		
		// Initialise pool of instructions
		CustomObjectPool.initCustomPools(SystemConfig.maxNumJavaThreads*SystemConfig.numEmuThreadsPerJavaThread, numStaticInstructions);
		
		// Pre-allocate all the possible operands
		Operand.preAllocateOperands();
	}

	private static IpcBase startCommunicationChannel(int pid) {
		IpcBase ipcBase = null;
		if(EmulatorConfig.communicationType==CommunicationType.sharedMemory) {
			ipcBase = new SharedMem(pid);
 		} else if(EmulatorConfig.communicationType==CommunicationType.network) {
 			//ipcBase = new Network(IpcBase.MaxNumJavaThreads*IpcBase.EmuThreadsPerJavaThread);
 			ipcBase = new Network();
 		} else if(EmulatorConfig.communicationType==CommunicationType.file) {
 			ipcBase = new FilePacket(getExecutableAndArguments());
 		} else {
 			ipcBase = null;
 			misc.Error.showErrorAndExit("Incorrect coomunication type : " + EmulatorConfig.communicationType);
 		}
		
		return ipcBase;
	}

	private static void startEmulator(String emulatorArguments, int pid, IpcBase ipcBase) {
		if(EmulatorConfig.communicationType==CommunicationType.file) {
			// The emulator is not needed when we are reading from a file
			emulator = null;
		} else {
			
			if (EmulatorConfig.emulatorType==EmulatorType.pin) {
				emulator = new Emulator(EmulatorConfig.PinTool, EmulatorConfig.PinInstrumentor, 
						emulatorArguments, ((SharedMem)ipcBase).idToShmGet);
			} else if (EmulatorConfig.emulatorType==EmulatorType.qemu) {
				emulator = new Emulator(EmulatorConfig.QemuTool + " " + emulatorArguments, pid);
			} else if (EmulatorConfig.emulatorType==EmulatorType.none) {
				emulator = null;
			} else {
				emulator = null;
				misc.Error.showErrorAndExit("Invalid emulator type : " + EmulatorConfig.emulatorType);
			}
		}
	}

	private static String constructEmulatorArguments(String benchmarkArguments) {
		String emulatorArguments = " ";
		
		if(EmulatorConfig.communicationType == CommunicationType.network) {
			System.out.println("Emulator argument passed! portStart is: "+Network.portStart);
			// Passing the start Port No through command line to the emulator
			emulatorArguments += "-P " + Network.portStart;	
		}
		
		if(EmulatorConfig.emulatorType == EmulatorType.qemu) {
			// send num instructions to skip and simulate to Qemu.
			// semantics : this fields apply locally to all the threads in Qemu.
			emulatorArguments += " -SO " + SimulationConfig.NumInsToIgnore 
					+ " -ST " + SimulationConfig.subsetSimSize;
		}
		
		// convention : benchmark specific arguments come at the end only.
		emulatorArguments += benchmarkArguments;
		return emulatorArguments;
	}
	
	public static void initializeArchitecturalComponents() {
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
	
	private static void getMyPID() {
		pid = -1;
		try {
			pid = Integer.parseInt( ( new File("/proc/self")).getCanonicalFile().getName() );
		} catch (NumberFormatException e) {
			e.printStackTrace();
			misc.Error.showErrorAndExit("Eror in obtaining pid of java process");
		} catch (IOException e) {
			e.printStackTrace();
			misc.Error.showErrorAndExit("Eror in obtaining pid of java process");
		}
	}

	public static String getEmulatorFile() {
		return emulatorFile;
	}

	public static void setEmulatorFile(String emulatorFile) {
		Main.emulatorFile = emulatorFile;
	}

	public static String[] getExecutableAndArguments() {
		return executableAndArguments;
	}

	public static String getBenchmarkArguments() {
		return benchmarkArguments;
	}
	
	public static long getStartTime() {
		return startTime;
	}
}