/*****************************************************************************
				Tejas Simulator
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

	Contributors:  Eldhose Peter
*****************************************************************************/
package net;

import generic.Event;
import generic.EventQueue;
import generic.RequestType;

import java.util.*;

import net.NOC.TOPOLOGY;
import net.RoutingAlgo.SELSCHEME;

import config.NocConfig;

import memorysystem.AddressCarryingEvent;
import memorysystem.nuca.NucaCacheBank;

public class Router extends Switch{
	
	protected Vector<Integer> bankId = new Vector<Integer>(2); //bank id of router(vector <row,column>)
//	protected int availBuff;                                   //available number of buffer in router
	protected RoutingAlgo routingAlgo = new RoutingAlgo();
	
//	public NOC.TOPOLOGY topology;
//	public RoutingAlgo.ALGO rAlgo;
	protected int numberOfRows;
	protected int numberOfColumns;
	protected NucaCacheBank bankReference;
	protected int latencyBetweenBanks;
	protected Vector<Router> neighbours;
	
	/************************************************************************
     * Method Name  : Router
     * Purpose      : Constructor for Router class
     * Parameters   : bank id, noc configuration, cache bank reference
     * Return       : void
     *************************************************************************/
	
	public Router(Vector<Integer> bankId, NocConfig nocConfig, NucaCacheBank bankReference)
	{
		super(nocConfig);
		this.bankId = bankId;
		this.topology = nocConfig.topology;
		this.rAlgo = nocConfig.rAlgo;
		this.numberOfRows = nocConfig.numberOfRows;
		this.numberOfColumns = nocConfig.numberOfColumns;
		this.bankReference = bankReference;
		this.latencyBetweenBanks = nocConfig.latencyBetweenBanks;
		this.neighbours= new Vector<Router>(4);
	}
	
	public Vector<Integer> getBankId()
	{
		return this.bankId;
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
	
	public boolean CheckNeighbourBuffer(RoutingAlgo.DIRECTION nextId,boolean reqOrReply)  //request for neighbour buffer
	{
		return ((Router) this.neighbours.elementAt(nextId.ordinal())).AllocateBuffer(reqOrReply);
	}
	
	/***************************************************************************************
     * Method Name  : RouteComputation
     * Purpose      : computing next bank id,Adaptive algorithm selects less contention path
     * Parameters   : current and destination bank id
     * Return       : next bank id
     ***************************************************************************************/
	public RoutingAlgo.DIRECTION RouteComputation(Vector<Integer> current,Vector<Integer> destination)
	{ 
		//find the route to go
		Vector<RoutingAlgo.DIRECTION> choices = new Vector<RoutingAlgo.DIRECTION>();
		switch (rAlgo) {
		case WESTFIRST :
			choices = routingAlgo.WestFirstnextBank(current, destination,this.topology,this.numberOfRows,this.numberOfColumns);
			break;
		case NORTHLAST : 
			choices = routingAlgo.NorthLastnextBank(current, destination,this.topology,this.numberOfRows,this.numberOfColumns);
			break;
		case NEGATIVEFIRST :
			choices = routingAlgo.NegativeFirstnextBank(current, destination,this.topology,this.numberOfRows,this.numberOfColumns);
			break;
		case TABLE :
		break;
		case SIMPLE :
			choices = routingAlgo.XYnextBank(current, destination,this.topology,this.numberOfRows,this.numberOfColumns);
			break;
		case FATTREE:
			return routingAlgo.fatTreenextBank(current, destination,this.numberOfColumns);
		}
		if(selScheme == SELSCHEME.ADAPTIVE && choices.size()>1)
		{
			if(((Router)this.neighbours.elementAt(choices.elementAt(0).ordinal())).bufferSize()>
			   ((Router)this.neighbours.elementAt(choices.elementAt(1).ordinal())).bufferSize())
				return choices.elementAt(0);
			else
				return choices.elementAt(1);
		}
		return choices.elementAt(0);
	}
	
	/************************************************************************
     * Method Name  : handleEvent
     * Purpose      : handle the event request and service it
     * Parameters   : eventq and event id
     * Return       : void
     *************************************************************************/

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		RoutingAlgo.DIRECTION nextID;
		boolean reqOrReply;
		Vector<Integer> currentId = this.getBankId();
		Vector<Integer> destinationId = ((AddressCarryingEvent)(event)).getDestinationBankId();
		RequestType requestType = event.getRequestType();
		if((topology == TOPOLOGY.OMEGA || topology == TOPOLOGY.BUTTERFLY || topology == TOPOLOGY.FATTREE)
				&& !currentId.equals(destinationId))  //event passed to switch in omega/buttrfly/fat tree connection
		{
			this.connection[0].getPort().put(
					event.update(
							eventQ,
							0,	//this.getLatency()
							this, 
							this.connection[0],
							requestType));
		}
		else if(requestType == RequestType.Cache_Read)
		{
			requestType = RequestType.CacheBank_Read;

			if(this.AllocateBuffer(false))
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
				//System.out.println(event.getRequestingElement());
			}
		}
		else if(requestType == RequestType.Cache_Write)
		{
			requestType = RequestType.CacheBank_Write;
			if(this.AllocateBuffer(false))
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
			if(this.AllocateBuffer(false))
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
				//System.out.println(event.getRequestingElement());
			}
		}
		else if(requestType == RequestType.Mem_Response)
		{
			requestType = RequestType.MemBank_Response;
			if(this.AllocateBuffer(true))
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
				//System.out.println(event.getRequestingElement());
			}
		}
		else if(requestType == RequestType.Main_Mem_Read)
		{
			requestType = RequestType.Main_MemBank_Read;
			if(this.AllocateBuffer(true))
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
				//System.out.println(event.getRequestingElement());
			}
		}
		else if(requestType == RequestType.Main_Mem_Write)
		{
			requestType = RequestType.Main_MemBank_Write;
			if(this.AllocateBuffer(true))
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
				//System.out.println(event.getRequestingElement());
			}
		}
		else if(requestType == RequestType.Main_Mem_Response)
		{
			requestType = RequestType.Main_MemBank_Response;
			if(this.AllocateBuffer(true))
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
				//System.out.println(event.getRequestingElement());
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
			if(requestType == RequestType.Main_Mem_Read || requestType == RequestType.Main_Mem_Response ||
					requestType == RequestType.Main_Mem_Write || requestType == RequestType.Main_MemBank_Read  ||
					requestType == RequestType.Main_MemBank_Response || requestType == RequestType.Main_MemBank_Write ||
					requestType == RequestType.Mem_Response || requestType == RequestType.MemBank_Response)
				reqOrReply = true;  //for reply messages
			else
				reqOrReply = false; //for incoming messages
			if(this.CheckNeighbourBuffer(nextID,reqOrReply))
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