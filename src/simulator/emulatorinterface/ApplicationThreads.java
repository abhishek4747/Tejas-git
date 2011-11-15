package emulatorinterface;

import java.util.ArrayList;

import emulatorinterface.communication.IpcBase;

public class ApplicationThreads {

	static class haltState {
		int tidApp;
		long time;
		public haltState(int appTid, long time) {
			super();
			this.tidApp = appTid;
			this.time = time;
		}
	}
	
	static class appThread {
		appThread () {
//			haltStates = new ArrayList<haltState>();
			finished = false;
			started = false;
			halted = false;
		}
//		ArrayList<haltState> haltStates;
		boolean finished;
		boolean started;
		boolean halted;
	}

	
	static int MaxApplicationThreads = IpcBase.EMUTHREADS * IpcBase.MAXNUMTHREADS;
	static ArrayList<appThread> applicationThreads = new ArrayList<appThread>(MaxApplicationThreads);
	
}
