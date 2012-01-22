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

import java.util.*;

import memorysystem.nuca.NucaCacheBank;

public class Router{
	
	protected Vector<Integer> bankId = new Vector<Integer>(2); //bank id of router(vector <row,column>)
	protected int availBuff;                                   //available number of buffer in router
	protected RoutingAlgo routingAlgo = new RoutingAlgo();
	protected Vector<NucaCacheBank> neighbours= new Vector<NucaCacheBank>(4);
				//0 - up ; 1 - right ; 2- down ; 3- left (clockwise) 
	
	public Router(Vector<Integer> bankId, int availBuff)
	{
		this.bankId = bankId;
		this.availBuff = availBuff;
	}
	
	public Vector<Integer> getBankId()
	{
		return this.bankId;
	}
	
	public boolean AllocateBuffer()  //TODO Check for mutual exclusion
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
		this.neighbours.add(dir.ordinal(), cacheBank);
	}
	public void SetConnectedBanks(RoutingAlgo.DIRECTION dir)
	{
		this.neighbours.add(dir.ordinal(), null);
	}
	public Vector<NucaCacheBank> GetNeighbours()
	{
		return this.neighbours;
	}
	
	public boolean CheckNeighbourBuffer(RoutingAlgo.DIRECTION nextId)  //request for neighbour buffer
	{
		return this.neighbours.elementAt(nextId.ordinal()).getRouter().AllocateBuffer();
	}
	
	public void FreeBuffer()
	{
		this.availBuff ++;
	}
	
	public RoutingAlgo.DIRECTION RouteComputation(Vector<Integer> current,
											Vector<Integer> destination, RoutingAlgo.ALGO algoType)
	{ 
		//find the route to go
		switch (algoType) {
		case WESTFIRST :
		break;
		case NORTHLAST : 
		break;
		case NEGATIVEFIRST :
		break;
		case TABLE :
		break;
		case SIMPLE :
			return routingAlgo.nextBank(current, destination);
		}
		return null;
	}	
}