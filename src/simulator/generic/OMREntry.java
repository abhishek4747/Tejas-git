package generic;

import java.util.ArrayList;
import memorysystem.AddressCarryingEvent;

public class OMREntry {
	public ArrayList<Event> outStandingEvents;
	public boolean readyToProceed;
	public AddressCarryingEvent eventToForward;
	
	public OMREntry(ArrayList<Event> outStandingEvent,boolean readyToProceed,AddressCarryingEvent eventToForward)
	{
		this.outStandingEvents = outStandingEvent;
		this.readyToProceed = readyToProceed;
		this.eventToForward = eventToForward;
	}
	
	public boolean containsWrite()
	{
		boolean contains = false;
		
		if(eventToForward != null && eventToForward.getRequestType() != RequestType.Cache_Write)
		{
			return false;
		}
		
		for(int i = 0; i < outStandingEvents.size(); i++)
		{
			if(outStandingEvents.get(i).getRequestType() == RequestType.Cache_Write)
			{
				contains = true;
			}
		}
		
		return contains;
	}
}
