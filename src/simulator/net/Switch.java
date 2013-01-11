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
	protected int availBuff;           //available number of buffers
	public int hopCounters;
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
		this.cacheBankColumns = nocConfig.numberOfBankColumns;
		this.topology = nocConfig.topology;
		this.rAlgo = nocConfig.rAlgo;
		this.availBuff = nocConfig.numberOfBuffers;
		this.hopCounters = 0;
		
	}
	public Switch(NocConfig nocConfig){
		super(nocConfig.portType,
				nocConfig.getAccessPorts(), 
				nocConfig.getPortOccupancy(),
				nocConfig.getLatency(),
				nocConfig.operatingFreq);
		this.selScheme = nocConfig.selScheme;
		this.connection = new Switch[4];
		this.availBuff = nocConfig.numberOfBuffers;
		this.range = new int[2]; // used in fat tree
		this.hopCounters = 0;
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
			if((range[0] + range[1])/2 < bankNumber)
				return 1;
			else
				return 3;
		}
	}
	/************************************************************************
     * Method Name  : AllocateBuffer
     * Purpose      : check whether buffer available
     * Parameters   : none
     * Return       : true if allocated , false if no buffer available
     *************************************************************************/
	public boolean AllocateBuffer(boolean reqOrReply)  // reqOrReplay = true=>incoming false=>outgoing 
	{
		if(reqOrReply){
			if(this.availBuff>0)     //incoming request leave atleast one buff space
			{						 //for outgoing request to avoid deadlock
				this.availBuff --;
				return true;
			}
		}
		else{
			if(this.availBuff>1)
			{
				this.availBuff --;
				return true;
			}
		}
		return false;
	}
	public void FreeBuffer()
	{
		this.availBuff ++;
	}
	
	public int bufferSize()
	{
		return this.availBuff;
	}
/*	public boolean CheckNeighbourBuffer(int nextId)  //request for neighbour buffer
	{
		return ((Switch) this.connection[nextId]).AllocateBuffer();
	}*/

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
		int nextID;
		Vector<Integer> destinationId = ((AddressCarryingEvent)(event)).getDestinationBankId();
		int bankNumber = destinationId.elementAt(1);
		String binary = Integer.toBinaryString(cacheBankColumns | bankNumber).substring(1);
		RequestType requestType = event.getRequestType();
		
		if(topology == TOPOLOGY.BUTTERFLY || topology == TOPOLOGY.OMEGA)
			nextID = nextIdbutterflyOmega(binary);
		else //if(topology == TOPOLOGY.FATTREE)
			nextID = nextIdFatTree(bankNumber);
		this.hopCounters++;
		this.connection[nextID].getPort().put(
				event.update(
						eventQ,
						1,
						this, 
						this.connection[nextID],
						requestType));
	}

}
