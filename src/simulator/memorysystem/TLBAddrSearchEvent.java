package memorysystem;

import generic.NewEvent;
import generic.NewEventQueue;
import generic.RequestType;
import generic.SimulationElement;
import generic.Time_t;

public class TLBAddrSearchEvent extends NewEvent
{
	long address;
	int lsqIndex;
	
	public TLBAddrSearchEvent(Time_t eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker,
			RequestType requestType, long address,
			int lsqIndex) 
	{
		super(eventTime, requestingElement, processingElement, tieBreaker,
				requestType);
		this.address = address;
		this.lsqIndex = lsqIndex;
	}

	public void handleEvent(NewEventQueue newEventQueue)
	{
		TLB processingTLB = (TLB)(this.getProcessingElement());
		
		// If Entry found in TLB
		if (processingTLB.searchTLBForPhyAddr(address)) 
		{
			//Validate the address in the LSQ right away
			newEventQueue.addEvent(new LSQValidateEvent(this.getEventTime(),//FIXME
														processingTLB,
														this.getRequestingElement(), 
														0, //tieBreaker,
														RequestType.VALIDATE_LSQ_ENTRY, 
														lsqIndex, 
														address));
		}
		else
		{
			//Add the requesting LSQ Index to Outstanding Request table
			boolean alreadyRequested = processingTLB.addOutstandingRequest(TLB.getPageID(address), lsqIndex);
			
			if (!alreadyRequested)
				//Fetch the physical address from from Page table
				newEventQueue.addEvent(new MainMemAccessForTLBEvent(this.getEventTime(),//FIXME
																	this.getProcessingElement(), 
																	0, //tieBreaker,
																	TLB.getPageID(address),
																	RequestType.MAIN_MEM_ACCESS_TLB));
		}
	}
}