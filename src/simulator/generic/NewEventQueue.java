package generic;

import java.util.PriorityQueue;

public class NewEventQueue 
{
	private PriorityQueue<NewEvent> priorityQueue;
		
	public NewEventQueue() 
	{
		priorityQueue = new PriorityQueue<NewEvent>(1, new NewEventComparator());
	}
	
	public void addEvent(NewEvent newEvent)
	{
		priorityQueue.add(newEvent);
	}
	
	public void processEvents()
	{
		NewEvent newEvent;
		long eventTime;
		
		//FIXME :  the current clock time to be accessed from somewhere
		long currentClockTime = GlobalClock.currentTime;
		
		while(!priorityQueue.isEmpty())
		{
			//get the eventTime of the event on the head of the queue.
			eventTime = priorityQueue.peek().getEventTime().getTime();
			if (eventTime <= currentClockTime)
			{
				//remove the event at the head of the queue.
				newEvent = priorityQueue.remove();
				
				//If the event could not be handled, add it to the queue.
				newEvent.handleEvent(this);
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
}