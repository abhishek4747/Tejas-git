package emulatorinterface;

import java.util.ArrayList;

import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.Packet;
import generic.InstructionLinkedList;

public class ThreadParams {
	boolean finished;
	boolean started;
	boolean halted;

	int readerLocation;
	long totalRead;
	Packet pold = new Packet();
	boolean isFirstPacket = true;
	ArrayList<Packet> packets=new ArrayList<Packet>();
	
	long lastTimerseen=0;
	 // for each distinct address a thread should have a threadState
	ArrayList<ThreadState> threadState = new ArrayList<ThreadState>();
	
	public void checkStarted() {
		if (this.isFirstPacket) {
			this.started = true;
		}
	}
	public void checkFirstPacket() {
		if (this.isFirstPacket)
			this.isFirstPacket = false;
	}
	public void updateReaderLocation(int numReads) {
		this.readerLocation = (this.readerLocation + numReads) % IpcBase.COUNT;

	}

}
