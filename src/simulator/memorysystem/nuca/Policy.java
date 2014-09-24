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

				Contributor: Anuj Arora
*****************************************************************************/
package memorysystem.nuca;

import generic.RequestType;
import java.util.Vector;

import net.NocInterface;

import config.SystemConfig;
import memorysystem.AddressCarryingEvent;

public class Policy {
	NucaCache nucaCache;
	Policy(NucaCache nucaCache)
	{
		this.nucaCache = nucaCache;
	}
	AddressCarryingEvent updateEventOnMiss(AddressCarryingEvent event,NucaCacheBank cacheBank)
	{
		if(event.getSourceId() == null || event.getDestinationId() == null)
		{
			misc.Error.showErrorAndExit(" source bank  id or destination bank id null ");
		}
		Vector<Integer> destinationId = (Vector<Integer>) SystemConfig.nocConfig.nocElements.
				getMemoryControllerId(((NocInterface) cacheBank.comInterface).getId());
		//System.err.println("In SNucaPolicy Address : "+ event.getAddress() +destinationBankId);
		AddressCarryingEvent addressEvent = event.updateEvent(event.getEventQ(),
											0,cacheBank, cacheBank.getRouter(), 
											RequestType.Main_Mem_Read,
											((NocInterface) cacheBank.comInterface).getId(),
											destinationId);
		return addressEvent;
	}

	protected void sendResponseToCore(AddressCarryingEvent addrEvent, NucaCacheBank cacheBank)
	{ 
		//System.err.println("sendResponseToCore");
		Vector<Integer> destination = nucaCache.getCoreId(addrEvent.coreId);
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(addrEvent.getEventQ(),
											 0,cacheBank, cacheBank.getRouter(), 
											 RequestType.Mem_Response, 
											 addrEvent.getAddress(),addrEvent.coreId,
											 ((NocInterface) cacheBank.comInterface).getId(),destination);
		cacheBank.getRouter().getPort().put(addressEvent);
	}
	
	void broadcastToOtherBanks(AddressCarryingEvent addrEvent, long address,NucaCacheBank cacheBank)
	{
		int bankset = ((DNuca)nucaCache).getBankSetId(address);
		Vector<Integer> sourceId = ((NocInterface) cacheBank.comInterface).getId();
		Vector<Vector<Integer>> bankIds = ((DNuca)nucaCache).bankSetNumToBankIds.get(
											((DNuca)nucaCache).bankSetnum.get(bankset));
		DNuca.eventId++;
		//System.err.println(DNuca.eventId);
		if(DNuca.eventId==Long.MAX_VALUE)
			DNuca.eventId=1;
		Vector<RequestType> temp = new Vector<RequestType>();
		temp.add(RequestType.Cache_Miss);
		((DNucaBank)cacheBank).eventIdToHitMissList.put(DNuca.eventId, temp);
		for(Vector<Integer> i:bankIds)
		{
			if(!(((NocInterface) cacheBank.comInterface).getId().get(0)==i.get(0) && 
					((NocInterface) cacheBank.comInterface).getId().get(1)==i.get(1)))
			{
				Vector<Integer> destinationId = i;
				AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(DNuca.eventId,addrEvent.getEventQ(),
						 0,cacheBank, 
						 cacheBank.getRouter(),
						 addrEvent.getRequestType(),
						 address,addrEvent.coreId,
						 sourceId,destinationId);
				cacheBank.getRouter().getPort().put(eventToBeSent);
			}
		}
	}
}
