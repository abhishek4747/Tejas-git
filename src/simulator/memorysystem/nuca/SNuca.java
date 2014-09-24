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

				Contributor: Anuj Arora
*****************************************************************************/
package memorysystem.nuca;

import java.util.Vector;

import main.ArchitecturalComponent;
import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import config.CacheConfig;
import net.ID;
import net.NOC.CONNECTIONTYPE;
import net.optical.TopLevelTokenBus;
import config.SystemConfig;

public class SNuca extends NucaCache
{
    public SNuca(CacheConfig cacheParameters, CoreMemorySystem containingMemSys, TopLevelTokenBus tokenbus,NucaType nucaType)
    {
    	super(cacheParameters, containingMemSys, tokenbus,nucaType);
    	makeCacheBanks(cacheParameters, containingMemSys, tokenbus,nucaType,this);
    }
    protected void makeCacheBanks(CacheConfig cacheParameters,CoreMemorySystem containingMemSys, TopLevelTokenBus tokenBus, NucaType nucaType, SNuca nucaCache)
   	{
       	int rows = SystemConfig.nocConfig.getNumberOfBankRows();
       	int cols = SystemConfig.nocConfig.getNumberOfBankColumns();
   		for(int i=0;i<rows;i++)
   		{
   			for(int j=0;j<cols;j++)
   			{
   				if(SystemConfig.nocConfig.nocElements.nocElementsLocations.get(i).get(j).equals("0"))
   				{
   					ID bankId = new ID(i,j);
   					cacheBank.add(new SNucaBank(bankId, cacheParameters, containingMemSys, this, nucaType));
   				}
   			}
   		}
   	}
    void putEventToRouter(AddressCarryingEvent addrEvent)
	{
		long address = addrEvent.getAddress();
		ID sourceId = getCoreId(addrEvent.coreId);
		ID destinationId = getBankId(address);
		if(accessedBankIds.get(destinationId)==null)
			accessedBankIds.put(destinationId,1);
		else
			accessedBankIds.put(destinationId,accessedBankIds.get(destinationId)+1);
		AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(addrEvent.getEventQ(),
											 0,ArchitecturalComponent.getCores()[addrEvent.coreId], 
											 ArchitecturalComponent.getCores()[addrEvent.coreId].getRouter(),
											 addrEvent.getRequestType(),
											 address,addrEvent.coreId,
											 sourceId,destinationId);
		
//		if(SystemConfig.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL) 
//		{
			ArchitecturalComponent.getCores()[addrEvent.coreId].getRouter().
			getPort().put(eventToBeSent);
//		}
	}
}		