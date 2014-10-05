package generic;

import java.util.Iterator;
import java.util.PriorityQueue;

import main.ArchitecturalComponent;
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
		if( priorityQueue.add(event) == false) {
			misc.Error.showErrorAndExit("Error in adding event to the event queue : " + event);
//			Event newEvent = event.clone();
//			priorityQueue.add(newEvent);
		}
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
				
//				XXX if(ArchitecturalComponent.getCore(0).getNoOfInstructionsExecuted()>3000000l) {
//					System.out.println("t(" + GlobalClock.getCurrentTime() + ") : " + event);
//				}
				
				if(event.serializationID==14518378l) {
					System.out.println("Culprit event : " + event);
				}
				
				if(event.serializationID==14518378l) {
					System.out.println("Culprit event : " + event);
				}
				
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
			Event event = iterator.next();			
			event.dump();
			/*if(event.getRequestType() == RequestType.PerformPulls)
			{
				System.out.println(event.getRequestType());
			}
			else if(event.getClass() == AddressCarryingEvent.class)
			{
				AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
				System.out.println(addrEvent.getRequestType() + "," + addrEvent.getAddress() + "," + addrEvent.coreId + "," + addrEvent.getProcessingElement() + "," + addrEvent.getRequestingElement() + ","+addrEvent.getEventTime());
			} else {
				System.out.println(event.getRequestType() + ","+ event.coreId + "," + event.getProcessingElement() + "," + event.getRequestingElement() + ","+event.getEventTime());
			}*/
		}
		System.out.println("------------------------------------------------------------------------------------");
	}
}