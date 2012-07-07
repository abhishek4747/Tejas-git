package memorysystem.nuca;

import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

import java.util.Vector;
import net.NOC.TOPOLOGY;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.InstructionCache;
import memorysystem.nuca.NucaCache.NucaType;

public class Policy {
	AddressCarryingEvent updateEventOnMiss(EventQueue eventQ,AddressCarryingEvent event,NucaCacheBank cacheBank,NucaType nucaType,TOPOLOGY topology)
	{

		Vector<Integer> sourceBankId = null;
		Vector<Integer> destinationBankId = null;
		long address = event.getAddress();
		RequestType requestType = RequestType.Main_Mem_Read;
		if(nucaType == NucaType.S_NUCA)
		{			
			//Add the request to the outstanding request buffer
			int alreadyRequested = cacheBank.addOutstandingRequest(event, address);
			sourceBankId =new Vector<Integer>(event.getDestinationBankId());
			destinationBankId = new Vector<Integer>(event.getSourceBankId());
			//System.out.println("added a new event in bankid " + router.getBankId());
			if (alreadyRequested==0)
			{
				event.oldRequestType = event.getRequestType();
				event.requestTypeStack.push(event.getRequestType());
				AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																			 0,
																			 cacheBank, 
																			 cacheBank.getRouter(), 
																			 RequestType.Main_Mem_Read, 
																			 address,
																			 ((AddressCarryingEvent)event).coreId);
				addressEvent.setSourceBankId(sourceBankId);
				addressEvent.setDestinationBankId(destinationBankId);
				return addressEvent;
			}
			else if(alreadyRequested == 2)
			{
				SimulationElement requestingElement = ((AddressCarryingEvent)event).oldRequestingElement;
				if(requestingElement.getClass() == Cache.class)
				{
					if(!cacheBank.connectedMSHR.contains(((Cache)requestingElement).missStatusHoldingRegister))
						cacheBank.connectedMSHR.add(((Cache)requestingElement).missStatusHoldingRegister);
					if(((Cache)requestingElement).missStatusHoldingRegister.containsKey(address >> ((Cache)requestingElement).blockSizeBits) &&
							event.getRequestType() == RequestType.Cache_Read )
					{
						((Cache)requestingElement).missStatusHoldingRegister.get(address >> ((Cache)requestingElement).blockSizeBits).readyToProceed = true;
						//((Cache)requestingElement).missStatusHoldingRegister.get(address >> ((Cache)requestingElement).blockSizeBits).eventToForward = event;
					}
					else if(((AddressCarryingEvent)event).getRequestType() != RequestType.Cache_Write)
					{
						System.out.println("Outstanding Request in Memory System from policy line 59 " + (address >> ((Cache)requestingElement).blockSizeBits) + ((Cache)requestingElement).missStatusHoldingRegister + event.getRequestType());
						System.exit(1);
					}
				}
				else if(requestingElement.getClass() == InstructionCache.class)
				{
					if(!cacheBank.connectedMSHR.contains(((InstructionCache)requestingElement).missStatusHoldingRegister))
						cacheBank.connectedMSHR.add(((InstructionCache)requestingElement).missStatusHoldingRegister);
					if(((InstructionCache)requestingElement).missStatusHoldingRegister.containsKey(address >> ((InstructionCache)requestingElement).blockSizeBits) &&
							event.getRequestType() == RequestType.Cache_Read_from_iCache )
					{
						((InstructionCache)requestingElement).missStatusHoldingRegister.get(address >> ((InstructionCache)requestingElement).blockSizeBits).readyToProceed = true;
//						((InstructionCache)requestingElement).missStatusHoldingRegister.get(address >> ((InstructionCache)requestingElement).blockSizeBits).eventToForward = event;
					}
					else if(((AddressCarryingEvent)event).getRequestType() != RequestType.Cache_Write)
					{
						System.out.println("Outstanding Request in Memory System from policy line 75");
						System.exit(1);
					}
				} 
				return null;
			}
			else
			{
				return null;
			}
		} 
/*		else if(topology == TOPOLOGY.BUS || topology == TOPOLOGY.RING)
		{
			sourceBankId = new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
			destinationBankId = new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
			int id = destinationBankId.remove(1);
			if(cacheBank.isFirstLevel)
			{
				((AddressCarryingEvent)event).oldSourceBankId = (Vector<Integer>) ((AddressCarryingEvent)event).getSourceBankId().clone(); 
			}
			if(id == cacheBank.cacheBankColumns -1 )
			{
				int id1 = destinationBankId.remove(0);
				if(id1 == cacheBank.cacheBankRows -1)
				{
					return ((AddressCarryingEvent)event).updateEvent(eventQ, 
																    0,
																    cacheBank, 
																    cacheBank.getRouter(), 
																    RequestType.Main_Mem_Read, 
																    cacheBank.router.getBankId(),
																    event.oldSourceBankId);
				}
				else
				{
					destinationBankId.add(id1+1);
					destinationBankId.add(0);
					return event.updateEvent(eventQ, 
										    0,
										    cacheBank, 
										    cacheBank.getRouter(), 
										    event.getRequestType(), 
										    sourceBankId,
										    destinationBankId);
					
				}
			}
			else
			{
				destinationBankId.add(id +1);
				return event.updateEvent(eventQ, 
									    0,
									    cacheBank, 
									    cacheBank.getRouter(), 
									    event.getRequestType(), 
									    sourceBankId,
									    destinationBankId);
			
			}
		}
		else if(cacheBank.isLastLevel)
		{
			if(cacheBank.isFirstLevel)
			{
				boolean alreadyRequested = cacheBank.addtoForwardedRequests(event, address);
				if (!alreadyRequested)
				{
					sourceBankId =new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
					destinationBankId = sourceBankId;
					((AddressCarryingEvent)event).oldRequestType = event.getRequestType();
					event.oldRequestType = event.getRequestType();
					AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																				 0,
																				 cacheBank, 
																				 cacheBank.getRouter(), 
																				 RequestType.Main_Mem_Read, 
																				 address,
																				 ((AddressCarryingEvent)event).coreId);
					addressEvent.setSourceBankId(sourceBankId);
					addressEvent.setDestinationBankId(destinationBankId);
					return addressEvent;
				}
				else
				{
					return null;
				}

			}
			else
			{
				boolean alreadyRequested = cacheBank.addtoForwardedRequests(event, address);
				if (!alreadyRequested)
				{
					sourceBankId =new Vector<Integer>(event.getDestinationBankId());
					destinationBankId = event.oldSourceBankId;
					event.oldRequestType = event.getRequestType();
					AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																				 0,
																				 cacheBank, 
																				 cacheBank.getRouter(), 
																				 RequestType.Main_Mem_Read, 
																				 address,
																				 ((AddressCarryingEvent)event).coreId);
					addressEvent.setSourceBankId(sourceBankId);
					addressEvent.setDestinationBankId(destinationBankId);
					return addressEvent;
				}
				else
				{
					return null;
				}
				
			}
		}
		*/
		else
		{
			if(cacheBank.isFirstLevel)
			{
				((AddressCarryingEvent)event).oldSourceBankId = (Vector<Integer>) ((AddressCarryingEvent)event).getSourceBankId().clone(); 
			}
			else if(cacheBank.isLastLevel)
			{
				sourceBankId = new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
				destinationBankId = new Vector<Integer>(((AddressCarryingEvent)event).oldSourceBankId);
				event.oldRequestType = event.getRequestType();
				int alreadyRequested = cacheBank.addOutstandingRequest(event, address);
				if (alreadyRequested==0)
				{
					event.oldRequestType = event.getRequestType();
					event.requestTypeStack.push(event.getRequestType());
					AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																				 0,
																				 cacheBank, 
																				 cacheBank.getRouter(), 
																				 RequestType.Main_Mem_Read, 
																				 address,
																				 ((AddressCarryingEvent)event).coreId);
					addressEvent.setSourceBankId(sourceBankId);
					addressEvent.setDestinationBankId(destinationBankId);
					return addressEvent;
				}
				else if(alreadyRequested == 2)
				{
					SimulationElement requestingElement = ((AddressCarryingEvent)event).oldRequestingElement;
					if(requestingElement.getClass() == Cache.class)
					{
						if(!cacheBank.connectedMSHR.contains(((Cache)requestingElement).missStatusHoldingRegister))
							cacheBank.connectedMSHR.add(((Cache)requestingElement).missStatusHoldingRegister);
						if(((Cache)requestingElement).missStatusHoldingRegister.containsKey(address >> ((Cache)requestingElement).blockSizeBits) &&
								event.getRequestType() == RequestType.Cache_Read )
						{
							((Cache)requestingElement).missStatusHoldingRegister.get(address >> ((Cache)requestingElement).blockSizeBits).readyToProceed = true;
							//((Cache)requestingElement).missStatusHoldingRegister.get(address >> ((Cache)requestingElement).blockSizeBits).eventToForward = event;
						}
						else if(((AddressCarryingEvent)event).getRequestType() != RequestType.Cache_Write)
						{
							System.out.println("Outstanding Request in Memory System from policy line 232 " + (address >> ((Cache)requestingElement).blockSizeBits) + ((Cache)requestingElement).missStatusHoldingRegister + event.getRequestType());
							System.exit(1);
						}
					}
					else if(requestingElement.getClass() == InstructionCache.class)
					{
						if(!cacheBank.connectedMSHR.contains(((InstructionCache)requestingElement).missStatusHoldingRegister))
							cacheBank.connectedMSHR.add(((InstructionCache)requestingElement).missStatusHoldingRegister);
						if(((InstructionCache)requestingElement).missStatusHoldingRegister.containsKey(address >> ((InstructionCache)requestingElement).blockSizeBits) &&
								event.getRequestType() == RequestType.Cache_Read_from_iCache )
						{
							((InstructionCache)requestingElement).missStatusHoldingRegister.get(address >> ((InstructionCache)requestingElement).blockSizeBits).readyToProceed = true;
//							((InstructionCache)requestingElement).missStatusHoldingRegister.get(address >> ((InstructionCache)requestingElement).blockSizeBits).eventToForward = event;
						}
						else if(((AddressCarryingEvent)event).getRequestType() != RequestType.Cache_Write)
						{
							System.out.println("Outstanding Request in Memory System from policy line 248");
							System.exit(1);
						}
					} 
					return null;
				}
				else
				{
					return null;
				}
			}
			sourceBankId = new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
			destinationBankId = new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
			int id = destinationBankId.remove(0);
			destinationBankId.add(0,id +1);
			requestType = event.getRequestType();
			return event.updateEvent(eventQ,
									 cacheBank.getLatencyDelay(), 
									 cacheBank,
									 cacheBank.getRouter(),
									 requestType,
									 sourceBankId, 
									 destinationBankId);
		}
	}
	
	AddressCarryingEvent updateEventOnHit(EventQueue eventQ,AddressCarryingEvent event,
										  NucaCacheBank cacheBank,NucaType nucaType,TOPOLOGY topology)
	{
		//Just return the read block
		Vector<Integer> sourceBankId = new Vector<Integer>(event.getDestinationBankId());
		Vector<Integer> destinationBankId;
		RequestType requestType = RequestType.Mem_Response;
		if(nucaType == NucaType.S_NUCA)
		{
			destinationBankId= new Vector<Integer>(event.getSourceBankId());
		}
		/*else if(topology == TOPOLOGY.BUS || topology == TOPOLOGY.RING)
		{
			//give proper value to destination bank id
			destinationBankId = null;
		}*/
		else if(cacheBank.isFirstLevel)
		{
			destinationBankId = sourceBankId;
		}
		else
		{
			destinationBankId = new Vector<Integer>();
			destinationBankId.add(sourceBankId.get(0)-1);
			destinationBankId.add(sourceBankId.get(1));
			requestType = RequestType.COPY_BLOCK;
		}
		return event.updateEvent(eventQ,
								cacheBank.getLatencyDelay(),
								cacheBank,
								cacheBank.getRouter(),
								requestType,
								sourceBankId,
								destinationBankId);
	
	}
}
