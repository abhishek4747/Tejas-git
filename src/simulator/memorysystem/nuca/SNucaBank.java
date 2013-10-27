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
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;
import net.*;
import net.NOC.CONNECTIONTYPE;
import java.util.Vector;
import config.CacheConfig;
import config.SystemConfig;
import memorysystem.AddressCarryingEvent;
import memorysystem.CacheLine;
import memorysystem.CoreMemorySystem;
import memorysystem.MESI;
import memorysystem.MainMemoryController;
import memorysystem.MemorySystem;
import memorysystem.nuca.NucaCache.NucaType;

public class SNucaBank extends NucaCacheBank implements NocInterface
{

	SNucaBank(Vector<Integer> bankId,CacheConfig cacheParameters, CoreMemorySystem containingMemSys,SNuca nucaCache, NucaType nucaType)
    {
        super(bankId,cacheParameters,containingMemSys,nucaCache, nucaType);
    }
    @Override
	public void handleEvent(EventQueue eventQ, Event event)
    {
    	if (event.getRequestType() == RequestType.Cache_Read
				|| event.getRequestType() == RequestType.Cache_Write ) 
    	{
    		this.handleAccess(eventQ, (AddressCarryingEvent)event);
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
		else 
		{
			System.err.println(event.getRequestType());
			misc.Error.showErrorAndExit(" unexpected request came to cache bank");
		}
	}
    public void handleAccess(EventQueue eventQ, AddressCarryingEvent event)
	{
		RequestType requestType = event.getRequestType();
		long address = event.getAddress();
		nucaCache.incrementTotalNucaBankAcesses(1);
		nucaCache.updateMaxHopLength(event.hopLength,event);
		nucaCache.updateMinHopLength(event.hopLength);
		nucaCache.updateAverageHopLength(event.hopLength);
		//Process the access
		CacheLine cl = this.processRequest(requestType, address,event);
		
		//IF HIT
		if (cl != null || nucaCache.missStatusHoldingRegister.containsWriteOfEvictedLine(address) )
		{
			int numOfOutStandingRequests = nucaCache.missStatusHoldingRegister.numOutStandingRequests(event);
			nucaCache.hits+=numOfOutStandingRequests; //
			nucaCache.noOfRequests += numOfOutStandingRequests;//
			policy.updateEventOnHit(event, this);
		}
		//IF MISS
		else
		{
			AddressCarryingEvent tempEvent= policy.updateEventOnMiss( (AddressCarryingEvent)event,this);
			if(tempEvent != null)
			{
				tempEvent.getProcessingElement().getPort().put(tempEvent);
			}
		}
	}
    protected void handleMemoryReadWrite(EventQueue eventQ, Event event) 
    {
		AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
		
		nucaCache.updateMaxHopLength(addrEvent.hopLength,addrEvent);
		nucaCache.updateMinHopLength(addrEvent.hopLength);
		nucaCache.updateAverageHopLength(addrEvent.hopLength);
		
		Vector<Integer> sourceId = addrEvent.getSourceId();
		Vector<Integer> destinationId = ((AddressCarryingEvent)event).getDestinationId();
		
		RequestType requestType = event.getRequestType();
		if(SystemConfig.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL)
		{
			MemorySystem.mainMemoryController.getPort().put(((AddressCarryingEvent)event).updateEvent(eventQ, 
												MemorySystem.mainMemoryController.getLatencyDelay(), this, 
												MemorySystem.mainMemoryController, requestType, sourceId,
												destinationId));
		}
	}
    protected void handleMainMemoryResponse(EventQueue eventQ, Event event) 
	{
		AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
		
		nucaCache.updateMaxHopLength(addrEvent.hopLength,addrEvent);
		nucaCache.updateMinHopLength(addrEvent.hopLength);
		nucaCache.updateAverageHopLength(addrEvent.hopLength);
		
		long addr = addrEvent.getAddress();
		
		Vector<Integer> sourceId;
		Vector<Integer> destinationId;
		
		if(event.getRequestingElement().getClass() == MainMemoryController.class)
		{
			sourceId = this.getId();
			destinationId = nucaCache.getBankId(addr);
			AddressCarryingEvent addressEvent = new AddressCarryingEvent(event.getEventQ(),
																		0,this, this.getRouter(), 
																		RequestType.Main_Mem_Response, 
																		addr,((AddressCarryingEvent)event).coreId,
																		sourceId,destinationId);
			this.getRouter().getPort().put(addressEvent);
		}
		
		if(event.getRequestingElement().getClass() == Router.class)
		{
			nucaCache.incrementTotalNucaBankAcesses(1);
			CacheLine evictedLine = this.fill(addr,MESI.EXCLUSIVE);
			if (evictedLine != null && 
					this.writePolicy != CacheConfig.WritePolicy.WRITE_THROUGH )
			{
				sourceId = new Vector<Integer>(this.getId());
				destinationId = (Vector<Integer>) nucaCache.getMemoryControllerId(nucaCache.getBankId(addr));
				
				AddressCarryingEvent addressEvent = new AddressCarryingEvent(event.getEventQ(),
																			 0,this, this.getRouter(), 
																			 RequestType.Main_Mem_Write, 
																			 addr,((AddressCarryingEvent)event).coreId,
																			 sourceId,destinationId);
				this.getRouter().getPort().put(addressEvent);
			}
			int numOfOutStandingRequests = nucaCache.missStatusHoldingRegister.numOutStandingRequests(addrEvent);
			nucaCache.misses += numOfOutStandingRequests;//change this value
			nucaCache.noOfRequests += numOfOutStandingRequests;//change this value
			policy.sendResponseToWaitingEvent((AddressCarryingEvent)event, this);
		}
	}

	@Override
	public SimulationElement getSimulationElement() {
		return this;
	}
	
	public long getEvictions() {
		return evictions;
	}

	public void setEvictions(long evictions) {
		this.evictions = evictions;
	}
	public void incrementEvictions(long evictions) {
		this.evictions += evictions;
	}
}