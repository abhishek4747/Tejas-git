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

	Contributors:  Eldhose Peter, Rajshekar
*****************************************************************************/
package net;

import generic.CommunicationInterface;
import generic.EventQueue;
import generic.Port;
import generic.RequestType;
import generic.SimulationElement;

import java.util.Vector;

import memorysystem.AddressCarryingEvent;

import config.NocConfig;
import config.SystemConfig;
/*****************************************************
 * 
 * NocInterface to make the router generic
 *
 *****************************************************/
public class NocInterface extends CommunicationInterface{

	Router router;
	SimulationElement reference;
	Vector<Integer> id;
	public NocInterface(NocConfig nocConfig, SimulationElement ref) {
		super();
		reference = ref;
		this.router = new Router(SystemConfig.nocConfig, this);
	}
	
	@Override
	public void sendMessage(EventQueue eventQueue, int delay, RequestType reqType, long addr,
			int coreId, Vector<Integer> destinationId, SimulationElement source, SimulationElement destination, int core_num) {
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQueue,
				delay,
				source,
				this.getRouter(), 
				reqType, 
				addr,
				coreId,
				this.getId(),
				destinationId);
		this.getRouter().getPort().put(addressEvent);
		
	}
	
	public Router getRouter(){
		return this.router;
	}
	public void setId(Vector<Integer> id)
	{
		this.id = id;
	}
	public Vector<Integer> getId()
	{
		return id;
	}
	public SimulationElement getSimulationElement()
	{
		return this.reference;
	}
	
}
