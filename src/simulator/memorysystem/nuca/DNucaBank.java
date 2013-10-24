package memorysystem.nuca;

import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.RequestType;
import generic.SimulationElement;

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

	DNucaBank(Vector<Integer> bankId,CacheConfig cacheParameters, CoreMemorySystem containingMemSys,DNuca nucaCache, NucaType nucaType)
    {
        super(bankId,cacheParameters,containingMemSys,nucaCache, nucaType);
        eventIdToHitMissList = new HashMap<Long, Vector<RequestType>>();
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
		else if (event.getRequestType() == RequestType.COPY_BLOCK)
		{
			handleCopyBlock(eventQ,event);
		}
		else if (event.getRequestType() == RequestType.CacheLine_Invalidate)
		{
			handleCacheLineInvalidate(eventQ,event);
		}
		else 
		{
			System.err.println(event.getRequestType());
			misc.Error.showErrorAndExit(" unexpected request came to cache bank");
		}
	}
    private void handleCacheLineInvalidate(EventQueue eventQ, Event event) 
    {
    	AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
    	/*System.err.println("Bank Id : " + this.getBankId());
    	System.err.println("Global cycle time :" + GlobalClock.getCurrentTime());
    	System.err.println("Address : " + addrEvent.getAddress());*/
    	CacheLine cl = this.access(addrEvent.getAddress());
    	cl.setState(MESI.INVALID);
	}
	private void handleCopyBlock(EventQueue eventQ, Event event) 
    {
    	AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
    	long addr = addrEvent.getAddress();
    	CacheLine evictedLine = this.fill(addr,MESI.EXCLUSIVE);
    	/*System.err.println("Core Id : " + event.coreId);
    	System.err.println("Handle Copy Block");
    	System.err.println("Source Id " + this.getBankId());
    	System.err.println("dest Id " + addrEvent.getSourceId());*/
    	AddressCarryingEvent eventToInvalidate = new AddressCarryingEvent(event.getEventQ(),
				 0,this, this.getRouter(), 
				 RequestType.CacheLine_Invalidate, 
				 addr,((AddressCarryingEvent)event).coreId,
				 this.getBankId(),addrEvent.getSourceId());
    	this.getRouter().getPort().put(eventToInvalidate);
		if (evictedLine != null && 
				this.writePolicy != CacheConfig.WritePolicy.WRITE_THROUGH )
		{
			Vector<Integer> sourceId = new Vector<Integer>(this.getId());
			Vector<Integer> destinationId = (Vector<Integer>) nucaCache.getMemoryControllerId(nucaCache.getBankId(addr));
			//System.out.println("cache Miss  sending request to Main Memory"+destinationBankId + " to event"+ event);
			
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
    	eventIdToHitMissList.get(addrEvent.event_id).add(addrEvent.getRequestType());
    	int bankset = ((DNuca)nucaCache).getBankSetId(addrEvent.getAddress());
    	bankset = ((DNuca)nucaCache).bankSetnum.get(bankset);
    	if(eventIdToHitMissList.get(addrEvent.event_id).size() == 
    			((DNuca)nucaCache).bankSetNumToBankIds.get(bankset).size())
    	{
    		if(eventIdToHitMissList.get(addrEvent.event_id).contains(RequestType.Cache_Hit))
    		{
    			int numOfOutStandingRequests = nucaCache.missStatusHoldingRegister.numOutStandingRequests(addrEvent);
				nucaCache.hits+=numOfOutStandingRequests; //
				nucaCache.noOfRequests += numOfOutStandingRequests;//
				sendCopyBlockRequest(addrEvent);
    			policy.updateEventOnHit(addrEvent, this);
    		}
    		else
    		{
    			AddressCarryingEvent tempEvent= policy.updateEventOnMiss( (AddressCarryingEvent)event,this);
				if(tempEvent != null)
				{
					tempEvent.getProcessingElement().getPort().put(tempEvent);
				}
    		}
    	}
	}
    void sendCopyBlockRequest(AddressCarryingEvent event)
    {
    	//System.err.println("Send Copy Block");
    	Vector<Integer> destination=new Vector<Integer>();
    	Vector<Integer> coreId = ArchitecturalComponent.getCores()[event.coreId].getId();
    	//System.err.println("Core Id : " + coreId);
    	int bankset = ((DNuca)nucaCache).getBankSetId(event.getAddress());
    	
    	int bankIndex = ((DNuca)nucaCache).bankSetNumToBankIds.get(((DNuca)nucaCache).bankSetnum.get(bankset)).indexOf(this.getBankId());
    	/*System.err.println("Bank Index : " + bankIndex);
    	System.err.println("Core ID : "+coreId);
    	System.err.println("Bank ID : "+bankId);*/
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
	    	/*System.err.println("Source : " + this.getId());
	    	System.err.println("Dest : " + destination);*/
			AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(event.getEventQ(),
					 0,this, 
					 event.getRequestingElement(),
					 RequestType.COPY_BLOCK,
					 event.getAddress(),event.coreId,
					 this.getId(),destination);
			this.getRouter().getPort().put(eventToBeSent);
    	}
    }
	public void handleAccess(EventQueue eventQ, AddressCarryingEvent event)
	{
		RequestType requestType = event.getRequestType();
		long address = event.getAddress();
		nucaCache.incrementTotalNucaBankAcesses(1);
		//Process the access
		CacheLine cl = this.processRequest(requestType, address,event);
		
		if(event.event_id==0)
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
				/*AddressCarryingEvent tempEvent= policy.updateEventOnMiss( (AddressCarryingEvent)event,this);
				if(tempEvent != null)
				{
					tempEvent.getProcessingElement().getPort().put(tempEvent);
				}*/
				policy.broadcastToOtherBanks(event, address,this);
			}
		}
		else
		{
			RequestType request;
			if (cl != null  || nucaCache.missStatusHoldingRegister.containsWriteOfEvictedLine(address))
			{
				request=RequestType.Cache_Hit;
			}
			else
			{
				request=RequestType.Cache_Miss;
			}
			
			AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(event.event_id,event.getEventQ(),
					 0,this, 
					 event.getRequestingElement(),
					 request,
					 address,event.coreId,
					 event.getDestinationId(),event.getSourceId());
			this.getRouter().getPort().put(eventToBeSent);
			
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