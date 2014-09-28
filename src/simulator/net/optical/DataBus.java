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

import memorysystem.SignalWavelengthEvent;
import memorysystem.nuca.NucaCacheBank;
import config.NocConfig;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class DataBus extends SimulationElement {
	
	public DataBus(NocConfig nocConfig, int numBanks, NucaCacheBank[][] cacheBank, int clusterId) {
		super(nocConfig.portType, nocConfig.getAccessPorts(), nocConfig.getPortOccupancy(), 
				nocConfig.getLatency(), nocConfig.operatingFreq);
		this.cacheBank = cacheBank;
		this.totalBanks = numBanks;
		this.clusterId = clusterId;
		wavelengths = new Integer[numBanks];
		for(int i = 0;i<numBanks ; i++)
			wavelengths[i] = i;
	}

	
	public int clusterId;
	public int totalBanks;
	public Integer[] wavelengths;
	public TopDataBus topDataBus;
	public NucaCacheBank[][] cacheBank;
	
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
//		RequestType requestType = event.getRequestType();
//		if(event.getClass() == SignalWavelengthEvent.class && ((SignalWavelengthEvent) event).getWavelength() == -1){
//		this.topDataBus.getPort().put(event.update(
//					eventQ,
//					1,
//					this, 
//					this.topDataBus,
//					requestType));
//		}
//
//		
//		
//		else if(clusterId == (int)(((SignalWavelengthEvent) event).getWavelength()/this.totalBanks)) 
//			this.cacheBank[(int)(((SignalWavelengthEvent) event).getWavelength()%this.totalBanks)][clusterId].getRouter().getPort().put(event.
//					update(
//						eventQ,
//						1,
//						this, 
//						this.cacheBank[(int)(((SignalWavelengthEvent) event).getWavelength()%this.totalBanks)][clusterId].getRouter(),
//						requestType));
//		else{
//			this.topDataBus.getPort().put(event.update(
//					eventQ,
//					1,
//					this, 
//					this.topDataBus,
//					requestType));
//		}
//		
	}
}
