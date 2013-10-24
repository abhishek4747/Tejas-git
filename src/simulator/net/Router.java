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
	protected RoutingAlgo routingAlgo = new RoutingAlgo();
	protected int numberOfRows;
	protected int numberOfColumns;
	protected NocInterface reference;
	protected int latencyBetweenBanks;
	protected Vector<Router> neighbours;
	
	/************************************************************************
     * Method Name  : Router
     * Purpose      : Constructor for Router class
     * Parameters   : bank id, noc configuration, cache bank reference
     * Return       : void
     *************************************************************************/
	
	public Router(NocConfig nocConfig, NocInterface reference)
	{
		super(nocConfig);
		this.topology = nocConfig.topology;
		this.rAlgo = nocConfig.rAlgo;
		this.numberOfRows = nocConfig.numberOfBankRows;
		this.numberOfColumns = nocConfig.numberOfBankColumns;
		this.reference = reference;
		this.latencyBetweenBanks = nocConfig.latencyBetweenBanks;
		this.neighbours= new Vector<Router>(4);
		this.hopCounters = 0;
	}
	/***************************************************
	 * Connects the banks
	 * @param dir
	 * @param networkElements
	 ***************************************************/
	public void SetConnectedBanks(RoutingAlgo.DIRECTION dir,NocInterface networkElements)
	{
		this.neighbours.add(dir.ordinal(), networkElements.getRouter());
	}
	/***************************************************
	 * Connects the banks
	 * @param dir
	 ***************************************************/
	public void SetConnectedBanks(RoutingAlgo.DIRECTION dir)
	{
		this.neighbours.add(dir.ordinal(), null);
	}
	public Vector<Router> GetNeighbours()
	{
		return this.neighbours;
	}
	/*****************************************************
	 * Check if the neighbour buffer has free entry
	 * reqOrReply is kept for future use
	 * @param nextId
	 * @param reqOrReply
	 * @return
	 *****************************************************/
	public boolean CheckNeighbourBuffer(RoutingAlgo.DIRECTION nextId,boolean reqOrReply)  //request for neighbour buffer
	{
		return ((Router) this.neighbours.elementAt(nextId.ordinal())).AllocateBuffer(nextId);
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
	
	public boolean reqOrReply(RequestType requestType){
		if(requestType == RequestType.Main_Mem_Read || requestType == RequestType.Main_Mem_Response ||
				requestType == RequestType.Main_Mem_Write || requestType == RequestType.Mem_Response)
			return true;  //for reply messages
		else
			return false; //for incoming messages
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
		Vector<Integer> currentId = this.reference.getId();
		Vector<Integer> destinationId = ((AddressCarryingEvent)(event)).getDestinationId();
		RequestType requestType = event.getRequestType();
		if((topology == TOPOLOGY.OMEGA || topology == TOPOLOGY.BUTTERFLY || topology == TOPOLOGY.FATTREE)
				&& !currentId.equals(destinationId))  //event passed to switch in omega/buttrfly/fat tree connection
		{
			this.hopCounters++;
			this.connection[0].getPort().put(
					event.update(
							eventQ,
							0,	//this.getLatency()
							this, 
							this.connection[0],
							requestType));
		}
		else if(currentId.equals(destinationId))                         //If this is the destination
		{
			this.reference.getPort().put(
					event.update(
							eventQ,
							0,
							this, 
							this.reference.getSimulationElement(),
							requestType));
			this.FreeBuffer();
		}
		else if(event.getRequestingElement().getClass() != Router.class){ //If this event is just entering NOC,
																			//then allocate buffer for it
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
		else
		{
			nextID = this.RouteComputation(currentId, destinationId);
			reqOrReply = reqOrReply(requestType);
			if(this.CheckNeighbourBuffer(nextID,reqOrReply))   //If buffer is available
																//forward the event
			{
				//post event to nextID
				this.hopCounters++;
				this.GetNeighbours().elementAt(nextID.ordinal()).getPort().put(
						event.update(
								eventQ,
								latencyBetweenBanks,        	//this.getLatency()
								this, 
								this.GetNeighbours().elementAt(nextID.ordinal()),
								requestType));
				this.FreeBuffer();
			}
			else                                               //If buffer is not available
			{													//keep the event here itself
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