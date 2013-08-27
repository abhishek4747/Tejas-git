package pipeline.outoforder;

import java.util.ArrayList;

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

		//attempt issue to lower level cache
		AddressCarryingEvent clone = (AddressCarryingEvent) addressEvent.clone();
		boolean isAddedinLowerMshr = this.iCache.addEvent(clone);
		if(!isAddedinLowerMshr)
		{
			misc.Error.showErrorAndExit("Unable to add event to iCache's MSHR !!" + 
				"\nevent = " + addressEvent + 
				"\niCache = " + this.iCache);
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
		
		if(l1Cache.missStatusHoldingRegister.getCurrentSize() >= l1Cache.missStatusHoldingRegister.getMSHRStructSize()) {
			return false;
		}
		
		//attempt issue to lower level cache
		AddressCarryingEvent clone = (AddressCarryingEvent) addressEvent.clone();
		boolean isAddedinLowerMshr = this.l1Cache.addEvent(clone);
		if(!isAddedinLowerMshr)
		{
			misc.Error.showErrorAndExit("Unable to add event to l1 cache's MSHR !!" + 
					"\nevent = " + addressEvent + 
					"\nl1Cache = " + this.l1Cache);
		}
		else
		{
			if(addressEvent.getRequestType() == RequestType.Cache_Write)
			{
				//L1MissStatusHoldingRegister.removeEvent(addressEvent);
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
			missPenalty =TLBuffer.getMemoryPenalty();
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
			/*//FIXME change 3//ArrayList<AddressCarryingEvent> handledRequests = iMissStatusHoldingRegister.removeRequestsByAddress(memResponse);
			for(int i = 0; i < handledRequests.size(); i++)
			{
				containingExecEngine.getFetcher().processCompletionOfMemRequest(handledRequests.get(i).getAddress());
			}*/
			
			containingExecEngine.getFetcher().processCompletionOfMemRequest(address);
		}
		
		//if response comes from l1Cache, inform memunit
		else if(memResponse.getRequestingElement() == l1Cache)
		{
			/*//FIXME change 4//ArrayList<AddressCarryingEvent> handledRequests = L1MissStatusHoldingRegister.removeRequestsByAddress(memResponse);
			for(int i = 0; i < handledRequests.size(); i++)
			{
				System.out.println("mem response for " + handledRequests.get(i));
				lsqueue.handleMemResponse(handledRequests.get(i).getAddress());
			}*/
			
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
