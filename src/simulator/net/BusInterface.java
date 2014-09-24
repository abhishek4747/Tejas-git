package net;

import java.util.Vector;

import memorysystem.AddressCarryingEvent;

import generic.CommunicationInterface;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class BusInterface extends CommunicationInterface{

	SimulationElement reference;
	
	public BusInterface(SimulationElement reference) {
		super();
		this.reference = reference;
	}

	@Override
	public void sendMessage(EventQueue eventQueue, int delay,
			RequestType reqTye, long addr, int coreId,
			Vector<Integer> destinationId, SimulationElement source,
			SimulationElement destination, int core_num) {
		destination.getPort().put(
				new AddressCarryingEvent(
					eventQueue,
					delay, //requestingCache.getLatency() + getNetworkDelay(), FIXME: 
					source, 
					destination,
					reqTye, 
					addr,
					core_num));		
	}
}
