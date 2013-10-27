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
		Vector<Integer> destinationId = (Vector<Integer>) nucaCache.getMemoryControllerId(cacheBank.getBankId());
		//System.err.println("In SNucaPolicy Address : "+ event.getAddress() +destinationBankId);
		AddressCarryingEvent addressEvent = event.updateEvent(event.getEventQ(),
											0,cacheBank, cacheBank.getRouter(), 
											RequestType.Main_Mem_Read,
											cacheBank.getBankId(),
											destinationId);
		return addressEvent;
	}

	void updateEventOnHit(AddressCarryingEvent event,
										  NucaCacheBank cacheBank)
	{
		sendResponseToWaitingEvent(event,cacheBank);
	}
	
	
	protected void sendResponseToWaitingEvent(AddressCarryingEvent event, NucaCacheBank cacheBank)
	{ 
		event.setRequestingElement(nucaCache);
		cacheBank.sendMemResponse(event);//sending response to NucaCache controller
	}
	
	void broadcastToOtherBanks(AddressCarryingEvent addrEvent, long address,NucaCacheBank cacheBank)
	{
		int bankset = ((DNuca)nucaCache).getBankSetId(address);
		Vector<Integer> sourceId = cacheBank.bankId;
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
			if(!(cacheBank.bankId.get(0)==i.get(0) && cacheBank.bankId.get(1)==i.get(1)))
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
