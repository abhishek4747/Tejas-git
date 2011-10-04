package generic;

import java.util.PriorityQueue;

public class EventQueue 
{
	private PriorityQueue<Event> priorityQueue;
	private Core[] coresHandled;

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
			eventTime = priorityQueue.peek().getEventTime().getTime();
			if (eventTime <= currentClockTime)
			{
				//remove the event at the head of the queue.
				event = priorityQueue.remove();
				
				//If the event could not be handled, add it to the queue.
				event.handleEvent(this);
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
		
	public Core[] getCoresHandled() {
		return coresHandled;
	}

	public void setCoresHandled(Core[] coresHandled) {
		this.coresHandled = coresHandled;
	}
}