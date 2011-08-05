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
		// If Entry found in TLB
		if (((TLB)(this.getProcessingElement())).searchTLBForPhyAddr(address)) 
		{
			//Validate the address in the LSQ right away
			newEventQueue.addEvent(new LSQValidateEvent(this.getEventTime(),//FIXME
														this.getProcessingElement(),
														this.getRequestingElement(), 
														0, //tieBreaker,
														RequestType.VALIDATE_LSQ_ENTRY, 
														lsqIndex, 
														address));
		}
		else
		{
			//TODO :  Add the requesting LSQ Index to Outstanding Request table
			
			//Fetch the physical address from from Page table
			newEventQueue.addEntry(new MainMemAccessEvent(containingMemSys, 
																							this, 
																							pageID, 
																							virtualAddr, 
																							lsqueue, 
																							index, 
																							MemEventQueue.clock
																							+ SystemConfig.mainMemoryLatency));
		}
	}
}
