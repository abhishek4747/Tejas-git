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
		while(BarrierTable.barrierList.get(packet.tgt) != null){  //checking for re initialization
//			System.err.println("Barrier reinit");
//			System.exit(0);
			packet.tgt++;
		}
//		System.out.println("It is the barrier init id: " + packet.ip + " add :"+ packet.tgt );
		barrierList.put(packet.tgt, barrier);
	}

}
