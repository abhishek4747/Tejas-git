package memorysystem;

import generic.*;

public class LSQCommitEventFromROB extends NewEvent
{
	int lsqIndex;
	
	public LSQCommitEventFromROB(Time_t eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker,
			RequestType requestType, int lsqIndex) 
	{
		super(eventTime, requestingElement, processingElement, tieBreaker,
				requestType);
		this.lsqIndex = lsqIndex;
	}

	public void handleEvent(NewEventQueue newEventQueue)
	{
		
	}
}
