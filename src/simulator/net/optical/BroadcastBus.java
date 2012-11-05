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

import config.NocConfig;
import generic.Event;
import generic.EventQueue;
import generic.SimulationElement;
import memorysystem.AddressCarryingEvent;
import memorysystem.nuca.NucaCacheBank;

public class BroadcastBus extends SimulationElement{
	
	public int totalBanks;
	public NucaCacheBank[][] banks;
	public int clusterId;
	public BroadcastBus(NocConfig nocConfig, int numBanks,NucaCacheBank[][] bankArray, int clusterId){
		super(nocConfig.portType,
				nocConfig.getAccessPorts(), 
				nocConfig.getPortOccupancy(),
				nocConfig.getLatency(),
				nocConfig.operatingFreq);
		totalBanks = numBanks;
		banks = bankArray;
		this.clusterId = clusterId;
	}
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		AddressCarryingEvent bCastEvent;
		for(int i=0;i<totalBanks;i++)
		{
			bCastEvent= (AddressCarryingEvent) ((AddressCarryingEvent)event).clone();
			banks[i][clusterId].getRouter().getPort().put(
					bCastEvent.update(
							eventQ,
							1,
							this, 
							banks[i][clusterId].getRouter(),
							bCastEvent.getRequestType()));
		}
		
	}
}
