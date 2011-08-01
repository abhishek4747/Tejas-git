package memorysystem;

import generic.NewEvent;
import generic.RequestType;
import generic.SimulationElement;

public class NewCacheAcessEvent extends NewEvent 
{
	public NewCacheAcessEvent(long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker, 
			RequestType requestType, CacheRequestPacket cacheRequestPacket)
	{
		super(eventTime, requestingElement, processingElement, tieBreaker, cacheRequestPacket.getType());
		
	}

	public NewEvent handleEvent()
	{
		//FIXME: check for request pay-load.
		return null;
	}
}
