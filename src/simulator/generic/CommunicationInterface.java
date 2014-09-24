package generic;

import java.util.Vector;

public abstract class CommunicationInterface {
	public abstract void sendMessage(EventQueue eventQueue,
			int delay, 
			RequestType reqTye, 
			long addr, 
			int coreId,
			Vector<Integer> destinationId,
			SimulationElement source,
			SimulationElement destination, 
			int core_num);
}
