package pipeline.outoforder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.ExecCompleteEvent;
import generic.GlobalClock;
import generic.RequestType;
import generic.SimulationElement;
import main.ArchitecturalComponent;
import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import memorysystem.LSQEntryContainingEvent;
import memorysystem.coherence.Directory;

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
		int tlbMissPenalty = performITLBLookup(address);
		
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(getCore().getEventQueue(),
				 iCache.getLatencyDelay() + tlbMissPenalty,
				 this, 
				 iCache,
				 RequestType.Cache_Read, 
				 address);

		//attempt issue to lower level cache
		this.iCache.getPort().put(addressEvent);
	}
	
	public void allocateLSQEntry(boolean isLoad, long address, ReorderBufferEntry robEntry)
	{
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
			misc.Error.showErrorAndExit("attempting to validate the address of a load/store that hasn't been issued");
		}
		
		lsqueue.getPort().put(
				new LSQEntryContainingEvent(
						getCore().getEventQueue(),
						lsqueue.getLatencyDelay(), 
						requestingElement,
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
												 address);
		
		
		if(l1Cache.missStatusHoldingRegister.getCurrentSize() >= l1Cache.missStatusHoldingRegister.getMSHRStructSize()) {
//			XXX if(ArchitecturalComponent.getCore(0).getNoOfInstructionsExecuted()>3000000l) {
//				System.out.println("------------ MSHR of L1. Size : " + this.l1Cache.getMissStatusHoldingRegister().getCurrentSize() + " at time " + GlobalClock.getCurrentTime());
//				this.l1Cache.getMissStatusHoldingRegister().dump();
//				
//				System.out.println("------------ Directory Pending Events ---------------");
//				((Directory)this.l1Cache.mycoherence).toStringPendingEvents();
//			}
			return false;
		}
		
		//attempt issue to lower level cache
		this.l1Cache.getPort().put(addressEvent);
				
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
	
	private int performITLBLookup(long address)
	{
		boolean tLBHit = iTLB.searchTLBForPhyAddr(address);
		int missPenalty = 0;
		if(!tLBHit){
			missPenalty = iTLB.getMemoryPenalty();
		}
		
		return missPenalty;
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
		//handle memory response
		
		AddressCarryingEvent memResponse = (AddressCarryingEvent) event;
		long address = memResponse.getAddress();
		
		// Unified cache scenario
		if(iCache==l1Cache)
		{
			containingExecEngine.getFetcher().processCompletionOfMemRequest(address);
			lsqueue.handleMemResponse(address);
		}

		//if response comes from iCache, inform fetchunit
		else if(memResponse.getRequestingElement() == iCache)
		{
			containingExecEngine.getFetcher().processCompletionOfMemRequest(address);
		}
		
		//if response comes from l1Cache, inform memunit
		else if(memResponse.getRequestingElement() == l1Cache)
		{
			lsqueue.handleMemResponse(address);
		}
		
		else
		{
			misc.Error.showErrorAndExit("mem response received by outordercoreMemSys from unkown object : " + memResponse.getRequestingElement());
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

	@Override
	public long getNumberOfMemoryRequests() {
		return getLsqueue().noOfMemRequests;
	}

	@Override
	public long getNumberOfLoads() {
		return getLsqueue().NoOfLd;
	}

	@Override
	public long getNumberOfStores() {
		return getLsqueue().NoOfSt;
	}

	@Override
	public long getNumberOfValueForwardings() {
		return getLsqueue().NoOfForwards;
	}

	@Override
	public void setNumberOfMemoryRequests(long numMemoryRequests) {
		getLsqueue().noOfMemRequests = numMemoryRequests;
	}

	@Override
	public void setNumberOfLoads(long numLoads) {
		getLsqueue().NoOfLd = numLoads;
	}

	@Override
	public void setNumberOfStores(long numStores) {
		getLsqueue().NoOfSt = numStores;
	}

	@Override
	public void setNumberOfValueForwardings(long numValueForwardings) {
		getLsqueue().NoOfForwards = numValueForwardings;
	}

}
