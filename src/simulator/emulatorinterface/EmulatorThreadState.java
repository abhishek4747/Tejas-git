package emulatorinterface;

import java.util.ArrayList;

import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.Packet;
import generic.InstructionLinkedList;

public class EmulatorThreadState {
	
	boolean finished;
	boolean started = false;
	boolean halted = false;
	boolean isFirstPacket = true;
	
	//int readerLocation;
	long totalRead;
	
	
	Packet pold = new Packet();
	EmulatorPacketList packetList = new EmulatorPacketList();
	
	public void checkStarted() {
		if (this.isFirstPacket) {
			this.started = true;
		}
	}
}
