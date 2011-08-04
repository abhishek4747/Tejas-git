package memorysystem;

import generic.NewEventQueue;
import generic.Time_t;
import generic.NewEvent;
import generic.RequestType;
import generic.SimulationElement;

/**
 * @author Moksh Upadhyay
 * Used to indicate to a memory element (generally caches) to inform 
 * them that a block has been received for some outstanding request
 */
public class BlockReadyEvent extends NewEvent
{
	/**
	 * Just stores the LSQ entry index if the ready event is for an LSQ.
	 * Stores the INVALID_INDEX otherwise.
	 */
	int lsqIndex = LSQ.INVALID_INDEX;
	
	//For the caches
	public BlockReadyEvent(Time_t eventTime, SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker,
			RequestType requestType) 
	{
		super(eventTime, requestingElement, processingElement, tieBreaker, requestType);
		// TODO Auto-generated constructor stub
	}
	 
	//For LSQ
	public BlockReadyEvent(Time_t eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker,
			RequestType requestType, int lsqIndex) 
	{
		super(eventTime, requestingElement, processingElement, tieBreaker,
				requestType);
		this.lsqIndex = lsqIndex;
	}

	@Override
	public NewEvent handleEvent(NewEventQueue newEventQueue)
	{
		return null;
	}
}
