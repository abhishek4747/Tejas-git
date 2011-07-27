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
}