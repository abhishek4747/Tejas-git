package memorysystem.nuca;
import generic.EventQueue;
import generic.GlobalClock;
import generic.RequestType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import config.SimulationConfig;
import config.SystemConfig;
import main.ArchitecturalComponent;
import memorysystem.AddressCarryingEvent;
import memorysystem.nuca.NucaCache.NucaType;

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
		cacheBank.sendMemResponse(event);
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
						 0,SystemConfig.nocConfig.nocElements.nocElements[sourceId.get(0)][sourceId.get(1)].getSimulationElement(), 
						 SystemConfig.nocConfig.nocElements.nocElements[destinationId.get(0)][destinationId.get(1)].getSimulationElement(),
						 addrEvent.getRequestType(),
						 address,addrEvent.coreId,
						 sourceId,destinationId);
				cacheBank.getRouter().getPort().put(eventToBeSent);
			}
		}
	}
}
