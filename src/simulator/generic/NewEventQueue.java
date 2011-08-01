package generic;

import java.util.PriorityQueue;

public class NewEventQueue 
{
	private PriorityQueue<NewEvent> priorityQueue;
		
	public NewEventQueue() 
	{
		priorityQueue = new PriorityQueue<NewEvent>(0, new NewEventComparator());
	}
	
	public void addEvent(NewEvent newEvent)
	{
		priorityQueue.add(newEvent);
	}
	
	public void processEvents()
	{
		NewEvent newEvent;
		long eventTime;
		long currentClockTime = 0; //FIXME :  the current clock time to be accessed from somewhere
		
		while(!priorityQueue.isEmpty())
		{
			eventTime = priorityQueue.peek().getEventTime();
			if (eventTime <= currentClockTime)
			{
				newEvent = priorityQueue.poll();
				newEvent.getProcessingElement().
					processRequest(newEvent.getSimulationRequest());
			}
		}
	}
}