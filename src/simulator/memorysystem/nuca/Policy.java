package memorysystem.nuca;

import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

import java.util.Vector;

import config.SimulationConfig;
import net.NOC.TOPOLOGY;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.InstructionCache;
import memorysystem.nuca.NucaCache.NucaType;

public class Policy {
	NucaCache nucaCache;
	Policy(NucaCache nucaCache)
	{
		this.nucaCache = nucaCache;
	}
	AddressCarryingEvent updateEventOnMiss(EventQueue eventQ,AddressCarryingEvent event,NucaCacheBank cacheBank,TOPOLOGY topology)
	{

		Vector<Integer> sourceBankId = null;
		Vector<Integer> destinationBankId = null;
		long address = event.getAddress();
		RequestType requestType = RequestType.Main_Mem_Read;
		NucaType nucaType = SimulationConfig.nucaType;
		if(nucaType == NucaType.S_NUCA)
		{			
			//Add the request to the outstanding request buffer
			int alreadyRequested = cacheBank.addOutstandingRequest(event, address);
			sourceBankId =new Vector<Integer>(event.getDestinationBankId());
			destinationBankId = new Vector<Integer>(event.getSourceBankId());
			//System.out.println("added a new event in bankid " + router.getBankId());
			if (alreadyRequested==0)
			{
				//System.out.println("outstanding request size "+ cacheBank.missStatusHoldingRegister.get((address >>> cacheBank.blockSizeBits)).outStandingEvents.size() + "address " + (address >> cacheBank.blockSizeBits) + destinationBankId + sourceBankId  + " core Id "+ event.coreId + "event time " + event.getEventTime());
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
		else if(nucaType == NucaType.CB_D_NUCA)
		{
			int setIndex = nucaCache.getSetIndex(address);
			if(event.index == nucaCache.cacheMapping.get(event.coreId).get(setIndex).size())
			{
				sourceBankId = new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
				destinationBankId = new Vector<Integer>(((AddressCarryingEvent)event).oldSourceBankId);
				event.oldRequestType = event.getRequestType();
				//nucaCache.printMshrStatus(cacheBank);
				int alreadyRequested = cacheBank.addOutstandingRequest(event, address);
				//nucaCache.printMshrStatus(cacheBank);
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
			else
			{
				if(event.index == 0)
				{
					event.oldSourceBankId = nucaCache.integerToBankId(nucaCache.cacheMapping.get(event.coreId).get(setIndex).get(event.index));
					sourceBankId = nucaCache.integerToBankId(nucaCache.cacheMapping.get(event.coreId).get(setIndex).get(event.index));
				}else
				{
					sourceBankId = (Vector<Integer>) event.getDestinationBankId().clone();
				}
				destinationBankId = nucaCache.integerToBankId(nucaCache.cacheMapping.get(event.coreId).get(setIndex).get(event.index));
				requestType = event.getRequestType();
				event.index++;
				return event.updateEvent(eventQ, 
								  0, 
								  cacheBank, 
								  cacheBank.router, 
								  requestType, 
								  address);
			}
			
		}
		else //if(nucaType == NucaType.D_NUCA)
		{
			if(cacheBank.isFirstLevel)
			{
				((AddressCarryingEvent)event).oldSourceBankId = (Vector<Integer>) ((AddressCarryingEvent)event).getSourceBankId().clone(); 
			}
			if(cacheBank.isLastLevel)
			{
				sourceBankId = new Vector<Integer>(cacheBank.getRouter().getBankId());
				destinationBankId = new Vector<Integer>();
				destinationBankId.add(0);
				destinationBankId.add(sourceBankId.get(1));
				event.oldRequestType = event.getRequestType();
				//System.out.println("before calling ");
				//nucaCache.printMshrStatus(cacheBank);
				int alreadyRequested = cacheBank.addOutstandingRequest(event, address);
				
				if (alreadyRequested==0)
				{
					//System.out.println("after calling and change is there");
					//nucaCache.printMshrStatus(cacheBank);
					event.oldRequestType = event.getRequestType();
					event.requestTypeStack.push(event.getRequestType());
					//System.out.println("outstanding request size "+ cacheBank.missStatusHoldingRegister.get((address >>> cacheBank.blockSizeBits)).outStandingEvents.size() + "address " + (address >> cacheBank.blockSizeBits)  +destinationBankId + sourceBankId + "this " + cacheBank.getRouter().getBankId() + " core Id "+ event.coreId + "event time " + event.getEventTime());
					//System.out.println("event destination bank id "+ event.getDestinationBankId() + " event source bank id " + event.getSourceBankId());
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
					//System.out.println("after calling slight change");
					//nucaCache.printMshrStatus(cacheBank);
					return null;
				}
			}
			else
			{
				sourceBankId = new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
				destinationBankId = new Vector<Integer>();
				destinationBankId.add(sourceBankId.get(0)+1);
				destinationBankId.add(sourceBankId.get(1));
				//int id = destinationBankId.remove(0);
				//destinationBankId.add(0,id +1);
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
	}
	
	AddressCarryingEvent updateEventOnHit(EventQueue eventQ,AddressCarryingEvent event,
										  NucaCacheBank cacheBank,TOPOLOGY topology)
	{
		//Just return the read block
		Vector<Integer> sourceBankId = new Vector<Integer>(event.getDestinationBankId());
		Vector<Integer> destinationBankId;
		RequestType requestType = RequestType.Mem_Response;
		NucaType nucaType = SimulationConfig.nucaType;
		if(nucaType == NucaType.S_NUCA)
		{
			destinationBankId= new Vector<Integer>(event.getSourceBankId());
		}
		/*else if(topology == TOPOLOGY.BUS || topology == TOPOLOGY.RING)
		{
			//give proper value to destination bank id
			destinationBankId = null;
		}*/
		if(nucaType == NucaType.CB_D_NUCA)
		{
			long address = event.getAddress();
			int setIndex = nucaCache.getSetIndex(address);
			if(event.index != 0)
			{
				destinationBankId = nucaCache.integerToBankId(nucaCache.cacheMapping.get(event.coreId).get(setIndex).get(event.index -1 ));
			}
			else 
				destinationBankId = sourceBankId;
		}
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
