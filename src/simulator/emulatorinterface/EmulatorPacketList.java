package emulatorinterface;

import java.util.ArrayList;

import emulatorinterface.communication.Packet;

public class EmulatorPacketList {
	
	ArrayList<Packet> packetList;
	int size = 0;

	public EmulatorPacketList() {
		super();
		this.packetList = new ArrayList<Packet>();
		
		for(int i=0; i<50; i++) {
			packetList.add(new Packet());
		}
	}
	
	public void add(Packet p) {
		if(size==packetList.size()) {
			misc.Error.showErrorAndExit("packetList : trying to push more packets for fuse function" +
				"current size = " + size);
		}
		this.packetList.get(size).set(p);
		size++;
	}
	
	public void clear() {
		size = 0;
	}
	
	public Packet get(int index) {
		if(index>size) {
			misc.Error.showErrorAndExit("trying to access element outside packetList size" + 
				"size = " + size + "\tindex = " + index);
		}

		return packetList.get(index);
	}
	
	public int size() {
		return size;
	}
}
