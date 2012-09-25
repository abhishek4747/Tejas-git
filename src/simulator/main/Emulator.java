package main;

import config.SimulationConfig;
import emulatorinterface.communication.StreamGobbler;

public class Emulator {
	
	private Process emulatorProcess;
	StreamGobbler s1;
	StreamGobbler s2;
	
	public Emulator(String pinTool, String pinInstrumentor, 
			String executableArguments, int pid) 
	{
		System.out.println("subset sim size = "  + 
				SimulationConfig.subsetSimSize + "\t" + 
				SimulationConfig.subsetSimulation);

		// Creating command for PIN tool.
		String cmd = pinTool + "/pin" +
				" -t " + pinInstrumentor +
				" -map " + SimulationConfig.MapEmuCores +
				" -numIgn " + SimulationConfig.NumInsToIgnore +
				" -numSim " + SimulationConfig.subsetSimSize +
				" -id " + pid +
				" -- " + executableArguments;
		
		startEmulator(cmd);
	}
	
	public Emulator(String qemuTool, int pid)
	{
		String cmd = "";
		
		startEmulator(cmd);
	}


	// Start the PIN process. Parse the cmd accordingly
	private void startEmulator(String cmd) {
		Runtime rt = Runtime.getRuntime();
		try {
			emulatorProcess = rt.exec(cmd);
			s1 = new StreamGobbler ("stdin", emulatorProcess.getInputStream ());
			s2 = new StreamGobbler ("stderr", emulatorProcess.getErrorStream ());
			s1.start ();
			s2.start ();
		} catch (Exception e) {
			e.printStackTrace();
			misc.Error.showErrorAndExit("Error in starting the emulator.\n" +
					"Emulator Command : " + cmd);
		}
	}
	
	// Should wait for PIN too before calling the finish function to deallocate stuff related to
	// the corresponding mechanism
	public void waitForEmulator() {
		try {
			emulatorProcess.waitFor();
			s1.join();
			s2.join();
		} catch (Exception e) { }
	}
	
	public void forceKill() {
		emulatorProcess.destroy();
	}
}
