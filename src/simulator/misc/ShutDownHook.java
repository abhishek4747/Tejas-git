package misc;

import generic.Statistics;
import main.Main;

public class ShutDownHook extends Thread {
	
	public void run()
	{
		try {
			Main.getEmulator().forceKill();
		}
		finally{
			System.out.println("shut down");
			
			if(Main.statFileWritten == false)
			{
				Statistics.printAllStatistics(Main.getEmulatorFile(), -1, -1);
			}
			//Runtime.getRuntime().runFinalization();
			Runtime.getRuntime().halt(0);
		}
	}

}
