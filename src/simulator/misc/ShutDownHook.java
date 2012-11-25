package misc;

import main.Main;

public class ShutDownHook extends Thread {
	
	public void run()
	{
		try {
			Main.getEmulator().forceKill();
		}
		finally{
			System.out.println("shut down");
			//Runtime.getRuntime().runFinalization();
			Runtime.getRuntime().halt(0);
		}
	}

}
