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

import generic.CommunicationInterface;
import generic.Event;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;

import java.util.Vector;

import config.Interconnect;
import config.SystemConfig;

public class NocElementDummy extends SimulationElement
{
	/************************************************************************
     * Method Name  : NocElementDummy(Constructor)
     * Purpose      : This is a dummy NOC element. In order to make a topology uniform,
     * 				  we may have to insert some blank spaces in the matrix format of
     * 				  element(cache, core, memory controllers, directory) placing.
     *                In those blank spaces, we use the dummy NOC elements, which 
     *                won't affect the functionality.
     * Parameters   : Port type, number of ports, port occupancy, event queue, latency and
     * 				  frequency
     * Return       : void
     *************************************************************************/
	public NocElementDummy(PortType portType, int noOfPorts, long occupancy,
			EventQueue eq, long latency, long frequency) {
		super(PortType.Unlimited, 1, 1, null, 1, 1);
		if(SystemConfig.interconnect == Interconnect.Bus)
		{
			comInterface = new BusInterface(this);
		}
		else if(SystemConfig.interconnect == Interconnect.Noc)
		{
			comInterface = new NocInterface(SystemConfig.nocConfig, this);
		}
	}
	public CommunicationInterface comInterface;
	Vector<Integer> nocElementId;
	
	
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}
}
