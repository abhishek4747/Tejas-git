package main;

import java.io.File;

import config.EmulatorConfig;
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
		
		System.out.println("marker functions = "  + SimulationConfig.markerFunctionsSimulation 
				+ "\t start marker = " + SimulationConfig.startMarker
				+ "\t end marker = " + SimulationConfig.endMarker);

		// Creating command for PIN tool.
		StringBuilder pin = null;
		
		if(new File(pinTool + "/pin.sh").exists())
		{
			pin = new StringBuilder(pinTool + "/pin.sh");
		}
		else
		{
			pin = new StringBuilder(pinTool + "/pin");
		}

		StringBuilder cmd = new StringBuilder(pin + // " -injection child "+
				" -t " + pinInstrumentor +
				" -map " + SimulationConfig.MapEmuCores +
				" -numIgn " + SimulationConfig.NumInsToIgnore +
				" -numSim " + SimulationConfig.subsetSimSize +
				" -id " + pid);
		
		if(SimulationConfig.pinpointsSimulation == true)
		{
			cmd.append(" -pinpointsFile " + SimulationConfig.pinpointsFile);
		}
		if(SimulationConfig.startMarker != "")
		{
			cmd.append(" -startMarker " + SimulationConfig.startMarker);
		}
		if(SimulationConfig.endMarker != "")
		{
			cmd.append(" -endMarker " + SimulationConfig.endMarker);
		}
		
		cmd.append(" -- " + executableArguments);
		System.out.println("command is : " + cmd.toString());
		
		startEmulator(cmd.toString());
	}
	
	public Emulator(String qemuTool, int pid)
	{
		startEmulator(qemuTool);
	}


	// Start the PIN process. Parse the cmd accordingly
	private void startEmulator(String cmd) {
		emulatorCommand = cmd;
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
		
		if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_PIN) {
			//System.err.println(errorMessage);
			Process process;
			String cmd[] = {"/bin/sh",
				      "-c",
				      "killall -9 " + Main.getEmulatorFile()
			};
	
			try 
			{
				process = Runtime.getRuntime().exec(cmd);
				int ret = process.waitFor();
				System.out.println("ret : " + ret);
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	private static String emulatorCommand = null;

	public static String getEmulatorCommand() {
		return emulatorCommand;
	}
	
	
}
