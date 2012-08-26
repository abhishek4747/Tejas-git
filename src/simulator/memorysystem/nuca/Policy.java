package memorysystem.nuca;
import generic.Event;
import generic.RequestType;
import java.util.ArrayList;
import java.util.Vector;
import config.CacheConfig;
import config.SimulationConfig;
import memorysystem.AddressCarryingEvent;
import memorysystem.CacheLine;
import memorysystem.MESI;
import memorysystem.nuca.NucaCache.NucaType;

public class Policy {
	NucaCache nucaCache;
	Policy(NucaCache nucaCache)
	{
		this.nucaCache = nucaCache;
	}
	AddressCarryingEvent updateEventOnMiss(AddressCarryingEvent event,NucaCacheBank cacheBank)
	{
		//System.out.println("inside update event on miss : NucaType " + SimulationConfig.nucaType);
		Vector<Integer> sourceBankId = null;
		Vector<Integer> destinationBankId = null;
		long address = event.getAddress();
		RequestType requestType = RequestType.Main_Mem_Read;
		NucaType nucaType = SimulationConfig.nucaType;
		if( nucaType == NucaType.S_NUCA )
		{			
			if(event.getSourceBankId() == null || event.getDestinationBankId() == null)
			{
				System.out.println(" bank id null ");
			}
			sourceBankId =new Vector<Integer>(event.getDestinationBankId());
			destinationBankId = new Vector<Integer>(event.getSourceBankId());
			//System.out.println("from updateeventOnMiss destination bank id" + destinationBankId + " source bank id" + sourceBankId);
			AddressCarryingEvent addressEvent = new AddressCarryingEvent(event.getEventQ(),
																		 0,
																		 cacheBank, 
																		 cacheBank.getRouter(), 
																		 requestType, 
																		 address,
																		 event.coreId,
																		 sourceBankId,
																		 destinationBankId);
			return addressEvent;
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
				//int alreadyRequested = cacheBank.addOutstandingRequest(event, address);
				//nucaCache.printMshrStatus(cacheBank);
				//if (alreadyRequested==0)
				{
					event.oldRequestType = event.getRequestType();
					AddressCarryingEvent addressEvent = new AddressCarryingEvent(event.getEventQ(),
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
				/*else
				{
					return null;
				}*/
			}
			else
			{
				if(event.index == 0)
				{
					event.oldSourceBankId = nucaCache.integerToBankId(nucaCache.cacheMapping.get(event.coreId).get(setIndex).get(event.index));
					sourceBankId = nucaCache.integerToBankId(nucaCache.cacheMapping.get(event.coreId).get(setIndex).get(event.index));
				}
				else
				{
					sourceBankId = (Vector<Integer>) event.getDestinationBankId().clone();
				}
				destinationBankId = nucaCache.integerToBankId(nucaCache.cacheMapping.get(event.coreId).get(setIndex).get(event.index));
				requestType = event.getRequestType();
				event.index++;
				return event.updateEvent(event.getEventQ(), 
								  0, 
								  cacheBank, 
								  cacheBank.router, 
								  requestType, 
								  sourceBankId,
								  destinationBankId);
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
				
//				if (alreadyRequested==0)
				{
					//System.out.println("after calling and change is there");
					//nucaCache.printMshrStatus(cacheBank);
					event.oldRequestType = event.getRequestType();
					//System.out.println("outstanding request size "+ cacheBank.missStatusHoldingRegister.get((address >>> cacheBank.blockSizeBits)).outStandingEvents.size() + "address " + (address >> cacheBank.blockSizeBits)  +destinationBankId + sourceBankId + "this " + cacheBank.getRouter().getBankId() + " core Id "+ event.coreId + "event time " + event.getEventTime());
					//System.out.println("event destination bank id "+ event.getDestinationBankId() + " event source bank id " + event.getSourceBankId());
					AddressCarryingEvent addressEvent = new AddressCarryingEvent(event.getEventQ(),
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
/*				else
				{
					//System.out.println("after calling slight change");
					//nucaCache.printMshrStatus(cacheBank);
					return null;
				}
*/			}
			else
			{
				sourceBankId = new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
				destinationBankId = new Vector<Integer>();
				destinationBankId.add(sourceBankId.get(0)+1);
				destinationBankId.add(sourceBankId.get(1));
				//int id = destinationBankId.remove(0);
				//destinationBankId.add(0,id +1);
				requestType = event.getRequestType();
				return event.updateEvent(event.getEventQ(),
										 cacheBank.getLatencyDelay(), 
										 cacheBank,
										 cacheBank.getRouter(),
										 requestType,
										 sourceBankId, 
										 destinationBankId);
			}
		}
	}
	
	private Vector<Integer> getDestinationBankId(AddressCarryingEvent event,NucaCacheBank cacheBank)
	{
		Vector<Integer> sourceBankId = new Vector<Integer>(cacheBank.getRouter().getBankId());
		Vector<Integer> destinationBankId = null;
		NucaType nucaType = SimulationConfig.nucaType;
		if(nucaType == NucaType.S_NUCA)
		{
			destinationBankId= new Vector<Integer>(event.getSourceBankId());
		}
		else if(nucaType == NucaType.CB_D_NUCA)
		{
			long address = event.getAddress();
			int setIndex = nucaCache.getSetIndex(address);
			if(event.index != 0)
			{
				destinationBankId = nucaCache.integerToBankId(nucaCache.cacheMapping.get(event.coreId).get(setIndex).get(event.index -1 ));
			}
			else 
			{
				destinationBankId = sourceBankId;
			}
		}
		else if(nucaType == NucaType.D_NUCA)
		{
			if(cacheBank.isFirstLevel)
			{
				destinationBankId = sourceBankId;
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
		if(nucaType == NucaType.CB_D_NUCA && event.index != 0 ||
			 nucaType == NucaType.D_NUCA && !cacheBank.isFirstLevel )
		{
				requestType= RequestType.COPY_BLOCK;
		}
		return requestType;
	}
	
	void updateEventOnHit(ArrayList<Event> outstandingRequestList,AddressCarryingEvent event,
										  NucaCacheBank cacheBank)
	{
		sendResponseToWaitingEvent(outstandingRequestList, cacheBank,true);
	}
	
	protected void sendResponseToWaitingEvent(ArrayList<Event> outstandingRequestList,NucaCacheBank cacheBank,boolean flag)
	{ 
		int numberOfWrites = 0;
		AddressCarryingEvent sampleWriteEvent = null;
		RequestType requestType = RequestType.Mem_Response;
		Vector<Integer> sourceBankId = (Vector<Integer>) cacheBank.getRouter().getBankId().clone();
		Vector<Integer> destinationBankId = null;
 		while (!outstandingRequestList.isEmpty())
		{	
			AddressCarryingEvent eventPoppedOut = (AddressCarryingEvent) outstandingRequestList.remove(0); 
			
			if (eventPoppedOut.getRequestType() == RequestType.Cache_Read)
			{
				destinationBankId = getDestinationBankId(eventPoppedOut, cacheBank);
				if(flag)
				{
					requestType = getRequestType(cacheBank, eventPoppedOut);
				}
				cacheBank.getRouter().getPort().put(eventPoppedOut.updateEvent(
																									eventPoppedOut.getEventQ(), 
																									0, cacheBank, 
																									cacheBank.getRouter(), 
																									requestType,
																									sourceBankId,
																									destinationBankId));
			}			
			else if (eventPoppedOut.getRequestType() == RequestType.Cache_Write)
			{
				if (cacheBank.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
				{
					numberOfWrites++;
					sampleWriteEvent = eventPoppedOut;
				}
				else
				{
					CacheLine cl = cacheBank.access(eventPoppedOut.getAddress());
					if (cl != null)
					{
							cl.setState(MESI.MODIFIED);
					}
					else
					{
						System.err.println(" line not present in cache after fill ");					
					}								
				}
			}
		}
		
		if(numberOfWrites > 0)
		{
			destinationBankId = getDestinationBankId(sampleWriteEvent, cacheBank);
			cacheBank.getRouter().getPort().put(sampleWriteEvent.updateEvent(
					sampleWriteEvent.getEventQ(), 
					0, cacheBank, 
					cacheBank.getRouter(), 
					RequestType.Main_Mem_Write,
					sourceBankId,
					destinationBankId));
		}
	}
}
