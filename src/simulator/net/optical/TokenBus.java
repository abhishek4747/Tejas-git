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
import memorysystem.AddressCarryingEvent;
import memorysystem.nuca.NucaCacheBank;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;
import net.optical.OpticalRouter;

public class TokenBus extends SimulationElement{
	
	public Token token;
	public int tokenHopTime;
	public int clusterId;
	public int currentId;
	public int currentIdLocal;
	public int totalBanks;
	public NucaCacheBank[][] banks;       //set from ConnectBanks() : fun call from NucaCache
	public TopLevelTokenBus topTokenBus;  //                        ,,
	
	public TokenBus(NocConfig nocConfig, NucaCacheBank cacheBanks[][], int numBanks, int cId) {
		super(nocConfig.portType, nocConfig.getAccessPorts(), nocConfig.getPortOccupancy(), 
				nocConfig.getLatency(), nocConfig.operatingFreq);
		// TODO Auto-generated constructor stub
		currentId = 0;
		currentIdLocal = 0;
		banks = cacheBanks;
		totalBanks = numBanks;
		clusterId = cId;
	}
	
	public void checkNodeIfNeeded(){
		
	}
	public void giveToken(Token token){
		
	}
	public void receiveToken(Token token){
		
	}
	
	public boolean availToken()
	{
		return token.free;
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		int flag = 0;
		if(((AddressCarryingEvent)event).getRequestType() == RequestType.TOKEN)
		{
			if(currentId == totalBanks)
			{
				currentId = 0;
				topTokenBus.getPort().put(event.
						update(topTokenBus.eq,
								1, 
								this, 
								this.topTokenBus, 
								RequestType.TOKEN));
			}
			else 
			{
				int i;
				for(i=currentId; i<totalBanks; i++)
				{
					if(((OpticalRouter)banks[i][clusterId].getRouter()).readyToSend){
						banks[clusterId][i].getRouter().getPort().put(event.
								update(topTokenBus.eq,
										0, 
										this, 
										this.banks[i][clusterId].getRouter(), 
										RequestType.TOKEN));
						flag = 1;
						currentId = i+1;
						break;
						
					}
				}
				if(flag == 0)
				{
					currentId = 0;
					this.getPort().put(event.
							update(topTokenBus.eq,
									1, 
									this, 
									this.topTokenBus, 
									RequestType.TOKEN));
				}
			}
		}
		else
		{
			flag =0;
			for(int j = currentIdLocal; j< totalBanks; j++)
			{
				if(((OpticalRouter)banks[j][clusterId].getRouter()).readyToSendLocally){
					banks[clusterId][j].getRouter().getPort().put(event.
							update(topTokenBus.eq,
									0, 
									this, 
									this.banks[j][clusterId].getRouter(), 
									RequestType.LOCAL_TOKEN));
					currentIdLocal = (j+1)% totalBanks;
					flag =1;
					break;
				}
			}
			if(flag == 0){
				currentIdLocal = 0 ;
				this.getPort().put(event.
						update(topTokenBus.eq,
								1,
								this,
								this, 
								RequestType.LOCAL_TOKEN));
			}
		}
	}
}
