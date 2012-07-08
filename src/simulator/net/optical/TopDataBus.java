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

public class TopDataBus extends SimulationElement {
	
	public Vector<DataBus> lowLevelData;
	public EntryPoint entryPoint;
	
	public TopDataBus(NocConfig nocConfig, Vector<DataBus> lowData) {
		super(nocConfig.portType, nocConfig.getAccessPorts(), nocConfig.getPortOccupancy(), 
				nocConfig.getLatency(), nocConfig.operatingFreq);
		this.lowLevelData = lowData;
	}
	public void setEntryPoint(EntryPoint ePoint){
		this.entryPoint = ePoint;
	}
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
		Vector<Integer> destinationBankId = ((AddressCarryingEvent) event).getDestinationBankId();
		RequestType requestType = event.getRequestType();
//		
//		if(((AddressCarryingEvent)event).getRequestType() == RequestType.Main_Mem_Read ||
//				((AddressCarryingEvent)event).getRequestType() == RequestType.Main_Mem_Write){
//			this.entryPoint.getPort().put(
//					((AddressCarryingEvent)event).update(eventQ, 
//							1, 
//							this,
//							this.entryPoint,
//							requestType));
//		}
//		//TODO
		if(requestType == RequestType.Mem_Response||
				requestType == RequestType.Main_Mem_Read ||
				requestType == RequestType.Main_Mem_Write){
			this.entryPoint.getPort().put(event.update(
					eventQ,
					1,
					this, 
					this.entryPoint,
					requestType));
			System.out.println("top data TO entryPoint Mem_Response  " + requestType + " "+ ((AddressCarryingEvent) event).getSourceBankId()+ " " +((AddressCarryingEvent) event).getDestinationBankId());
		}
		else
			this.lowLevelData.elementAt(destinationBankId.elementAt(1)).getPort().put(
															((AddressCarryingEvent)event).update(eventQ, 
															1, 
															this,
															this.lowLevelData.elementAt(destinationBankId.elementAt(1)),
															requestType));
		
	}

}
