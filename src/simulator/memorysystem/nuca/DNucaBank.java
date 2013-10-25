package memorysystem.nuca;

import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import java.util.HashMap;
import java.util.Vector;

import main.ArchitecturalComponent;
import memorysystem.AddressCarryingEvent;
import memorysystem.CacheLine;
import memorysystem.CoreMemorySystem;
import memorysystem.MESI;
import memorysystem.MainMemoryController;
import memorysystem.MemorySystem;
import memorysystem.nuca.NucaCache.NucaType;
import net.NocInterface;
import net.Router;
import net.NOC.CONNECTIONTYPE;
import config.CacheConfig;
import config.SystemConfig;

public class DNucaBank extends NucaCacheBank implements NocInterface
{
	public HashMap<Long,Vector<RequestType>> eventIdToHitMissList;
	public HashMap<Long,Vector<Integer>> eventIdToHitBankId;
	static int sum;

	DNucaBank(Vector<Integer> bankId,CacheConfig cacheParameters, CoreMemorySystem containingMemSys,DNuca nucaCache, NucaType nucaType)
    {
        super(bankId,cacheParameters,containingMemSys,nucaCache, nucaType);
        eventIdToHitMissList = new HashMap<Long, Vector<RequestType>>();
        eventIdToHitBankId = new HashMap<Long, Vector<Integer>>();
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
		else if (event.getRequestType() == RequestType.Cache_Hit||
				event.getRequestType() == RequestType.Cache_Miss)
		{
			handleCacheHitMiss(eventQ, event);
		}
		else if (event.getRequestType() == RequestType.Send_Migrate_Block)
		{
			handleSendCopyBlock(eventQ,event);
		}
		else if (event.getRequestType() == RequestType.Migrate_Block)
		{
			handleCopyBlock(eventQ,event);
		}
		else 
		{
			System.err.println(event.getRequestType());
			misc.Error.showErrorAndExit(" unexpected request came to cache bank");
		}
	}
    private void handleSendCopyBlock(EventQueue eventQ, Event event) 
    {
    	AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
    	
    	nucaCache.updateMaxHopLength(addrEvent.hopLength,addrEvent);
		nucaCache.updateMinHopLength(addrEvent.hopLength);
		nucaCache.updateAverageHopLength(addrEvent.hopLength);
    	sendCopyBlockRequest(addrEvent);
    	CacheLine cl = this.access(addrEvent.getAddress());
    	if(cl!=null)//if line is already invalid
    		cl.setState(MESI.INVALID);
	}
    void sendCopyBlockRequest(AddressCarryingEvent event)
    {
    	Vector<Integer> destination=new Vector<Integer>();
    	Vector<Integer> coreId = ArchitecturalComponent.getCores()[event.coreId].getId();
    	
    	int bankset = ((DNuca)nucaCache).getBankSetId(event.getAddress());
    	
    	int bankIndex = ((DNuca)nucaCache).bankSetNumToBankIds.get(((DNuca)nucaCache).bankSetnum.get(bankset)).indexOf(this.getBankId());
    	if(coreId.get(1)-this.getBankId().get(1)>0)
    	{
    		destination = ((DNuca)nucaCache).bankSetNumToBankIds.get(bankset).get(bankIndex+1);
    	}
    	else if(coreId.get(1)-this.getBankId().get(1)<0)
    	{
    		destination = ((DNuca)nucaCache).bankSetNumToBankIds.get(bankset).get(bankIndex-1);
    	}
    	
    	if(coreId.get(1)-this.getBankId().get(1)!=0)
	    {
			AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(event.getEventQ(),
					 0,this, 
					 this.getRouter(),
					 RequestType.Migrate_Block,
					 event.getAddress(),event.coreId,
					 this.getId(),destination);
			this.getRouter().getPort().put(eventToBeSent);
    	}
    }
	private void handleCopyBlock(EventQueue eventQ, Event event) 
    {
    	AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
    	nucaCache.updateMaxHopLength(addrEvent.hopLength,addrEvent);
		nucaCache.updateMinHopLength(addrEvent.hopLength);
		nucaCache.updateAverageHopLength(addrEvent.hopLength);
    	long addr = addrEvent.getAddress();
    	
    	CacheLine evictedLine = this.fill(addr,MESI.EXCLUSIVE);
    	if (evictedLine != null && 
				this.writePolicy != CacheConfig.WritePolicy.WRITE_THROUGH )
		{
			Vector<Integer> sourceId = new Vector<Integer>(this.getId());
			Vector<Integer> destinationId = (Vector<Integer>) nucaCache.getMemoryControllerId(nucaCache.getBankId(addr));
			
			AddressCarryingEvent addressEvent = new AddressCarryingEvent(event.getEventQ(),
																		 0,this, this.getRouter(), 
																		 RequestType.Main_Mem_Write, 
																		 addr,((AddressCarryingEvent)event).coreId,
																		 sourceId,destinationId);
			this.getRouter().getPort().put(addressEvent);
		}
	}
	public void handleCacheHitMiss(EventQueue eventQ, Event event) 
    {
    	AddressCarryingEvent addrEvent = (AddressCarryingEvent)event;
    	nucaCache.updateMaxHopLength(addrEvent.hopLength,addrEvent);
		nucaCache.updateMinHopLength(addrEvent.hopLength);
		nucaCache.updateAverageHopLength(addrEvent.hopLength);
    	eventIdToHitMissList.get(addrEvent.event_id).add(addrEvent.getRequestType());
    	if(addrEvent.getRequestType()==RequestType.Cache_Hit)
    	{	
    		int numOfOutStandingRequests = nucaCache.missStatusHoldingRegister.numOutStandingRequests(addrEvent);
			nucaCache.hits+=numOfOutStandingRequests;
			nucaCache.noOfRequests += numOfOutStandingRequests;
			policy.updateEventOnHit(addrEvent, this);
			eventIdToHitBankId.put(addrEvent.event_id, addrEvent.getSourceId());
    	}
    	int bankset = ((DNuca)nucaCache).getBankSetId(addrEvent.getAddress());
    	bankset = ((DNuca)nucaCache).bankSetnum.get(bankset);
    	if(eventIdToHitMissList.get(addrEvent.event_id).size() == 
    			((DNuca)nucaCache).bankSetNumToBankIds.get(bankset).size())
    	{
    		if(eventIdToHitMissList.get(addrEvent.event_id).contains(RequestType.Cache_Hit))
    		{
    			
    			
				@SuppressWarnings("unchecked")
				AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(event.getEventQ(),
						 0,this, 
						 this.getRouter(),
						 RequestType.Send_Migrate_Block,
						 addrEvent.getAddress(),event.coreId,
						 this.getId(),(Vector<Integer>)eventIdToHitBankId.get(addrEvent.event_id).clone());
				this.getRouter().getPort().put(eventToBeSent);
    		}
    		else
    		{
    			AddressCarryingEvent tempEvent= policy.updateEventOnMiss( (AddressCarryingEvent)event,this);
				if(tempEvent != null)
				{
					tempEvent.getProcessingElement().getPort().put(tempEvent);
				}
    		}
			eventIdToHitBankId.remove(addrEvent.event_id);
    		eventIdToHitMissList.remove(addrEvent.event_id);
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
		
		if(event.event_id==0) //Broadcast has not been done yet
		{
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
				policy.broadcastToOtherBanks(event, address,this);
			}
		}
		else
		{
			RequestType request;
			if (cl != null || nucaCache.missStatusHoldingRegister.containsWriteOfEvictedLine(address))
			{
				request=RequestType.Cache_Hit;
			}
			else
			{
				request=RequestType.Cache_Miss;
			}
			
			AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(event.event_id,event.getEventQ(),
					 0,this, 
					 this.getRouter(),
					 request,
					 address,event.coreId,
					 this.getBankId(),event.getSourceId());
			this.getRouter().getPort().put(eventToBeSent);
			
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

    	nucaCache.updateMaxHopLength(addrEvent.hopLength,(AddressCarryingEvent)event);
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