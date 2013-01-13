package pipeline.outoforder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.ExecCompleteEvent;
import generic.GlobalClock;
import generic.RequestType;
import generic.SimulationElement;
import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import memorysystem.LSQEntryContainingEvent;
import memorysystem.MemorySystem;

public class OutOrderCoreMemorySystem extends CoreMemorySystem {
	
	OutOrderExecutionEngine containingExecEngine;

	public OutOrderCoreMemorySystem(Core core) {
		super(core);
		core.getExecEngine().setCoreMemorySystem(this);
		containingExecEngine = (OutOrderExecutionEngine)core.getExecEngine();
	}
	

	
	//To issue the request to instruction cache
	public void issueRequestToInstrCache(long address)
	{
		int tlbMissPenalty = performTLBLookup(address);
		
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
	
	public void allocateLSQEntry(boolean isLoad, long address, ReorderBufferEntry robEntry)
	{
		if (!MemorySystem.bypassLSQ)
			robEntry.setLsqEntry(lsqueue.addEntry(isLoad, address, robEntry));
	}
	
	//To issue the request to LSQ
	public void issueRequestToLSQ(SimulationElement requestingElement, 
											ReorderBufferEntry robEntry)
	{
		if(robEntry.isOperand1Available() == false ||
						robEntry.isOperand2Available() == false ||
						robEntry.getAssociatedIWEntry() == null ||
						robEntry.getIssued() == false)
		{
			System.out.println("attempting to validate the address of a load/store that hasn't been issued");
		}
		
		lsqueue.getPort().put(
				new LSQEntryContainingEvent(
						getCore().getEventQueue(),
						lsqueue.getLatencyDelay(), 
						requestingElement, //Requesting Element
						lsqueue, 
						RequestType.Tell_LSQ_Addr_Ready,
						robEntry.getLsqEntry(),
						this.coreID));
	}
	
	//To issue the request directly to L1 cache
	public boolean issueRequestToL1Cache(RequestType requestType, 
											long address)
	{
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(getCore().getEventQueue(),
																	 l1Cache.getLatencyDelay(),
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
		
		if(L1MissStatusHoldingRegister.getCurrentSize() >= L1MissStatusHoldingRegister.getMSHRStructSize())
		{
			return false;
		}
		
		if(l1Cache.missStatusHoldingRegister.getCurrentSize() >= l1Cache.missStatusHoldingRegister.getMSHRStructSize()) {
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
					//L1MissStatusHoldingRegister.removeEvent(addressEvent);
				}
			}
		}
		return true;
	}
	
	//To commit Store in LSQ
	public void issueLSQCommit(ReorderBufferEntry robEntry)
	{
		lsqueue.getPort().put(
				 new LSQEntryContainingEvent(
						getCore().getEventQueue(),
						lsqueue.getLatencyDelay(),
						null,
						lsqueue, 
						RequestType.LSQ_Commit, 
						robEntry.getLsqEntry(),
						this.coreID));
	}
	
	private int performTLBLookup(long address)
	{
		boolean TLBHit=TLBuffer.searchTLBForPhyAddr(address);
		int missPenalty=0;
		if(!TLBHit){
			missPenalty =TLBuffer.getMissPenalty();
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
			iMissStatusHoldingRegister.removeRequests(memResponse);
			containingExecEngine.getFetcher().processCompletionOfMemRequest(address);
		}
		
		//if response comes from l1Cache, inform memunit
		else if(memResponse.getRequestingElement() == l1Cache)
		{
			L1MissStatusHoldingRegister.removeRequests(memResponse);
			lsqueue.handleMemResponse(address);
		}
		
		else
		{
			System.out.println("mem response received by outordercoreMemSys from unkown object : " + memResponse.getRequestingElement());
		}
	}
	
	public void sendExecComplete(ReorderBufferEntry robEntry)
	{
		getCore().getEventQueue().addEvent(
				new ExecCompleteEvent(
						getCore().getEventQueue(),
						GlobalClock.getCurrentTime(),
						null,
						containingExecEngine.getExecuter(),
						RequestType.EXEC_COMPLETE,
						robEntry));
	}

}
