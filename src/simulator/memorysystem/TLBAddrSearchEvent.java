package memorysystem;

import emulatorinterface.Newmain;
import generic.GlobalClock;
import generic.NewEvent;
import generic.NewEventQueue;
import generic.PortRequestEvent;
import generic.RequestType;
import generic.SimulationElement;
import generic.Time_t;

public class TLBAddrSearchEvent extends NewEvent
{
	long address;
	LSQEntry lsqEntry;
	
	public TLBAddrSearchEvent(Time_t eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker,
			RequestType requestType, long address,
			LSQEntry lsqEntry) 
	{
		super(eventTime, requestingElement, processingElement, tieBreaker,
				requestType);
		this.address = address;
		this.lsqEntry = lsqEntry;
	}

	public void handleEvent(NewEventQueue newEventQueue)
	{
		TLB processingTLB = (TLB)(this.getProcessingElement());
		
		// If Entry found in TLB
		if (processingTLB.searchTLBForPhyAddr(address)) 
		{
			//Validate the address in the LSQ right away
			newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker, 
					1, //noOfSlots,
					new LSQValidateEvent(this.getRequestingElement().getLatencyDelay(),//FIXME
														processingTLB,
														this.getRequestingElement(), 
														0, //tieBreaker,
														RequestType.VALIDATE_LSQ_ENTRY, 
														lsqEntry, 
														address)));
		}
		else
		{
			//Add the requesting LSQ Index to Outstanding Request table
			boolean alreadyRequested = processingTLB.addOutstandingRequest(TLB.getPageID(address), lsqEntry);
			
			if (!alreadyRequested)
				//Fetch the physical address from from Page table
				newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker,  
						1, //noOfSlots,
						new MainMemAccessForTLBEvent(MemorySystem.getMainMemLatencyDelay(),//FIXME
																	this.getProcessingElement(), 
																	0, //tieBreaker,
																	TLB.getPageID(address),
																	RequestType.MAIN_MEM_ACCESS_TLB)));
		}
	}
}
