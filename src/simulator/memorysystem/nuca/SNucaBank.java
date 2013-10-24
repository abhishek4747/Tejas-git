
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
	protected Vector<Integer> bankId = new Vector<Integer>(2); //bank id of router(vector <row,column>)

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
		//Process the access
		CacheLine cl = this.processRequest(requestType, address,event);
		
		//IF HIT
		if (cl != null || nucaCache.missStatusHoldingRegister.containsWriteOfEvictedLine(address) )
		{
			//System.exit(0);
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
    protected void handleMemoryReadWrite(EventQueue eventQ, Event event) {
		//System.out.println(((AddressCarryingEvent)event).getDestinationBankId() + ""+ ((AddressCarryingEvent)event).getSourceBankId());
		AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
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
		long addr = addrEvent.getAddress();
		//System.err.println(addr);
		Vector<Integer> sourceId;
		Vector<Integer> destinationId;
		if(event.getRequestingElement().getClass() == MainMemoryController.class)
		{
			//System.err.println(event.getRequestingElement().getClass());
			sourceId = this.getId();
			destinationId = nucaCache.getBankId(addr);
			AddressCarryingEvent addressEvent = new AddressCarryingEvent(event.getEventQ(),
																		0,this, this.getRouter(), 
																		RequestType.Main_Mem_Response, 
																		addr,((AddressCarryingEvent)event).coreId,
																		sourceId,destinationId);
			this.getRouter().getPort().put(addressEvent);
		}
		//System.err.println(event.getRequestingElement().getClass());
		if(event.getRequestingElement().getClass() == Router.class)
		{
			//System.err.println(event.getRequestingElement().getClass());
			CacheLine evictedLine = this.fill(addr,MESI.EXCLUSIVE);
			if (evictedLine != null && 
					this.writePolicy != CacheConfig.WritePolicy.WRITE_THROUGH )
			{
				sourceId = new Vector<Integer>(this.getId());
				destinationId = (Vector<Integer>) nucaCache.getMemoryControllerId(nucaCache.getBankId(addr));
				//System.out.println("cache Miss  sending request to Main Memory"+destinationBankId + " to event"+ event);
				
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
		// TODO Auto-generated method stub
		return this;
	}
	
	public int getStartIdx(long addr) {
		long SetMask =( 1 << (numSetsBits) )- 1;
		int bankNumBits = (int) (Math.log(nucaCache.cacheRows)/Math.log(2));
		int startIdx = (int) ((addr >>> (blockSizeBits+bankNumBits)) & (SetMask));
		return startIdx;
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