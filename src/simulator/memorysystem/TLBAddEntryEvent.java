package memorysystem;

import generic.NewEvent;
import generic.NewEventQueue;
import generic.RequestType;
import generic.SimulationElement;
import generic.Time_t;

public class TLBAddEntryEvent extends NewEvent
{
	long pageID;
	
	public TLBAddEntryEvent(Time_t eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker,
			RequestType requestType, long pageID) 
	{
		super(eventTime, requestingElement, processingElement, tieBreaker,
				requestType);
		this.pageID = pageID;
	}
	
	public void handleEvent(NewEventQueue newEventQueue)
	{
		//Add the entry into the TLB
		((TLB)(this.getProcessingElement())).addTLBEntry(TLB.getPageID(pageID));
		
		newEventQueue.addEntry(new LSQValidateEvent(containingMemSys,
								lsqIndex,
								addr,
								MemEventQueue.clock
									+ containingMemSys.lsqueue.getLatency()));
	}
}
