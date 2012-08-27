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
import net.*;
import net.NOC.TOPOLOGY;
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
	NucaCache nucaCache;

	NucaCacheBank(Vector<Integer> bankId,CacheConfig cacheParameters, CoreMemorySystem containingMemSys,NucaCache nucaCache)
    {
        super(cacheParameters,containingMemSys);
    	this.timestamp = 0;
    	this.cacheParameters = cacheParameters;
        this.router = new Router(bankId,cacheParameters.nocConfig,this);
        isLastLevel = false;
        isFirstLevel = false;
        nucaType = NucaType.S_NUCA;
        topology = cacheParameters.nocConfig.topology;
        policy = new Policy(nucaCache);
        this.nucaCache = nucaCache;
        this.cacheBankColumns = cacheParameters.getNumberOfBankColumns();
        this.cacheBankRows = cacheParameters.getNumberOfBankRows();
        this.connectedMSHR = nucaCache.connectedMSHR;
    }
    
    public Router getRouter()
	{
		return this.router;
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
				event.getRequestType() == RequestType.Main_Mem_Write)
		{
			this.handleMemoryReadWrite(eventQ,event);
		}
		else if (event.getRequestType() == RequestType.Main_Mem_Response )
		{
			this.handleMainMemoryResponse(eventQ,event);
		}
		else if( event.getRequestType() == RequestType.COPY_BLOCK )
		{
			this.handleCopyBlock(eventQ, event);
		}
	}
	
    
    protected void handleCopyBlock(EventQueue eventQ,Event event)
	{
		long address = ((AddressCarryingEvent)event).getAddress();
		CacheLine evictedLine = this.fill(address,MESI.EXCLUSIVE);

		if (evictedLine != null)
		{
			AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
					 0,
					 this, 
					 this.getRouter(), 
					 RequestType.Main_Mem_Write, 
					 evictedLine.getTag() << this.blockSizeBits,
					 event.coreId,
					 this.getRouter().getBankId(),
					 ((AddressCarryingEvent)(event)).getDestinationBankId());
			this.getPort().put(addressEvent);
		}
		//TODO invalidation of line copied from previous cachebank
		Vector<Integer> sourceBankId = new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
		Vector<Integer> destinationBankId = null;
		if(SimulationConfig.nucaType == NucaType.D_NUCA)
		{
			destinationBankId = ((AddressCarryingEvent)event).oldSourceBankId;
		}
		else
		{
			System.err.println(" COPY BLOCK request came for other nuca type ");
			System.exit(1);
		}
		
		this.getRouter().getPort().put(((AddressCarryingEvent)event).
				updateEvent(eventQ,
							0, 
							this, 
							this.getRouter(), 
							RequestType.Mem_Response,
							sourceBankId,
							destinationBankId));
	}
	
	private void handleMainMemoryResponse(EventQueue eventQ, Event event) 
	{
		long addr = ((AddressCarryingEvent)(event)).getAddress();
		CacheLine evictedLine = this.fill(addr,MESI.EXCLUSIVE);
		
		if (evictedLine != null && 
				this.writePolicy != CacheConfig.WritePolicy.WRITE_THROUGH )
		{
			AddressCarryingEvent eventCame =   (AddressCarryingEvent)event;
			AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																								 0,
																								 this, 
																								 this.getRouter(), 
																								 RequestType.Main_Mem_Write, 
																								 evictedLine.getTag() << this.blockSizeBits,
																								eventCame.coreId,
																								eventCame.getDestinationBankId(),
																								eventCame.getSourceBankId());
			this.getRouter().getPort().put(addressEvent);
		}
		ArrayList<Event> outstandingRequestList = nucaCache.missStatusHoldingRegister.removeRequests(addr);
		policy.sendResponseToWaitingEvent(outstandingRequestList, this, false);
		nucaCache.misses += outstandingRequestList.size();			
		nucaCache.noOfRequests += outstandingRequestList.size();

	}

	private void handleMemoryReadWrite(EventQueue eventQ, Event event) 
	{
		AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
		if(addrEvent.getDestinationBankId() == null || addrEvent.getSourceBankId() == null)
		{
			System.out.println(" destination bank id null ");
		}
		Vector<Integer> sourceBankId = new Vector<Integer>(addrEvent.getDestinationBankId());
		Vector<Integer> destinationBankId = new Vector<Integer>(addrEvent.getSourceBankId());
		//System.out.println("from handle memory read write destination bank id" + destinationBankId + " source bank id" + sourceBankId);
		MemorySystem.mainMemory.getPort().put(((AddressCarryingEvent)event).updateEvent(eventQ, 
															MemorySystem.mainMemory.getLatencyDelay(), 
															event.getProcessingElement(), 
															MemorySystem.mainMemory, 
															event.getRequestType(),
															sourceBankId,
															destinationBankId));		
	}

	public void handleAccess(EventQueue eventQ, AddressCarryingEvent event)
	{
		RequestType requestType = event.getRequestType();
		long address = ((AddressCarryingEvent)(event)).getAddress();
		
		//Process the access
		//System.out.println("inside handle access for cache bank " + this.router.getBankId());
		CacheLine cl = this.processRequest(requestType, address);
		if(((AddressCarryingEvent)event).getSourceBankId() == null || ((AddressCarryingEvent)event).getSourceBankId() == null )
		{
			System.out.println(" destinationbank is null  from handle access");
			System.exit(1);
		}
		//IF HIT
		if (cl != null || nucaCache.missStatusHoldingRegister.containsWriteOfEvictedLine(address) )
		{
			ArrayList<Event> eventsToBeServed = nucaCache.missStatusHoldingRegister.removeRequests(address); 
			nucaCache.hits += eventsToBeServed.size();
			nucaCache.noOfRequests += eventsToBeServed.size();
			policy.updateEventOnHit(eventsToBeServed, (AddressCarryingEvent) event, this);
		}
		else
		{
			AddressCarryingEvent tempEvent= policy.updateEventOnMiss( (AddressCarryingEvent)event,this);
			if(tempEvent != null)
			{
				this.getRouter().getPort().put(tempEvent);
			}
		}
	}
	
	protected void handleMemResponse(EventQueue eventQ, Event event)
	{
		((AddressCarryingEvent)event).oldRequestingElement.getPort().put
											(event.update
														(eventQ, 
														((AddressCarryingEvent)event).oldRequestingElement.getLatencyDelay() , 
														 this,
														 ((AddressCarryingEvent)event).oldRequestingElement,
														 RequestType.Mem_Response));
	}
	
}