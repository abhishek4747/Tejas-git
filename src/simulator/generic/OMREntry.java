package generic;

import java.util.ArrayList;
import memorysystem.AddressCarryingEvent;

public class OMREntry {
	public ArrayList<AddressCarryingEvent> outStandingEvents;
	public AddressCarryingEvent eventToForward;
	
	public OMREntry(ArrayList<AddressCarryingEvent> outStandingEvent, AddressCarryingEvent eventToForward)
	{
		this.outStandingEvents = outStandingEvent;
		this.eventToForward = eventToForward;
	}
	
	public boolean containsWriteToAddress(long addr)
	{
		if(eventToForward != null && eventToForward.getRequestType() == RequestType.Cache_Write &&
				eventToForward.getAddress() == addr)
		{
			return true;
		}
		
		for(int i = 0; i < outStandingEvents.size(); i++)
		{
			if(outStandingEvents.get(i).getRequestType() == RequestType.Cache_Write &&
					outStandingEvents.get(i).getAddress() == addr)
			{
				return true;
			}
		}
		
		return false;
	}
}
