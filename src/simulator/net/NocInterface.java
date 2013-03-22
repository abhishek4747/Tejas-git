package net;

import generic.Port;
import generic.SimulationElement;

import java.util.Vector;

public interface NocInterface {
	
	public Router getRouter();
	public Vector<Integer> getId();
	public Port getPort();
	public SimulationElement getSimulationElement();
	
}
