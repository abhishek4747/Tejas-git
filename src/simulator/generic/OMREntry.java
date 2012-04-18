package generic;

import java.util.ArrayList;

public class OMREntry {
	public ArrayList<Event> outStandingEvents;
	public boolean readyToProceed;
	public Event eventToForward;
	public OMREntry(ArrayList<Event> outStandingEvent,boolean readyToProceed,Event eventToForward)
	{
		this.outStandingEvents = outStandingEvent;
		this.readyToProceed = readyToProceed;
		this.eventToForward = eventToForward;
	}
}
