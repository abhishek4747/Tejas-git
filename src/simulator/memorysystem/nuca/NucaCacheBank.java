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

				Contributor: Mayur Harne
*****************************************************************************/
package memorysystem.nuca;
import generic.Event;
import generic.EventQueue;
import generic.OMREntry;
import generic.RequestType;
import generic.SimulationElement;
import net.*;
import net.NOC.TOPOLOGY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;

import pipeline.inorder.MemUnitIn;
import config.CacheConfig;
import config.SimulationConfig;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CacheLine;
import memorysystem.CoreMemorySystem;
import memorysystem.MESI;
import memorysystem.MemorySystem;
import memorysystem.nuca.NucaCache.NucaType;

public class NucaCacheBank extends Cache
{
	public Router router;
	CacheConfig cacheParameters;
	public Hashtable<Long, ArrayList<Event>> forwardedRequests;
	boolean isLastLevel;
	boolean isFirstLevel;
	NucaType nucaType;
	TOPOLOGY topology;
	Policy policy;
	int cacheBankRows;
	int cacheNumber;
	int cacheBankColumns;
	NucaCache nucaCache;
	NucaCacheBank(Vector<Integer> bankId,CacheConfig cacheParameters, CoreMemorySystem containingMemSys,NucaCache nucaCache)
    {
        super(cacheParameters,containingMemSys);
    	this.timestamp = 0;
    	this.cacheParameters = cacheParameters;
        this.router = new Router(bankId,cacheParameters.nocConfig,this);
        forwardedRequests = new Hashtable<Long, ArrayList<Event>>();
        isLastLevel = false;
        isFirstLevel = false;
        nucaType = NucaType.S_NUCA;
        topology = cacheParameters.nocConfig.topology;
        policy = new Policy(nucaCache);
        this.nucaCache = nucaCache;
        this.cacheBankColumns = cacheParameters.getNumberOfBankColumns();
        this.cacheBankRows = cacheParameters.getNumberOfBankRows();
    }
    
    public Router getRouter()
	{
		return this.router;
	}
    
    public boolean addtoForwardedRequests(Event event,long address)
    {
			boolean entryAlreadyThere;
			long blockAddr = address >>> blockSizeBits;
			if (!/*NOT*/forwardedRequests.containsKey(blockAddr))
			{
				entryAlreadyThere = false;
				forwardedRequests.put(blockAddr, new ArrayList<Event>());
			}
			else if (forwardedRequests.get(blockAddr).isEmpty())
				entryAlreadyThere = false;
			else
				entryAlreadyThere = true;
			forwardedRequests.get(blockAddr).add(event);
			return entryAlreadyThere;
    }
	
    /************************************************************************
     * Method Name  : processRequest
     * Purpose      : process the request that comes to cache
     * Parameters   : Request-type,Address in cache which will be processed
     * Return       : CacheLine which was replaced by the new line
     *************************************************************************/
   
    /************************************************************************
     * Method Name  : fill
     * Purpose      : fill the cache line with data specified by the address
     * Parameters   : address
     * Return       : CacheLine which will be replaced during filling cache
     *************************************************************************/
	
    /************************************************************************
     * Method Name  : handleEvent
     * Purpose      : handle the event posted by upper level cache because of miss
     * Parameters   : EventQueue,Event
     * Return       : None
     *************************************************************************/
	@Override
	public void handleEvent(EventQueue eventQ, Event event){
		if(!((AddressCarryingEvent)event).getDestinationBankId().equals(this.getRouter().getBankId()))
		{
			
			System.out.println("error routed to different router");
			System.exit(1);
		}
		if (event.getRequestType() == RequestType.Cache_Read
				|| event.getRequestType() == RequestType.Cache_Write
				|| event.getRequestType() == RequestType.Cache_Read_from_iCache)
			this.handleAccess(eventQ, event);
		else if (event.getRequestType() == RequestType.Mem_Response)
			this.handleMemResponse(eventQ, event);
		else if (event.getRequestType() == RequestType.Main_Mem_Read ||  
				event.getRequestType() == RequestType.Main_Mem_Write)
			this.handleMemoryReadWrite(eventQ,event);
		else if (event.getRequestType() == RequestType.Main_Mem_Response)
			this.handleMainMemoryResponse(eventQ,event);
		else if( event.getRequestType() == RequestType.COPY_BLOCK)
		{
			this.handleCopyBlock(eventQ, event);
		}
	}
	
    /************************************************************************
     * Method Name  : handleCopyBlock
     * Purpose      : handles the events whose request type is copy block
     * Parameters   : EventQueue,Event
     * Return       : None
     *************************************************************************/
	protected void handleCopyBlock(EventQueue eventQ,Event event)
	{
		long address = ((AddressCarryingEvent)event).getAddress();
		CacheLine evictedLine = this.fill(address,MESI.EXCLUSIVE);
		//if condition to be  changed
		if (evictedLine != null)
		{
			AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																		 0,
																		 this, 
																		 this, 
																		 RequestType.Main_Mem_Write, 
																		 evictedLine.getTag() << this.blockSizeBits,
																		 ((AddressCarryingEvent)event).coreId);
			Vector<Integer> sourceBankId = new Vector<Integer>(
															this.getRouter().getBankId());
			Vector<Integer> destinationBankId = new Vector<Integer>(
																	((AddressCarryingEvent)
																     (event)).
																	 getDestinationBankId());
			
			addressEvent.setSourceBankId(sourceBankId);
			addressEvent.setDestinationBankId(destinationBankId);
			this.getPort().put(addressEvent);
			
		}
		if(SimulationConfig.nucaType == NucaType.D_NUCA)
		{
			Vector<Integer> sourceBankId = new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
			this.getRouter().getPort().put(((AddressCarryingEvent)event).
															updateEvent(eventQ,
																		0, 
																		this, 
																		this.getRouter(), 
																		RequestType.Mem_Response,
																		sourceBankId,
																		((AddressCarryingEvent)event).oldSourceBankId));
		}
		else
		{
			Vector<Integer> sourceBankId = new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
			int setIndex =  nucaCache.getSetIndex(address);
			this.getRouter().getPort().put(((AddressCarryingEvent)event).
															updateEvent(eventQ,
																		0, 
																		this, 
																		this.getRouter(), 
																		RequestType.Mem_Response,
																		sourceBankId,
																		nucaCache.integerToBankId(nucaCache.cacheMapping.get(((AddressCarryingEvent)event).coreId).get(setIndex).get(0))));
		}
	}
	
    /************************************************************************
     * Method Name  : handleMainMemoryResponse
     * Purpose      : handles the events whose request type is Main_Mem_response
     * Parameters   : EventQueue,Event
     * Return       : None
     *************************************************************************/
	private void handleMainMemoryResponse(EventQueue eventQ, Event event) {//changes are required....
		long addr = ((AddressCarryingEvent)(event)).getAddress();
		CacheLine evictedLine = this.fill(addr,MESI.EXCLUSIVE);
		//if condition to be  changed
		if (evictedLine != null)
		{
			AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																		 0,
																		 this, 
																		 this.getRouter(), 
																		 RequestType.Main_Mem_Write, 
																		 evictedLine.getTag() << this.blockSizeBits,
																		 ((AddressCarryingEvent)event).coreId);
			Vector<Integer> sourceBankId = new Vector<Integer>(
															   ((AddressCarryingEvent)
															    (event)).
															    getDestinationBankId());
			Vector<Integer> destinationBankId = new Vector<Integer>(
																	((AddressCarryingEvent)
																     (event)).
																	 getSourceBankId());
			
			addressEvent.setSourceBankId(sourceBankId);
			addressEvent.setDestinationBankId(destinationBankId);
			this.getRouter().getPort().put(addressEvent);
		}
		long blockAddr = addr >>> this.blockSizeBits;
			
		if (!this.missStatusHoldingRegister.containsKey(blockAddr))
		{
			nucaCache.printidTOBankMapping();
			System.err.println("Cache Error : request not present in cache bank" +((AddressCarryingEvent)(event)).getSourceBankId() + ((AddressCarryingEvent)(event)).getDestinationBankId() + this.getRouter().getBankId() + "event Time " +event.getEventTime() + " address " + blockAddr );
			System.exit(1);
		}
		//System.out.println("block address removed "+ blockAddr);
		ArrayList<Event> outstandingRequestList = this.missStatusHoldingRegister.remove(blockAddr).outStandingEvents;
		while (!/*NOT*/outstandingRequestList.isEmpty())
		{				
			AddressCarryingEvent tempevent = (AddressCarryingEvent) outstandingRequestList.remove(0);
			if (tempevent.getRequestType() == RequestType.Cache_Write ||
				((AddressCarryingEvent)event).oldRequestType == RequestType.Cache_Write	)
			{
				if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
				{
					AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																				 0,
																				 this, 
																				 this.getRouter(), 
																				 RequestType.Main_Mem_Write, 
																				 addr,
																				 ((AddressCarryingEvent)event).coreId);
					Vector<Integer> sourceBankId = new Vector<Integer>(
																	   ((AddressCarryingEvent)
																	    (tempevent)).
																	    getDestinationBankId());
					Vector<Integer> destinationBankId = new Vector<Integer>(
																		((AddressCarryingEvent)
																	     (tempevent)).
																		 getSourceBankId());

					addressEvent.setSourceBankId(sourceBankId);
					addressEvent.setDestinationBankId(destinationBankId);
					this.getRouter().getPort().put(addressEvent);
				}				
			}
			else if(tempevent.getRequestType() == RequestType.Cache_Read ||
					((AddressCarryingEvent)event).oldRequestType == RequestType.Cache_Read ||
					tempevent.getRequestType() == RequestType.Cache_Read_from_iCache ||
					((AddressCarryingEvent)event).oldRequestType == RequestType.Cache_Read_from_iCache)
			{
				Vector<Integer> sourceBankId = new Vector<Integer>(
																   ((AddressCarryingEvent)
																    tempevent).
																    getDestinationBankId());
				Vector<Integer> destinationBankId = new Vector<Integer>(
																	((AddressCarryingEvent)
																     tempevent).
																	 getSourceBankId());

				this.getRouter().getPort().put(
						((AddressCarryingEvent)tempevent).updateEvent(
																	tempevent.getEventQ(),
																	0, //For same cycle response //outstandingRequestList.get(0).getRequestingElement().getLatencyDelay(),
																	this,
																	this.getRouter(),
																	RequestType.Mem_Response,									
																	sourceBankId,
																	destinationBankId));
			}
			else
			{
				System.err.println("Cache Error : A request was of type other than Cache_Read or Cache_Write from line 164 error " + event.getRequestType() + ((AddressCarryingEvent)event).getSourceBankId() + ((AddressCarryingEvent)event).getDestinationBankId());
				System.exit(1);
			}
		}
		Vector<Integer> indexToRemove = new Vector<Integer>();
		for(int i=0; i < connectedMSHR.size();i++)
		{
			
			Hashtable<Long,OMREntry> tempMissStatusHoldingRegister = connectedMSHR.get(i);
			int readyToProceedCount =0;
			int instructionProceeded =0;
			Enumeration<OMREntry> omrIte = tempMissStatusHoldingRegister.elements();
			Enumeration<Long> omrKeys = tempMissStatusHoldingRegister.keys();
			while(omrIte.hasMoreElements())
			{
				OMREntry omrEntry = omrIte.nextElement();
				Long key = omrKeys.nextElement();
				if(omrEntry.readyToProceed && ((AddressCarryingEvent)omrEntry.eventToForward).getDestinationBankId().equals(this.getRouter().getBankId()))
				{
					readyToProceedCount++;
					SimulationElement requestingElement = omrEntry.eventToForward.getRequestingElement();
					if(requestingElement.getClass() != MemUnitIn.class)
					{
						omrEntry.readyToProceed = false;
					}
					handleAccess(omrEntry.eventToForward.getEventQ(), omrEntry.eventToForward);
					if(!omrEntry.readyToProceed)
					{
						instructionProceeded++;
					}
				}
				if(missStatusHoldingRegister.size() >= MSHRSize)
				{
					break;
				}
			}
			if(readyToProceedCount == instructionProceeded && readyToProceedCount>0)
			{
				indexToRemove.add(i);
			}
			if(missStatusHoldingRegister.size() >= MSHRSize)
			{
				break;
			}
		}
		for(int i=0;i<indexToRemove.size();i++)
		{
			this.connectedMSHR.remove(indexToRemove.get(i));
		}
	}

    /************************************************************************
     * Method Name  : handleMainMemoryReadWrite
     * Purpose      : handles the events whose request type is Main_Mem_Read or write.
     * Parameters   : EventQueue,Event
     * Return       : None
     *************************************************************************/
	private void handleMemoryReadWrite(EventQueue eventQ, Event event) {
		//System.out.println(((AddressCarryingEvent)event).getDestinationBankId() + ""+ ((AddressCarryingEvent)event).getSourceBankId());
		Vector<Integer> sourceBankId = new Vector<Integer>(
														   ((AddressCarryingEvent)
														    event).
														    getDestinationBankId());
		Vector<Integer> destinationBankId = new Vector<Integer>(
															((AddressCarryingEvent)
														     event).
															 getSourceBankId());
		
		RequestType requestType = event.getRequestType();
		MemorySystem.mainMemory.getPort().put(((AddressCarryingEvent)event).updateEvent(eventQ, 
															MemorySystem.mainMemory.getLatencyDelay(), 
															event.getProcessingElement(), 
															MemorySystem.mainMemory, 
															requestType,
															sourceBankId,
															destinationBankId));		
	}

	/************************************************************************
     * Method Name  : handleAccess
     * Purpose      : handles the events whose request type is Cache-read or cache-write
     * Parameters   : EventQueue,Event
     * Return       : None
     *************************************************************************/
	protected void handleAccess(EventQueue eventQ, Event event)
	{
		/*if(!((AddressCarryingEvent)event).getDestinationBankId().equals(this.getRouter().getBankId()))
		{
			System.out.println("before else");
			System.out.println("destination bank id "+((AddressCarryingEvent)event).getDestinationBankId() + "this " + this.getRouter().getBankId());
			System.exit(1);
		}*/
		RequestType requestType = event.getRequestType();
		long address = ((AddressCarryingEvent)(event)).getAddress();
		
		//Process the access
		CacheLine cl = this.processRequest(requestType, address);

		//System.out.println("requesting for address "+ address);
		//IF HIT
		if (cl != null)
		{
			//Schedule the requesting element to receive the block TODO (for LSQ)
			/* check for requesting element and processing element*/
			if (requestType == RequestType.Cache_Read || requestType == RequestType.Cache_Read_from_iCache ) 
			{
				//Just return the read block
				this.getRouter().getPort().put(policy.updateEventOnHit(eventQ, (AddressCarryingEvent)event, this,topology));
			}
			else if (requestType == RequestType.Cache_Write)
			{
				if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
				{
					Vector<Integer> sourceBankId = new Vector<Integer>(
																	   ((AddressCarryingEvent)
																	    (event)).
																	    getDestinationBankId());
					Vector<Integer> destinationBankId = new Vector<Integer>(
																		((AddressCarryingEvent)
																	     (event)).
																		 getSourceBankId());

					AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																				 0,
																				 this, 
																				 this.getRouter(), 
																				 RequestType.Main_Mem_Write, 
																				 address,
																				 ((AddressCarryingEvent)event).coreId);
					addressEvent.setSourceBankId(sourceBankId);
					addressEvent.setDestinationBankId(destinationBankId);
					this.getRouter().getPort().put(addressEvent);
				}
			}
		}
		
		//IF MISS
		else 		
		{
			/*if(!((AddressCarryingEvent)event).getDestinationBankId().equals(this.getRouter().getBankId()))
			{
				System.out.println("inside else");
				System.out.println("destination bank id "+((AddressCarryingEvent)event).getDestinationBankId() + "this " + this.getRouter().getBankId());
				System.exit(1);
			}*/

			AddressCarryingEvent tempEvent= policy.updateEventOnMiss(eventQ, (AddressCarryingEvent)event, this,topology);
			if(tempEvent != null)
			{
				if(!((AddressCarryingEvent)tempEvent).getSourceBankId().equals(this.getRouter().getBankId()))
				{
					
					System.out.println("reached wrong cache column ");
					//System.out.println("error routed to different router 527");
					//System.exit(1);
				}

				if(tempEvent.getRequestType() == RequestType.Main_Mem_Read)
				{
					//System.out.println("address added to mshr " + (tempEvent.getAddress() >> blockSizeBits) );
					//System.out.println("outstanding request size " + missStatusHoldingRegister.get((tempEvent.getAddress() >> blockSizeBits)).outStandingEvents.size());
				}
				this.getRouter().getPort().put(tempEvent);
			}
		}
	}

	/************************************************************************
     * Method Name  : handleMemResponse
     * Purpose      : handles the events whose request type is Mem_response
     * Parameters   : EventQueue,Event
     * Return       : None
     *************************************************************************/
	protected void handleMemResponse(EventQueue eventQ, Event event)
	{
		//System.out.println("served the request for address "+ ((AddressCarryingEvent)event).getAddress());	
		((AddressCarryingEvent)event).oldRequestingElement.getPort().put
											(event.update
														(eventQ, 
														((AddressCarryingEvent)event).oldRequestingElement.getLatencyDelay() , 
														 this,
														 ((AddressCarryingEvent)event).oldRequestingElement,
														 RequestType.Mem_Response));
	}
}