package memorysystem.nuca;
import generic.RequestType;
import java.util.Vector;
import config.SimulationConfig;
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
		Vector<Integer> sourceBankId = null;
		Vector<Integer> destinationBankId = null;
		long address = event.getAddress();
		RequestType requestType = RequestType.Main_Mem_Read;
		NucaType nucaType = SimulationConfig.nucaType;
		if( nucaType == NucaType.S_NUCA )
		{			
			if(event.getSourceBankId() == null || event.getDestinationBankId() == null)
			{
				misc.Error.showErrorAndExit(" source bank  id or destination bank id null ");
			}
			sourceBankId = new Vector<Integer>(cacheBank.getBankId());
			destinationBankId = (Vector<Integer>) nucaCache.getDestinationBankId(event.getAddress(), event.coreId);
			AddressCarryingEvent addressEvent = new AddressCarryingEvent(event.getEventQ(),
																		 0,  cacheBank, cacheBank.getRouter(), 
																		 RequestType.Main_Mem_Read, address,
																		 ((AddressCarryingEvent)event).coreId,
																		 sourceBankId, destinationBankId);
			return addressEvent;
		} 
		else if(nucaType == NucaType.CB_D_NUCA)
		{
			int setIndex = nucaCache.getSetIndex(address);
			if(SimulationConfig.broadcast)
			{
				int index = ((CBDNuca)nucaCache).bankIdtoIndex(event.coreId,setIndex,cacheBank.bankId);
				if(index == nucaCache.cacheMapping.get(event.coreId).get(setIndex).size() -1 )
				{
					sourceBankId = new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
					destinationBankId = (Vector<Integer>) nucaCache.integerToBankId(nucaCache.cacheMapping.get(event.coreId).get(setIndex).get(0)).clone();
					//System.out.println("cache Miss  sending request to Main Memory"+destinationBankId + " to event"+ event);
					AddressCarryingEvent addressEvent = new AddressCarryingEvent(event.getEventQ(),
																				 0,cacheBank, cacheBank.getRouter(), 
																				 RequestType.Main_Mem_Read, 
																				 address,((AddressCarryingEvent)event).coreId,
																				 sourceBankId,destinationBankId);
					//System.out.println("sent mem request from bank " + destinationBankId + "for event " + addressEvent);
					return addressEvent;
				} else {
					return null;
				}
			}
			else
			{
				int index = ((CBDNuca)nucaCache).bankIdtoIndex(event.coreId,setIndex,cacheBank.bankId);
				if(index == nucaCache.cacheMapping.get(event.coreId).get(setIndex).size() -1 )
				{
					sourceBankId = new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
					destinationBankId = (Vector<Integer>) nucaCache.integerToBankId(nucaCache.cacheMapping.get(event.coreId).get(setIndex).get(0)).clone();
					//System.out.println("cache Miss  sending request to Main Memory"+destinationBankId + " to event"+ event);
					AddressCarryingEvent addressEvent = new AddressCarryingEvent(event.getEventQ(),
																				 0,cacheBank, cacheBank.getRouter(), 
																				 RequestType.Main_Mem_Read, 
																				 address,((AddressCarryingEvent)event).coreId,
																				 sourceBankId,destinationBankId);
					return addressEvent;
				}
				else
				{
					sourceBankId = (Vector<Integer>) cacheBank.getBankId().clone();
					destinationBankId = nucaCache.integerToBankId(nucaCache.cacheMapping.get(event.coreId).get(setIndex).get(index +1));
					//System.out.println("cache Miss  sending request to cache bank"+destinationBankId + " to event"+ event);
					requestType = event.getRequestType();
					return event.updateEvent(event.getEventQ(), 
									  		  0,cacheBank, cacheBank.router, 
									  		  requestType, sourceBankId,
									  		  destinationBankId);
				}
			}
		}
		else if(nucaType == NucaType.D_NUCA)
		{
			if(cacheBank.isLastLevel)
			{
				sourceBankId = new Vector<Integer>(cacheBank.getBankId());
				destinationBankId = (Vector<Integer>) nucaCache.getDestinationBankId(event.getAddress(), event.coreId);
				AddressCarryingEvent addressEvent = new AddressCarryingEvent(event.getEventQ(),
																			 0,  cacheBank, cacheBank.getRouter(), 
																			 RequestType.Main_Mem_Read, address,
																			 ((AddressCarryingEvent)event).coreId,
																			 sourceBankId, destinationBankId);
				return addressEvent;
			}
			else
			{
				sourceBankId = new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
				destinationBankId = new Vector<Integer>();
				destinationBankId.add(sourceBankId.get(0)+1);
				destinationBankId.add(sourceBankId.get(1));
				requestType = event.getRequestType();
				return event.updateEvent(event.getEventQ(), cacheBank.getLatencyDelay(), 
										 									cacheBank, cacheBank.getRouter(), requestType,
										 									sourceBankId, destinationBankId);
			}
		}
		else 
		{
			misc.Error.showErrorAndExit(" unrecognized request type of event ");
			return null;
		}
	}
	
	private Vector<Integer> getDestinationBankId(AddressCarryingEvent event,NucaCacheBank cacheBank)
	{
		Vector<Integer> sourceBankId = new Vector<Integer>(cacheBank.getBankId());
		Vector<Integer> destinationBankId = null;
		NucaType nucaType = SimulationConfig.nucaType;
		if(nucaType == NucaType.S_NUCA)
		{
			destinationBankId = null;
		}
		else if(nucaType == NucaType.CB_D_NUCA)
		{
			long address = event.getAddress();
			int setIndex = nucaCache.getSetIndex(address);
			int bankIndex = getBankIndex(cacheBank.bankId, setIndex, event.coreId);
			if(bankIndex != 0)
			{
				destinationBankId = nucaCache.integerToBankId(nucaCache.cacheMapping.get(event.coreId).get(setIndex).get(bankIndex -1 ));
			}
			else 
			{
				destinationBankId = null;
			}
		}
		else if(nucaType == NucaType.D_NUCA)
		{
			if(cacheBank.isFirstLevel)
			{
				destinationBankId = null;
			}
			else
			{
				destinationBankId = new Vector<Integer>();
				destinationBankId.add(sourceBankId.get(0)-1);
				destinationBankId.add(sourceBankId.get(1));
			}
		}
		return destinationBankId;
	}
	
	private RequestType getRequestType(NucaCacheBank cacheBank,AddressCarryingEvent event)
	{
		NucaType nucaType = SimulationConfig.nucaType;
		RequestType requestType = RequestType.Mem_Response;
		int setIndex = nucaCache.getSetIndex(event.getAddress());
		if( ( nucaType == NucaType.CB_D_NUCA && getBankIndex(event.getDestinationBankId(), setIndex, event.coreId) != 0 )||
			 ( nucaType == NucaType.D_NUCA && !cacheBank.isFirstLevel) )
		{
				requestType= RequestType.COPY_BLOCK;
		}
		return requestType;
	}
	
	void updateEventOnHit(AddressCarryingEvent event,
										  NucaCacheBank cacheBank)
	{
		/*if(SimulationConfig.nucaType == NucaType.CB_D_NUCA && SimulationConfig.broadcast)
		{
			Vector<Integer> sourceBankId = new Vector<Integer>(event.getDestinationBankId());
			Vector<Integer> destinationBankId = new Vector<Integer>(event.oldSourceBankId);
			event.updateEvent(event.getEventQ(), 0, cacheBank, cacheBank.getRouter(), 
							  RequestType.Cache_Hit,sourceBankId,destinationBankId);
			cacheBank.getRouter().getPort().put(event);
		}
		else*/
		{
			//System.out.println("cache Hit  for to event"+ event + event.getDestinationBankId());
			sendResponseToWaitingEvent(event,cacheBank,true);
		}
	}
	
	private int getBankIndex(Vector<Integer> bankId,int setNumber,int coreId)
	{
		int bankNumber = nucaCache.cacheColumns*bankId.get(0) + bankId.get(1);

		for(int i=0;i<nucaCache.cacheMapping.get(coreId).get(setNumber).size(); i++ )
		{
			 if(nucaCache.cacheMapping.get(coreId).get(setNumber).get(i) == bankNumber )
				 return i;
		}
		return -1;
	}
	
	protected void sendResponseToWaitingEvent(AddressCarryingEvent event, NucaCacheBank cacheBank,boolean flag)
	{ 
		RequestType requestType; 
		Vector<Integer> destinationBankId = null;
		if(flag && (requestType = getRequestType(cacheBank, event)) == RequestType.COPY_BLOCK 
				&& (destinationBankId = getDestinationBankId(event, cacheBank))!=null)
		{
			AddressCarryingEvent addrEvent = new AddressCarryingEvent(event.getEventQ(),
												0,cacheBank,cacheBank.getRouter(),
												requestType,event.getAddress(),event.coreId,
												cacheBank.getBankId(),destinationBankId);
			cacheBank.getRouter().getPort().put(addrEvent);
		}
		event.setRequestingElement(nucaCache);
		cacheBank.sendMemResponse(event);
	}
}
