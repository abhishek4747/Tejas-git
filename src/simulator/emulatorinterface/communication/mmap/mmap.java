package emulatorinterface.communication.mmap;

import emulatorinterface.DynamicInstructionBuffer;
import emulatorinterface.communication.*;


public class mmap //extends IpcBase
{
	static int t;

	public mmap(){
		t=5;
		System.out.println("dummy constructor");
	}
	
	public Process startPIN(String cmd) throws Exception{
		Runtime rt = Runtime.getRuntime();
		try {
			Process p = rt.exec(cmd);
			StreamGobbler s1 = new StreamGobbler ("stdin", p.getInputStream ());
			StreamGobbler s2 = new StreamGobbler ("stderr", p.getErrorStream ());
			s1.start ();
			s2.start ();
			return p;
		} catch (Exception e) {
			return null;
		}
	}
	
	public void createRunnables(DynamicInstructionBuffer passPackets) {
		System.out.println("creat readers");
		t++;
	}
	
	public long doExpectedWaitForSelf() {
		System.out.println("wait for self");
		return 0;
	}
	
	public void doWaitForPIN(Process p){
		System.out.println("wait for pin");
	}
	
	public void finish(){
		System.out.println("finish"+t);
	}
}
