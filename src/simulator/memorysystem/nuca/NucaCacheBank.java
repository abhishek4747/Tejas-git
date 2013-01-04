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
import java.util.Vector;
import config.CacheConfig;
import config.SimulationConfig;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CacheLine;
import memorysystem.CoreMemorySystem;
import memorysystem.MESI;
import memorysystem.MemorySystem;
import memorysystem.MissStatusHoldingRegister;
import memorysystem.nuca.NucaCache.NucaType;

public class NucaCacheBank extends Cache
{
	public Router router;
	CacheConfig cacheParameters;
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
	NucaCache nucaCache;

	NucaCacheBank(Vector<Integer> bankId,CacheConfig cacheParameters, CoreMemorySystem containingMemSys,NucaCache nucaCache)
    {
        super(cacheParameters,containingMemSys);
    	this.timestamp = 0;
    	this.cacheParameters = cacheParameters;
    	if(cacheParameters.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL)
    		this.router = new Router(cacheParameters.nocConfig,this);
    	else
    		this.router = new OpticalRouter(cacheParameters.nocConfig, this);
        isLastLevel = false;
        isFirstLevel = false;
        nucaType = NucaType.S_NUCA;
        topology = cacheParameters.nocConfig.topology;
        policy = new Policy(nucaCache);
        this.nucaCache = nucaCache;
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
    
    @Override
	public void handleEvent(EventQueue eventQ, Event event){
    	if (event.getRequestType() == RequestType.Cache_Read
				|| event.getRequestType() == RequestType.Cache_Write ) 
    	{
			this.handleAccess(eventQ, (AddressCarryingEvent)event);
    	}
		else if (event.getRequestType() == RequestType.Mem_Response)
		{
			this.handleMemResponse(eventQ, event);
		}
		else if (event.getRequestType() == RequestType.Main_Mem_Read ||
				  event.getRequestType() == RequestType.Main_Mem_Write )
		{
			this.handleMemoryReadWrite(eventQ,event);
		}
		else if (event.getRequestType() == RequestType.Main_Mem_Response )
		{
			handleMainMemoryResponse(eventQ, event);
		}
		else if( event.getRequestType() == RequestType.COPY_BLOCK )
		{
			this.handleCopyBlock(eventQ, event);
		}
		/*else if ( event.getRequestType() == RequestType.Cache_Hit || 
				event.getRequestType() == RequestType.Cache_Miss )
		{
			handleBroadCastAcks(eventQ, (AddressCarryingEvent)event);
		}*/
		else {
			misc.Error.showErrorAndExit(" unexpected request came to cache bank");
		}
	}
	

    /*private void handleBroadCastAcks(EventQueue eventQ,
			AddressCarryingEvent event) {
    	OMREntry omrEntry = nucaCache.missStatusHoldingRegister.getMshrEntry(event.getAddress());
    	if(omrEntry == null)
    	{
    		System.out.println(" error omrEntry null ");
    		System.exit(1);
    	}
    	if(event.getRequestType() == RequestType.Cache_Hit)
    	{
    		omrEntry.hit = true;
    	}
    	omrEntry.acks++;
    	if(omrEntry.acks == nucaCache.cacheMapping.get(event.coreId).get(0).size())
    	{
    		if(omrEntry.hit)
    		{
    			//ArrayList<Event> outstandingRequestList = nucaCache.missStatusHoldingRegister.removeRequests(event.getAddress());
    			int numOfOutStandingRequests = nucaCache.missStatusHoldingRegister.numOutStandingRequests(event.getAddress());
    			nucaCache.hits += numOfOutStandingRequests;//outstandingRequestList.size();
				nucaCache.noOfRequests += numOfOutStandingRequests;//outstandingRequestList.size();
    			policy.sendResponseToWaitingEvent(event,this, false);
    		}
    		else
    		{
    			
    			event.setSourceBankId(this.bankId);
    			event.setDestinationBankId(this.bankId);
    			event.setRequestType(RequestType.Main_Mem_Read);
    			handleMemoryReadWrite(eventQ, event);
    		}
    	}
	}*/

	protected void handleCopyBlock(EventQueue eventQ,Event event)
	{
		AddressCarryingEvent addrEvent = (AddressCarryingEvent)event;
		long address = addrEvent.getAddress();
		CacheLine evictedLine = this.fill(address,MESI.EXCLUSIVE);

		if (evictedLine != null)
		{
			AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
					 0,
					 this, 
					 nucaCache, 
					 RequestType.Main_Mem_Write, 
					 evictedLine.getTag() << this.blockSizeBits,
					 event.coreId);
			nucaCache.getPort().put(addressEvent);
		}
		CacheLine cl = nucaCache.cacheBank[addrEvent.getSourceBankId().get(0)][addrEvent.getSourceBankId().get(1)].access(((AddressCarryingEvent)event).getAddress());
		if(cl != null)
		{
			cl.setState(MESI.INVALID);
		}
	}
	
	private void handleMainMemoryResponse(EventQueue eventQ, Event event) 
	{
		AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
		long addr = addrEvent.getAddress();
		
		CacheLine evictedLine = this.fill(addr,MESI.EXCLUSIVE);
		
		if (evictedLine != null && 
				this.writePolicy != CacheConfig.WritePolicy.WRITE_THROUGH )
		{
			AddressCarryingEvent eventCame =   (AddressCarryingEvent)event;
			Vector<Integer> sourceBankId = new Vector<Integer>(this.bankId);
			Vector<Integer> destinationbankId = new Vector<Integer>(nucaCache.getDestinationBankId(addr, addrEvent.coreId));
			AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
													 0,this,this.getRouter(), 
													 RequestType.Main_Mem_Write, 
													 evictedLine.getTag() << this.blockSizeBits,
													 eventCame.coreId,
													 sourceBankId,destinationbankId);
			this.getRouter().getPort().put(addressEvent);
		}
		int numOfOutStandingRequests = nucaCache.missStatusHoldingRegister.numOutStandingRequests(addrEvent);
		nucaCache.misses += numOfOutStandingRequests;//change this value
		nucaCache.noOfRequests += numOfOutStandingRequests;//change this value
		policy.sendResponseToWaitingEvent(addrEvent, this, false);
	}
	
	private void handleMemoryReadWrite(EventQueue eventQ, Event event) {
		//System.out.println(((AddressCarryingEvent)event).getDestinationBankId() + ""+ ((AddressCarryingEvent)event).getSourceBankId());
		AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
		Vector<Integer> sourceBankId = new Vector<Integer>(addrEvent.getDestinationBankId());
		Vector<Integer> destinationBankId = new Vector<Integer>(addrEvent.getSourceBankId());
		
		RequestType requestType = event.getRequestType();
		if(this.cacheParameters.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL)
		{
			MemorySystem.mainMemory.getPort().put(((AddressCarryingEvent)event).updateEvent(eventQ, 
																											MemorySystem.mainMemory.getLatencyDelay(), this, 
																											MemorySystem.mainMemory, requestType, sourceBankId,
																											destinationBankId));
		}
		else{
		/*	unwanted code */
			this.getRouter().getPort().put(((AddressCarryingEvent)event).updateEvent(eventQ, 
					1, 
					this, 
					(OpticalRouter)this.getRouter(), 
					requestType,
					sourceBankId,
					destinationBankId));		
		}
	}

	public void handleAccess(EventQueue eventQ, AddressCarryingEvent event)
	{
		RequestType requestType = event.getRequestType();
		long address = event.getAddress();
		
		//Process the access
		CacheLine cl = this.processRequest(requestType, address);

		//IF HIT
		if (cl != null || nucaCache.missStatusHoldingRegister.containsWriteOfEvictedLine(address) )
		{
			int numOfOutStandingRequests = nucaCache.missStatusHoldingRegister.numOutStandingRequests(event);
			nucaCache.hits += numOfOutStandingRequests; //
			nucaCache.noOfRequests += numOfOutStandingRequests;//
			policy.updateEventOnHit(event, this);
		}
		//IF MISS
		else
		{
			AddressCarryingEvent tempEvent= policy.updateEventOnMiss( (AddressCarryingEvent)event,this);
			if(tempEvent != null)
			{
				this.getRouter().getPort().put(tempEvent);
			}
		}
	}
}