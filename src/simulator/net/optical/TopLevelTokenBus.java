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

package net.optical;

import java.util.Vector;

import config.NocConfig;

import memorysystem.AddressCarryingEvent;

import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class TopLevelTokenBus extends SimulationElement{
	
	public Vector<TokenBus> tokenbus;
	public int currentCluster;
	public int totalCluster;
	public Token token;
	public EntryPoint entryPoint;
	public EventQueue eq;
	public long frequency;
	
	public TopLevelTokenBus()
	{
		super(generic.PortType.Unlimited,-1,0,0,-100);
		this.frequency = -1;
	}
	
	public TopLevelTokenBus(NocConfig nocConfig, Vector<TokenBus> lowTokenBus, EntryPoint ePoint) {
		super(nocConfig.portType, nocConfig.getAccessPorts(), nocConfig.getPortOccupancy(), 
				nocConfig.getLatency(), nocConfig.operatingFreq);
		// TODO Auto-generated constructor stub
		
		this.tokenbus = lowTokenBus;
		this.currentCluster = 0;
		this.totalCluster = lowTokenBus.size();
		this.entryPoint = ePoint;
		eq = new EventQueue();
	}
	
	public void putToken(){
		this.getPort().put( new AddressCarryingEvent
				(eq,
				1,
				this,
				this,
				RequestType.TOKEN,
				0));
		int i=this.tokenbus.size();
		for(int j=0;j<i;j++){
			this.tokenbus.elementAt(j).getPort().put(new AddressCarryingEvent
					(eq, 
					1, 
					this, 
					this.tokenbus.elementAt(j),
					RequestType.LOCAL_TOKEN,
					0));
		}
	}
	
	public void setFrequency(long frequency){
		this.frequency = frequency;
	}
	
	public long getFrequency(){
		return this.frequency;
	}
	public void setParameters(NocConfig nocConfig, Vector<TokenBus> lowTokenBus, EntryPoint ePoint){
		
//		this.setPort(new Port(nocConfig.portType,nocConfig.getAccessPorts(),nocConfig.getPortOccupancy()));
		this.setLatency(nocConfig.getLatency());
		this.setFrequency(nocConfig.operatingFreq);
		this.tokenbus = lowTokenBus;
		this.currentCluster = 0;
		this.totalCluster = lowTokenBus.size();
		this.entryPoint = ePoint;
		eq = new EventQueue();
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		if(currentCluster == totalCluster){
			this.entryPoint.getPort().put(event.
					update(eq,
							1, 
							this, 
							this.entryPoint, 
							event.getRequestType()));
			currentCluster = 0;
		}
		else{
			tokenbus.elementAt(currentCluster).getPort().put(event.
					update(eq,
							1, 
							this, 
							this.tokenbus.elementAt(currentCluster), 
							RequestType.TOKEN));
			currentCluster = currentCluster + 1;
		}
	}
}
