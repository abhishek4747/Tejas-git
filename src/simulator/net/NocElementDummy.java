package net;

import generic.Event;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;

import java.util.Vector;

import config.SystemConfig;

public class NocElementDummy extends SimulationElement implements NocInterface
{
	public NocElementDummy(PortType portType, int noOfPorts, long occupancy,
			EventQueue eq, long latency, long frequency) {
		super(PortType.Unlimited, 1, 1, null, 1, 1);
		this.router = new Router(SystemConfig.nocConfig, this);
	}
	Router router;
	Vector<Integer> nocElementId;
	
	@Override
	public Router getRouter() {
		// TODO Auto-generated method stub
		return router;
	}

	@Override
	public Vector<Integer> getId() {
		// TODO Auto-generated method stub
		return nocElementId;
	}
	public void setId(Vector<Integer> id) {
		// TODO Auto-generated method stub
		nocElementId = id;
	}
	@Override
	public SimulationElement getSimulationElement() {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}
}
