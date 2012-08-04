package generic;

import java.util.Iterator;
import java.util.PriorityQueue;

import memorysystem.AddressCarryingEvent;

public class EventQueue 
{
	private PriorityQueue<Event> priorityQueue;

	public EventQueue() 
	{
		priorityQueue = new PriorityQueue<Event>(1, new EventComparator());
	}
	
	public void addEvent(Event event)
	{
		priorityQueue.add(event);
	}
	
	public void processEvents()
	{
		Event event;
		long eventTime;
		
		long currentClockTime = GlobalClock.currentTime;
		
		while(!priorityQueue.isEmpty())
		{
			//get the eventTime of the event on the head of the queue.
			eventTime = priorityQueue.peek().getEventTime();
			if (eventTime <= currentClockTime)
			{
				//remove the event at the head of the queue.
				event = priorityQueue.remove();
				
				//If the event could not be handled, add it to the queue.
				//TODO This is in compliance with the new structure.
				event.getProcessingElement().handleEvent(this, event);
			}
			else
			{
				break;
			}
		}
	}
	
	public boolean isEmpty()
	{
		return priorityQueue.isEmpty();
	}
	
	public void dump()
	{
		System.out.println("------------------------------------------------------------------------------------");
		System.out.println("event queue size = " + priorityQueue.size());
		Iterator<Event> iterator = priorityQueue.iterator();
		while(iterator.hasNext())
		{
			AddressCarryingEvent event = (AddressCarryingEvent) iterator.next();
			System.out.println(event.getRequestType() + "," + event.getAddress() + "," + event.coreId);
		}
		System.out.println("------------------------------------------------------------------------------------");
	}
}