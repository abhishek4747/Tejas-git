package generic;

import emulatorinterface.communication.Packet;

import java.util.Hashtable;

public class BarrierTable {
	
	public static Hashtable<Long, Barrier> barrierList = new Hashtable<Long, Barrier>();
	
	public BarrierTable(){
//		this.barrierList = new Hashtable<Long, Barrier>();
	}
	
	public static void barrierListAdd(Packet packet){
		Barrier barrier = new Barrier(packet.tgt, (int) packet.ip);
		barrierList.put(packet.tgt, barrier);
	}

}
