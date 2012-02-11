/*****************************************************************************
				BhartiSim Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
------------------------------------------------------------------------------------------------------------


*****************************************************************************/
package net;

import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

import java.util.*;

import config.NocConfig;

import memorysystem.AddressCarryingEvent;
import memorysystem.nuca.NucaCacheBank;

public class Router extends SimulationElement{
	
	protected Vector<Integer> bankId = new Vector<Integer>(2); //bank id of router(vector <row,column>)
	protected int availBuff;                                   //available number of buffer in router
	protected RoutingAlgo routingAlgo = new RoutingAlgo();
	protected Vector<Router> neighbours= new Vector<Router>(4);
				//0 - up ; 1 - right ; 2- down ; 3- left (clockwise) 
	public NOC.TOPOLOGY topology;
	public RoutingAlgo.ALGO rAlgo;
	protected int numberOfRows;
	protected int numberOfColumns;
	protected NucaCacheBank bankReference;
	protected int latencyBetweenBanks;
	
	public Router(Vector<Integer> bankId, NocConfig nocConfig, NucaCacheBank bankReference)
	{
		super(nocConfig.portType,
				nocConfig.getAccessPorts(), 
				nocConfig.getPortOccupancy(),
				nocConfig.getLatency(),
				nocConfig.operatingFreq);
		this.bankId = bankId;
		this.availBuff = nocConfig.numberOfBuffers;
		this.topology = nocConfig.topology;
		this.rAlgo = nocConfig.rAlgo;
		this.numberOfRows = nocConfig.numberOfRows;
		this.numberOfColumns = nocConfig.numberOfColumns;
		this.bankReference = bankReference;
		this.latencyBetweenBanks = nocConfig.latencyBetweenBanks;
	}
	
	public Vector<Integer> getBankId()
	{
		return this.bankId;
	}
	
	public boolean AllocateBuffer()  
	{
		if(this.availBuff>0)
		{
			this.availBuff --;
			return true;
		}
		return false;
	}
	public void SetConnectedBanks(RoutingAlgo.DIRECTION dir,NucaCacheBank cacheBank)
	{
		this.neighbours.add(dir.ordinal(), cacheBank.getRouter());
	}
	public void SetConnectedBanks(RoutingAlgo.DIRECTION dir)
	{
		this.neighbours.add(dir.ordinal(), null);
	}
	public Vector<Router> GetNeighbours()
	{
		return this.neighbours;
	}
	
	public boolean CheckNeighbourBuffer(RoutingAlgo.DIRECTION nextId)  //request for neighbour buffer
	{
		return this.neighbours.elementAt(nextId.ordinal()).AllocateBuffer();
	}
	
	public void FreeBuffer()
	{
		this.availBuff ++;
	}
	
	public RoutingAlgo.DIRECTION RouteComputation(Vector<Integer> current,Vector<Integer> destination)
	{ 
		//find the route to go
		switch (this.rAlgo) {
		case WESTFIRST :
			return routingAlgo.WestFirstnextBank(current, destination,this.topology,this.numberOfRows,this.numberOfColumns);
		case NORTHLAST : 
			return routingAlgo.NorthLastnextBank(current, destination,this.topology,this.numberOfRows,this.numberOfColumns);
		case NEGATIVEFIRST :
			return routingAlgo.NegativeFirstnextBank(current, destination,this.topology,this.numberOfRows,this.numberOfColumns);
		case TABLE :
		break;
		case SIMPLE :
			return routingAlgo.XYnextBank(current, destination,this.topology,this.numberOfRows,this.numberOfColumns);
		}
		return routingAlgo.XYnextBank(current, destination,this.topology,this.numberOfRows,this.numberOfColumns);
		//no mans land
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		RoutingAlgo.DIRECTION nextID;
		Vector<Integer> currentId = this.getBankId();
		Vector<Integer> destinationId = ((AddressCarryingEvent)(event)).getDestinationBankId();
		RequestType requestType = event.getRequestType();
		if(requestType == RequestType.Cache_Read)
		{
			requestType = RequestType.CacheBank_Read;
			if(this.AllocateBuffer())
			{
				this.getPort().put(
						event.update(
								eventQ,
								0,	//this.getLatency()
								this, 
								this,
								requestType));
			}
			else
			{
				//post event to this ID
				this.getPort().put(
						event.update(
								eventQ,
								latencyBetweenBanks,
								this, 
								this,
								requestType));
				System.out.println(event.getRequestingElement());
			}
		}
		else if(requestType == RequestType.Cache_Write)
		{
			requestType = RequestType.CacheBank_Write;
			if(this.AllocateBuffer())
			{
				this.getPort().put(
						event.update(
								eventQ,
								0,	//this.getLatency()
								this, 
								this,
								requestType));
			}
			else
			{
				//post event to this ID
				this.getPort().put(
						event.update(
								eventQ,
								latencyBetweenBanks,
								this, 
								this,
								requestType));
			}
		}
		else if(requestType == RequestType.Cache_Read_from_iCache)
		{
			requestType = RequestType.CacheBank_Read_from_iCache;
			if(this.AllocateBuffer())
			{
				this.getPort().put(
						event.update(
								eventQ,
								0,	//this.getLatency()
								this, 
								this,
								requestType));
			}
			else
			{
				//post event to this ID
				this.getPort().put(
						event.update(
								eventQ,
								latencyBetweenBanks,
								this, 
								this,
								requestType));
				System.out.println(event.getRequestingElement());
			}
		}
		else if(requestType == RequestType.Mem_Response)
		{
			requestType = RequestType.MemBank_Response;
			if(this.AllocateBuffer())
			{
				this.getPort().put(
						event.update(
								eventQ,
								0,	//this.getLatency()
								this, 
								this,
								requestType));
			}
			else
			{
				//post event to this ID
				this.getPort().put(
						event.update(
								eventQ,
								latencyBetweenBanks,
								this, 
								this,
								requestType));
				System.out.println(event.getRequestingElement());
			}
		}
		else if(requestType == RequestType.Main_Mem_Read)
		{
			requestType = RequestType.Main_MemBank_Read;
			if(this.AllocateBuffer())
			{
				this.getPort().put(
						event.update(
								eventQ,
								0,	//this.getLatency()
								this, 
								this,
								requestType));
			}
			else
			{
				//post event to this ID
				this.getPort().put(
						event.update(
								eventQ,
								latencyBetweenBanks,
								this, 
								this,
								requestType));
				System.out.println(event.getRequestingElement());
			}
		}
		else if(requestType == RequestType.Main_Mem_Write)
		{
			requestType = RequestType.Main_MemBank_Write;
			if(this.AllocateBuffer())
			{
				this.getPort().put(
						event.update(
								eventQ,
								0,	//this.getLatency()
								this, 
								this,
								requestType));
			}
			else
			{
				//post event to this ID
				this.getPort().put(
						event.update(
								eventQ,
								latencyBetweenBanks,
								this, 
								this,
								requestType));
				System.out.println(event.getRequestingElement());
			}
		}
		else if(requestType == RequestType.Main_Mem_Response)
		{
			requestType = RequestType.Main_MemBank_Response;
			if(this.AllocateBuffer())
			{
				this.getPort().put(
						event.update(
								eventQ,
								0,	//this.getLatency()
								this, 
								this,
								requestType));
			}
			else
			{
				//post event to this ID
				this.getPort().put(
						event.update(
								eventQ,
								latencyBetweenBanks,
								this, 
								this,
								requestType));
				System.out.println(event.getRequestingElement());
			}
		}
		else if(currentId.equals(destinationId))
		{
			if(requestType == RequestType.CacheBank_Read)
				requestType = RequestType.Cache_Read;
			else if(requestType == RequestType.CacheBank_Write)
				requestType = RequestType.Cache_Write;
			else if(requestType == RequestType.CacheBank_Read_from_iCache)
				requestType = RequestType.Cache_Read_from_iCache;
			else if(requestType == RequestType.MemBank_Response)
				requestType = RequestType.Mem_Response;
			else if(requestType == RequestType.Main_MemBank_Read)
				requestType = RequestType.Main_Mem_Read;
			else if(requestType == RequestType.Main_MemBank_Write)
				requestType = RequestType.Main_Mem_Write;
			else if(requestType == RequestType.Main_MemBank_Response)
				requestType = RequestType.Main_Mem_Response;
			this.bankReference.getPort().put(
					event.update(
							eventQ,
							0,
							this, 
							this.bankReference,
							requestType));
			this.FreeBuffer();
		}
		else
		{
			nextID = this.RouteComputation(currentId, destinationId);
			if(this.CheckNeighbourBuffer(nextID))
			{
				//post event to nextID
				this.GetNeighbours().elementAt(nextID.ordinal()).getPort().put(
						event.update(
								eventQ,
								latencyBetweenBanks,	//this.getLatency()
								this, 
								this.GetNeighbours().elementAt(nextID.ordinal()),
								requestType));
				this.FreeBuffer();
			}
			else
			{
				//post event to this ID
				this.getPort().put(
						event.update(
								eventQ,
								latencyBetweenBanks,
								this, 
								this,
								requestType));
			}
		}
	}	
}