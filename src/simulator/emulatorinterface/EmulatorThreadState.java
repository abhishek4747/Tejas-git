package emulatorinterface;

import generic.GenericCircularQueue;
import generic.Instruction;

public class EmulatorThreadState {
	
	boolean finished;
	boolean started = false;
	boolean halted = false;
	boolean isFirstPacket = true;
	
	//int readerLocation;
	long totalRead;
	
	// We are assuming that one assembly instruction can have atmost 50 micro-operations.
	// Its an over-estimation.
	GenericCircularQueue<Instruction> outstandingMicroOps = new GenericCircularQueue<Instruction>(Instruction.class, 50);
	
	// Packet pold = new Packet();
	EmulatorPacketList packetList = new EmulatorPacketList();
	
	public void checkStarted() {
		if (this.isFirstPacket) {
			this.started = true;
		}
	}
}
