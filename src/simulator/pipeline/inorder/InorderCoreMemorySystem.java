package pipeline.inorder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;

public class InorderCoreMemorySystem extends CoreMemorySystem {
	
	public InorderCoreMemorySystem(Core core)
	{
		super(core);
	}
	
	//To issue the request directly to L1 cache
	//missPenalty field has been added to accomodate the missPenalty incurred due to TLB miss
	public boolean issueRequestToL1Cache(RequestType requestType, 
											long address,
											InorderPipeline inorderPipeline)
	{
		int tlbMissPenalty = performTLBLookup(address, inorderPipeline);
		
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(getCore().getEventQueue(),
																	 l1Cache.getLatencyDelay() + tlbMissPenalty,
																	 this, 
																	 l1Cache,
																	 requestType, 
																	 address,
																	 core.getCore_number());
		
		// Check mshr isfull and do something
		if(L1MissStatusHoldingRegister.isFull())
		{
			return false;
		}
		
		//if not full add event to own mshr
		boolean newOMREntryCreated = L1MissStatusHoldingRegister.addOutstandingRequest(addressEvent);
		
		//if new OMREntry has been created, then request should be forwarded to lower cache
		//else, then a request for the same address exists in the mshr, hence another request is unnecessary
		if(newOMREntryCreated)
		{
			//attempt issue to lower level cache
			AddressCarryingEvent clone = (AddressCarryingEvent) addressEvent.clone();
			boolean isAddedinLowerMshr = this.l1Cache.addEvent(clone);
			if(!isAddedinLowerMshr)
			{
				//if lower level cache had its mshr full
				L1MissStatusHoldingRegister.handleLowerMshrFull(clone);
			}
			else
			{
				if(addressEvent.getRequestType() == RequestType.Cache_Write)
				{
					L1MissStatusHoldingRegister.removeEvent(addressEvent);
				}
			}
		}
		return true;
	}
	
	//To issue the request to instruction cache
	public void issueRequestToInstrCache(long address, InorderPipeline inorderPipeline)
	{
		int tlbMissPenalty = performTLBLookup(address, inorderPipeline);
		
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(getCore().getEventQueue(),
				 iCache.getLatencyDelay() + tlbMissPenalty,
				 this, 
				 iCache,
				 RequestType.Cache_Read, 
				 address,
				 core.getCore_number());

		//add event to own mshr
		boolean newOMREntryCreated = iMissStatusHoldingRegister.addOutstandingRequest(addressEvent);
		
		//if new OMREntry has been created, then request should be forwarded to lower cache
		//else, then a request for the same address exists in the mshr, hence another request is unnecessary
		if(newOMREntryCreated)
		{
			//attempt issue to lower level cache
			AddressCarryingEvent clone = (AddressCarryingEvent) addressEvent.clone();
			boolean isAddedinLowerMshr = this.iCache.addEvent(clone);
			if(!isAddedinLowerMshr)
			{
				//if lower level cache had its mshr full
				iMissStatusHoldingRegister.handleLowerMshrFull(clone);
			}
		}
	}
	
	private int performTLBLookup(long address, InorderPipeline inorderPipeline)
	{
		boolean TLBHit=TLBuffer.searchTLBForPhyAddr(address);
		int missPenalty=0;
		if(!TLBHit){
			missPenalty =TLBuffer.getMissPenalty();
			this.core.getExecutionEngineIn().setStallFetch(missPenalty);
			this.core.getExecutionEngineIn().setStallPipelinesExecute(inorderPipeline.getId(),missPenalty);
			inorderPipeline.getIfIdLatch().incrementStallCount(missPenalty);
		}
		return missPenalty;
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
		//handle memory response
		
		AddressCarryingEvent memResponse = (AddressCarryingEvent) event;
		long address = memResponse.getAddress();
		
		//if response comes from iCache, inform fetchunit
		if(memResponse.getRequestingElement() == iCache)
		{
			iMissStatusHoldingRegister.removeRequests(address);
			core.getExecutionEngineIn().getFetchUnitIn().processCompletionOfMemRequest(address);
		}
		
		//if response comes from l1Cache, inform memunit
		else if(memResponse.getRequestingElement() == l1Cache)
		{
			//TODO currently handling only reads
			L1MissStatusHoldingRegister.removeRequests(address);
			core.getExecutionEngineIn().getMemUnitIn().processCompletionOfMemRequest(address);
		}
		
		else
		{
			System.out.println("mem response received by inordercoreMemSys from unkown object : " + memResponse.getRequestingElement());
		}
	}
	
	public boolean isMshrFull()
	{
		if(L1MissStatusHoldingRegister.isFull() && iMissStatusHoldingRegister.isFull())
		{
			return true;
		}
		return false;
	}

}
