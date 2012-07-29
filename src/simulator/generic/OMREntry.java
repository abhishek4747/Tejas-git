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
}
