package net;

import generic.CommunicationInterface;
import generic.Event;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;

import java.util.Vector;

import config.Interconnect;
import config.SystemConfig;

public class NocElementDummy extends SimulationElement
{
	public NocElementDummy(PortType portType, int noOfPorts, long occupancy,
			EventQueue eq, long latency, long frequency) {
		super(PortType.Unlimited, 1, 1, null, 1, 1);
		if(SystemConfig.interconnect == Interconnect.Bus)
		{
			comInterface = new BusInterface(this);
		}
		else if(SystemConfig.interconnect == Interconnect.Noc)
		{
			comInterface = new NocInterface(SystemConfig.nocConfig, this);
		}
	}
	public CommunicationInterface comInterface;
	Vector<Integer> nocElementId;
	
	
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}
}
