package generic;

import java.util.Comparator;
import java.util.PriorityQueue;
import generic.Event;
import generic.EventComparator;

/**
 *discrete event simulator's priority queue of events
 */

public class EventQueue {
	
	private PriorityQueue<Event> eventQ;
	private Core core;
	
	public EventQueue(Core containingCore)
	{
		Comparator<Event> eventComparator = new EventComparator();
		eventQ = new PriorityQueue<Event>(1, eventComparator);
		
		core = containingCore;
	}
	
	public void processEvents()
	{
		//all events scheduled at or before core.Clock have to be handled
		Event polledEvent;
		long eventTime;
		
		while(eventQ.isEmpty() == false)
		{			
			eventTime  = eventQ.peek().getEventTime();
							
			if(eventTime <= core.getClock())
			{
				polledEvent = eventQ.poll();						
				polledEvent.handleEvent();
			}
			else
			{
				break;
			}
		}
	}
	
	public void addEvent(Event newEvent)
	{
		eventQ.add(newEvent);
	}
	
	public void removeEvent(Event event)
	{
		eventQ.remove(event);
	}

}