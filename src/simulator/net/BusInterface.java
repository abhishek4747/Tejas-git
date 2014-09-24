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

import java.util.Vector;

import memorysystem.AddressCarryingEvent;

import generic.CommunicationInterface;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class BusInterface extends CommunicationInterface{

	SimulationElement reference; //Connection to the simulation element which contains this interface.
	
	public BusInterface(SimulationElement reference) {
		super();
		this.reference = reference;
	}
	/*
	 * Messages are coming from simulation elements(cores, cache banks) in order to pass it to another
	 * through electrical snooping Bus.
	 */
	@Override
	public void sendMessage(EventQueue eventQueue, int delay,
			RequestType reqTye, long addr, int coreId,
			Vector<Integer> destinationId, SimulationElement source,
			SimulationElement destination, int core_num) {
		destination.getPort().put( //Put event to the destination
				new AddressCarryingEvent(
					eventQueue,
					delay,
					source, 
					destination,
					reqTye, 
					addr,
					core_num));		
	}
}
