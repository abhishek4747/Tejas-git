package net;

import java.util.Vector;

import memorysystem.AddressCarryingEvent;
import net.NOC.TOPOLOGY;
import net.RoutingAlgo.ALGO;
import net.RoutingAlgo.SELSCHEME;

import config.NocConfig;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class Switch extends SimulationElement{
	
	protected SELSCHEME selScheme;
	protected Switch connection[];
	protected int range[];
	protected int level;
	protected int cacheBankColumns;
	public TOPOLOGY topology;
	public ALGO rAlgo;
	//0 - up ; 1 - right ; 2- down ; 3- left (clockwise) 
	
	public Switch(NocConfig nocConfig,int level){
		super(nocConfig.portType,
				nocConfig.getAccessPorts(), 
				nocConfig.getPortOccupancy(),
				nocConfig.getLatency(),
				nocConfig.operatingFreq);
		this.selScheme = nocConfig.selScheme;
		this.connection = new Switch[4];
		this.level = level; //used in omega network
		this.cacheBankColumns = nocConfig.numberOfColumns;
		this.topology = nocConfig.topology;
		this.rAlgo = nocConfig.rAlgo;
		
	}
	public Switch(NocConfig nocConfig){
		super(nocConfig.portType,
				nocConfig.getAccessPorts(), 
				nocConfig.getPortOccupancy(),
				nocConfig.getLatency(),
				nocConfig.operatingFreq);
		this.selScheme = nocConfig.selScheme;
		this.connection = new Switch[4];
		this.range = new int[2]; // used in fat tree
	}
	
	public int nextIdbutterflyOmega(String binary)
	{
		if(binary.charAt(level) == '0')
			return 2;
		else
			return 3;
	}
	
	public int nextIdFatTree(int bankNumber)
	{
		if(bankNumber < range[0] || bankNumber > range[1])
			return 0;
		else
		{
			if((range[0] + range[1])/2 > bankNumber)
				return 1;
			else
				return 3;
		}
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
		int nextID;
		Vector<Integer> destinationId = ((AddressCarryingEvent)(event)).getDestinationBankId();
		int bankNumber = destinationId.elementAt(1);
		String binary = Integer.toBinaryString(cacheBankColumns | bankNumber).substring(1);
		RequestType requestType = event.getRequestType();
		
		String routerClassName = new String("net.Router");
		//System.out.println("WORNG PLACE");
		//System.exit(0);
		
		if(this.getClass().getName().equals(routerClassName))
			((Router)(this)).bankReference.getPort().put(
					event.update(
							eventQ,
							0,
							this, 
							((Router)(this)).bankReference,
							requestType));
		if(topology == TOPOLOGY.BUTTERFLY || topology == TOPOLOGY.OMEGA)
			nextID = nextIdbutterflyOmega(binary);
		else //if(topology == TOPOLOGY.FATTREE)
			nextID = nextIdFatTree(bankNumber);
		
		this.connection[nextID].getPort().put(
				event.update(
						eventQ,
						1,
						this, 
						this.connection[nextID],
						requestType));
	}

}
