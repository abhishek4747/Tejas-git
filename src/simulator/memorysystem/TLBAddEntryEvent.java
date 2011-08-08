package memorysystem;

import java.util.ArrayList;

import generic.GlobalClock;
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
		TLB processingTLB = (TLB)(this.getProcessingElement());
		
		//Add the entry into the TLB
		processingTLB.addTLBEntry(pageID);
		
		//TODO : Pickup all outstanding requests and LSQValidate them
		ArrayList<Integer> outstandingRequestList = processingTLB.outstandingRequestTable.get(pageID);
		
		while (!outstandingRequestList.isEmpty())
		{
			newEventQueue.addEvent(new LSQValidateEvent(new Time_t(GlobalClock.getCurrentTime() +
																processingTLB.containingMemSys.lsqueue.getLatency().getTime()), //FIXME
														processingTLB,
														processingTLB.containingMemSys.lsqueue, 
														0, //tieBreaker,
														RequestType.VALIDATE_LSQ_ENTRY, 
														outstandingRequestList.remove(0), 
														pageID)); //FIXME : Right now, we are passing pageID in place of ADDRESS
		}
	}
}
