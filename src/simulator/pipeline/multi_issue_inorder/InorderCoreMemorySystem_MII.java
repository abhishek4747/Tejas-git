package pipeline.multi_issue_inorder;

import config.SimulationConfig;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;

public class InorderCoreMemorySystem_MII extends CoreMemorySystem {
	
	MultiIssueInorderExecutionEngine containingExecEngine;
	public int numOfLoads=0;
	public long numOfStores;
	public InorderCoreMemorySystem_MII(Core core)
	{
		super(core);
		core.getExecEngine().setCoreMemorySystem(this);
		containingExecEngine = (MultiIssueInorderExecutionEngine)core.getExecEngine();
	}
	
	//To issue the request directly to L1 cache
	//missPenalty field has been added to accomodate the missPenalty incurred due to TLB miss
	public boolean issueRequestToL1Cache(RequestType requestType, 
											long address)
	{
		MultiIssueInorderPipeline inorderPipeline = (MultiIssueInorderPipeline)core.getPipelineInterface();

		int tlbMissPenalty = performDTLBLookup(address, inorderPipeline);
		
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(getCore().getEventQueue(),
																	 l1Cache.getLatencyDelay() + tlbMissPenalty,
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
			misc.Error.showErrorAndExit("Unable to add event to l1 Cache's MSHR !!" + 
					"\nevent = " + addressEvent + 
					"\niCache = " + this.iCache);
		}
		
		containingExecEngine.updateNoOfMemRequests(1);
		if(requestType == RequestType.Cache_Read)
		{
			containingExecEngine.updateNoOfLd(1);
		}
		else if(requestType == RequestType.Cache_Write)
		{
			containingExecEngine.updateNoOfSt(1);
		}
		
		return true;
	}
	
	//To issue the request to instruction cache
	public void issueRequestToInstrCache(long address)
	{
		MultiIssueInorderPipeline inorderPipeline = (MultiIssueInorderPipeline)core.getPipelineInterface();
		
		int tlbMissPenalty = performITLBLookup(address, inorderPipeline);
		
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
	
	private int performITLBLookup(long address, MultiIssueInorderPipeline inorderPipeline)
	{
		boolean tLBHit = iTLB.searchTLBForPhyAddr(address);
		int missPenalty = 0;
		if(!tLBHit){
			missPenalty = iTLB.getMemoryPenalty();
		}
		return missPenalty;
	}
	
	private int performDTLBLookup(long address, MultiIssueInorderPipeline inorderPipeline)
	{
		boolean tLBHit = dTLB.searchTLBForPhyAddr(address);
		int missPenalty = 0;
		if(!tLBHit){
			missPenalty = dTLB.getMemoryPenalty();
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
			// iMissStatusHoldingRegister.removeRequestsByAddress(memResponse);
			containingExecEngine.getFetchUnitIn().processCompletionOfMemRequest(address);
		}
		
		//if response comes from l1Cache, inform memunit
		else if(memResponse.getRequestingElement() == l1Cache)
		{
			// L1MissStatusHoldingRegister.removeRequestsByAddress(memResponse);
			containingExecEngine.getMemUnitIn().processCompletionOfMemRequest(address);
		}
		
		else
		{
			System.out.println("mem response received by inordercoreMemSys from unkown object : " + memResponse.getRequestingElement());
		}
	}

	@Override
	public long getNumberOfMemoryRequests() {
		return containingExecEngine.noOfMemRequests;
	}

	@Override
	public long getNumberOfLoads() {
		return containingExecEngine.noOfLd;
	}

	@Override
	public long getNumberOfStores() {
		return containingExecEngine.noOfSt;
	}

	@Override
	public long getNumberOfValueForwardings() {
		return 0;
	}

}
