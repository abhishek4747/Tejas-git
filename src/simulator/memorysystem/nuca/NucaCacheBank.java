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
import net.NOC.CONNECTIONTYPE;
import net.NOC.TOPOLOGY;
import net.optical.OpticalRouter;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;

import pipeline.inorder.MemUnitIn;
import config.CacheConfig;
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
	int cacheBankColumns;
	int counter = 0;
	int sendcounter =0;
//	NOC noc;
	protected Vector<Integer> bankId = new Vector<Integer>(2); //bank id of router(vector <row,column>)
	
	NucaCacheBank(Vector<Integer> bankId,CacheConfig cacheParameters, CoreMemorySystem containingMemSys)
    {
        super(cacheParameters,containingMemSys);
    	this.timestamp = 0;
    	this.cacheParameters = cacheParameters;
    	if(cacheParameters.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL)
    		this.router = new Router(cacheParameters.nocConfig,this);
    	else
    		this.router = new OpticalRouter(cacheParameters.nocConfig, this);
        forwardedRequests = new Hashtable<Long, ArrayList<Event>>();
        isLastLevel = false;
        isFirstLevel = false;
        nucaType = NucaType.S_NUCA;
        topology = cacheParameters.nocConfig.topology;
        policy = new Policy();
        this.cacheBankColumns = cacheParameters.getNumberOfBankColumns();
        this.cacheBankRows = cacheParameters.getNumberOfBankRows();
        this.bankId  = bankId;
  //      this.noc = noc;
    }
    
    public Router getRouter()
	{
		return this.router;
	}
    public Vector<Integer> getBankId()
	{
		return this.bankId;
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
    public CacheLine processRequest(RequestType requestType, long addr)
	{
		noOfRequests++;
		//boolean isHit;
		/* access the Cache */
		CacheLine ll = null;
		if(requestType == RequestType.Cache_Read || requestType == RequestType.Cache_Read_from_iCache)
			ll = this.read(addr);
		else if (requestType == RequestType.Cache_Write)
			ll = this.write(addr);
		
		if(ll == null)
		{
			/* Miss */
//			if (!(request.isWriteThrough()))//TODO For testing purposes only
			this.misses++;
		} 
		else 
		{
			/* Hit */
			/* do nothing */
//			if (!(request.isWriteThrough()))//TODO For testing purposes only
			this.hits++;				
		}
		return ll;
	}

    /************************************************************************
     * Method Name  : fill
     * Purpose      : fill the cache line with data specified by the address
     * Parameters   : address
     * Return       : CacheLine which will be replaced during filling cache
     *************************************************************************/
	public CacheLine fill(long addr) //Returns a copy of the evicted line
	{
		CacheLine evictedLine = null;
		
		/* remove the block size */
		long tag = addr >>> this.blockSizeBits;

		/* search all the lines that might match */
		long laddr = tag >>> this.assocBits;
		laddr = laddr << assocBits; // replace the associativity bits with zeros.

		/* remove the tag portion */
		laddr = laddr & numLinesMask;

		/* find any invalid lines -- no eviction */
		CacheLine fillLine = null;
		boolean evicted = false;
		for (int idx = 0; idx < assoc; idx++) 
		{
			CacheLine ll = this.lines[(int)(laddr + (long)(idx))];
			if (!(ll.isValid())) 
			{
				fillLine = ll;
				break;
			}
		}
		
		/* LRU replacement policy -- has eviction*/
		if (fillLine == null) 
		{
			evicted = true; // We need eviction in this case
			double minTimeStamp = Double.MAX_VALUE;
			for(int idx=0; idx<assoc; idx++) 
			{
				CacheLine ll = this.lines[(int)(laddr + (long)(idx))];
				if(minTimeStamp > ll.getTimestamp()) 
				{
					minTimeStamp = ll.getTimestamp();
					fillLine = ll;
				}
			}
		}

		/* if there has been an eviction */
		if (evicted) 
		{
			evictedLine = fillLine.copy();
			
			//if (fillLine.getPid() != request.getThreadID()) //TODO I didn't understand the logic
			//{
				/* increase eviction count */
				this.evictions++;

				/* log the line */
				evictedLines.addElement(fillLine.getTag());
			//}
		}
		/* This is the new fill line */
		fillLine.setState(MESI.SHARED);
		//fillLine.setValid(true);
		mark(fillLine, tag);
		return evictedLine;
	}

    /************************************************************************
     * Method Name  : handleEvent
     * Purpose      : handle the event posted by upper level cache because of miss
     * Parameters   : EventQueue,Event
     * Return       : None
     *************************************************************************/
	@Override
	public void handleEvent(EventQueue eventQ, Event event){
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
		CacheLine evictedLine = this.fill(((AddressCarryingEvent)event).getAddress());
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
															this.getBankId());
			Vector<Integer> destinationBankId = new Vector<Integer>(
																	((AddressCarryingEvent)
																     (event)).
																	 getDestinationBankId());
			
			addressEvent.setSourceBankId(sourceBankId);
			addressEvent.setDestinationBankId(destinationBankId);
			this.getPort().put(addressEvent);
			
		}
		Vector<Integer> sourceBankId = new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
		
		this.getRouter().getPort().put(((AddressCarryingEvent)event).
													updateEvent(eventQ,
																0, 
																this, 
																this.getRouter(), 
																RequestType.Mem_Response,
																sourceBankId,
																((AddressCarryingEvent)event).oldSourceBankId));
		//System.out.println("copy block Event to NOC" + "from"  + sourceBankId + "to" +((AddressCarryingEvent)event).oldSourceBankId+ "with address" + ((AddressCarryingEvent)event).getAddress());
	}
	
    /************************************************************************
     * Method Name  : handleMainMemoryResponse
     * Purpose      : handles the events whose request type is Main_Mem_response
     * Parameters   : EventQueue,Event
     * Return       : None
     *************************************************************************/
	private void handleMainMemoryResponse(EventQueue eventQ, Event event) {//changes are required....
		counter++;
		//System.out.println("counter " + counter + "send counter " + sendcounter);
		long addr = ((AddressCarryingEvent)(event)).getAddress();
		CacheLine evictedLine = this.fill(addr);
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
			//System.out.println("Event to NOC" + "from" + ((AddressCarryingEvent)addressEvent).getSourceBankId() + "to" + ((AddressCarryingEvent)addressEvent).getDestinationBankId() + "with address" + ((AddressCarryingEvent)addressEvent).getAddress());
			this.getRouter().getPort().put(addressEvent);
		}
		long blockAddr = addr >>> this.blockSizeBits;
			
		if (!this.missStatusHoldingRegister.containsKey(blockAddr))
		{
			System.err.println("Cache Error : A request was of type other than Cache_Read or Cache_Write from line 416 "+((AddressCarryingEvent)event).getAddress());
			System.exit(1);
		}
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
					//System.out.println("Event to NOC" + "from" + ((AddressCarryingEvent)addressEvent).getSourceBankId() + "to" + ((AddressCarryingEvent)addressEvent).getDestinationBankId() + "with address" + ((AddressCarryingEvent)addressEvent).getAddress());
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
				//System.out.println("ssEvent to NOC" + "from" + sourceBankId + "to" + destinationBankId + "with address" + ((AddressCarryingEvent)event).getAddress());
				this.getRouter().getPort().put(
						((AddressCarryingEvent)tempevent).updateEvent(
																	eventQ,
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
				if(omrEntry.readyToProceed)
				{
					readyToProceedCount++;
					SimulationElement requestingElement = omrEntry.eventToForward.getRequestingElement();
					if(requestingElement.getClass() != MemUnitIn.class)
					{
						omrEntry.readyToProceed = false;
					}
					handleAccess(eventQ, omrEntry.eventToForward);
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
		
		sendcounter++;
		Vector<Integer> sourceBankId = new Vector<Integer>(
														   ((AddressCarryingEvent)
														    event).
														    getDestinationBankId());
		Vector<Integer> destinationBankId = new Vector<Integer>(
															((AddressCarryingEvent)
														     event).
															 getSourceBankId());
		
		RequestType requestType = event.getRequestType();
		//System.out.println("Event to main memory" + sourceBankId + "  " + destinationBankId + requestType);
//		((OpticalNOC)noc).entryPoint.getPort().put(((AddressCarryingEvent)event).updateEvent(eventQ, 
//															1, 
//															this, 
//															((OpticalNOC)noc).entryPoint, 
//															requestType,
//															sourceBankId,
//															destinationBankId));
		if(this.cacheParameters.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL){
		MemorySystem.mainMemory.getPort().put(((AddressCarryingEvent)event).updateEvent(eventQ, 
															MemorySystem.mainMemory.getLatencyDelay(), 
															event.getProcessingElement(), 
															MemorySystem.mainMemory, 
															requestType,
															sourceBankId,
															destinationBankId));
		}
		else{
		/*	unwanted code */
		this.getRouter().getPort().put(((AddressCarryingEvent)event).updateEvent(eventQ, 
				1, 
				this, 
				this.getRouter(), 
				requestType,
				sourceBankId,
				destinationBankId));		
		}
	}

	/************************************************************************
     * Method Name  : handleAccess
     * Purpose      : handles the events whose request type is Cache-read or cache-write
     * Parameters   : EventQueue,Event
     * Return       : None
     *************************************************************************/
	protected void handleAccess(EventQueue eventQ, Event event)
	{
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
					this.getRouter().getPort().put(policy.updateEventOnHit(eventQ, (AddressCarryingEvent)event, this,nucaType,topology));
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
					//System.out.println("Event to NOC" + "from" + ((AddressCarryingEvent)addressEvent).getSourceBankId() + "to" + ((AddressCarryingEvent)addressEvent).getDestinationBankId() + "with address" + ((AddressCarryingEvent)addressEvent).getAddress());
					this.getRouter().getPort().put(addressEvent);
				}
			}
		}
		
		//IF MISS
		else 		
		{
			AddressCarryingEvent tempEvent= policy.updateEventOnMiss(eventQ, (AddressCarryingEvent)event, this,nucaType,topology);
			if(tempEvent != null){
				//System.out.println("Event to NOC" + "from" + ((AddressCarryingEvent)tempEvent).getSourceBankId() + "to" + ((AddressCarryingEvent)tempEvent).getDestinationBankId() + "with address" + ((AddressCarryingEvent)tempEvent).getAddress());
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
		//System.out.println("i dont know" + ((AddressCarryingEvent)event).getSourceBankId() + " " + ((AddressCarryingEvent)event).getDestinationBankId() + " " + ((AddressCarryingEvent)event).getAddress());
		((AddressCarryingEvent)event).oldRequestingElement.getPort().put
											(event.update
														(eventQ, 
														((AddressCarryingEvent)event).oldRequestingElement.getLatencyDelay() , 
														 this,
														 ((AddressCarryingEvent)event).oldRequestingElement,
														 RequestType.Mem_Response));
//		this.getRouter().getPort().put
//			(event.update
//					(eventQ, 
//					 1 , 
//					 this,
//					 this.getRouter(),
//					 RequestType.Mem_Response));

	}
}