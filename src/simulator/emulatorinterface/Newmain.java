package emulatorinterface;

import misc.Error;
import config.SimulationConfig;
import config.XMLParser;
import emulatorinterface.DynamicInstructionBuffer;
import emulatorinterface.communication.*;
import emulatorinterface.communication.shm.SharedMem;
import emulatorinterface.translator.x86.objparser.ObjParser;
import generic.InstructionTable;

public class Newmain {
	
	public static int handled=0;
	public static int notHandled=0;

	public static void main(String[] arguments) throws Exception 
	{
		// check command line arguments
		checkCommandLineArguments(arguments);

		// Read the command line arguments
		String executableFile = arguments[0];

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
		Process process = createPINinterface(ipcBase, executableFile,
				dynamicInstructionBuffer);

		// returns the number of instructions. and waits on a semaphore for
		// finishing of reader threads
		long icount = ipcBase.doExpectedWaitForSelf();

		// Call these functions at last
		ipcBase.doWaitForPIN(process);
		ipcBase.finish();
		
		// Display coverage
		double coverage = (double)(handled*100)/(double)(handled+notHandled);
		System.out.print("\n\tProgram ran succesfuly with a coverage =  " + coverage + " %\n");
	}

	// TODO Must provide parameters to make from here
	private static void configureEmulator() {

	}

	private static Process createPINinterface(IPCBase ipcBase,
			String executableFilePath,
			DynamicInstructionBuffer dynamicInstructionBuffer) {

		// Creating command for PIN tool.
		String cmd = SimulationConfig.PinTool + "/pin" + " -t "
				+ SimulationConfig.PinInstrumentor + " -map "
				+ SimulationConfig.MapEmuCores + " -- " + executableFilePath;

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
		if (arguments.length != 1) {
			Error.showErrorAndExit("\n\tIllegal number of arguments !!");
		}
	}
}