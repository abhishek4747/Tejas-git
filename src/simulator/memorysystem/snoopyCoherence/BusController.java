package memorysystem.snoopyCoherence;

import generic.EventQueue;
import generic.GlobalClock;
import generic.RequestType;
import generic.SimulationElement;

import java.util.ArrayList;

import generic.Event;

import memorysystem.AddressCarryingEvent;
import memorysystem.CacheLine;
import memorysystem.Cache;
import memorysystem.MESI;

public class BusController 
{
	
	private final int busOccupancy = 1;
	private int numBuses;
	private Cache sharedMem;
	protected ArrayList<Cache> upperLevel;
	protected Cache lowerCache;
//	private Bus busSet[];
	private long busBusyUntil[];
	
	public BusController(ArrayList<Cache> upperLevel, Cache lowerCache, int numberOfBuses, Cache sharedMem) 
	{
		super();
		this.numBuses = numberOfBuses;
		this.sharedMem = sharedMem;
		this.upperLevel = upperLevel;
		this.lowerCache = lowerCache;
		busBusyUntil = new long[numberOfBuses];
		for (int i = 0; i < numberOfBuses; i++)
		{
			busBusyUntil[i] = 0;
		}
	}
	
	public void processWriteHit(EventQueue eventQ, Cache requestingCache, CacheLine cl, long address)
	{
		if (cl.getState() == MESI.MODIFIED);
		else if (cl.getState() == MESI.EXCLUSIVE)
			cl.setState(MESI.MODIFIED);
		else if (cl.getState() == MESI.SHARED)
		{			
			//Put the invalidate events for other cores
			ArrayList<Event> eventList = new ArrayList<Event>();
			for (int i = 0; i < upperLevel.size(); i++)
			{
				Cache destCache = upperLevel.get(i);
				if (destCache != requestingCache && destCache.access(address) != null)
					eventList.add(
							new AddressCarryingEvent(
									eventQ, 
									destCache.getLatencyDelay(),
									requestingCache,
									destCache,
									RequestType.MESI_Invalidate, 
									address));
			}
			this.getBusAndPutEvents(eventList);
			
			//Don't wait for the replies and set the state to modified
			cl.setState(MESI.MODIFIED);
		}
		else
		{
			System.out.println("Forbidden condition reached");
		}
	}
	
	public void processReadMiss(EventQueue eventQ, Cache requestingCache, long address)
	{
		for (int i = 0; i < upperLevel.size(); i++)
		{
			CacheLine cacheLine = upperLevel.get(i).access(address);
			if (cacheLine != null)
				switch (cacheLine.getState())
				{
				case MODIFIED:
					this.getBusAndPutEvent(
							new AddressCarryingEvent(
								eventQ,
								upperLevel.get(i).getLatencyDelay(),
								requestingCache,
								upperLevel.get(i),
								RequestType.Request_for_modified_copy, 
								address));
					return;
				case EXCLUSIVE:
					this.getBusAndPutEvent(
							new AddressCarryingEvent(
								eventQ,
								upperLevel.get(i).getLatencyDelay(),
								requestingCache,
								upperLevel.get(i),
								RequestType.Request_for_copy, 
								address));
					return;
				case SHARED:
					this.getBusAndPutEvent(
							new AddressCarryingEvent(
								eventQ,
								upperLevel.get(i).getLatencyDelay(),
								requestingCache,
								upperLevel.get(i),
								RequestType.Request_for_copy, 
								address));
					return;
				}
		}
		
		//Store shared memory copy in the cache
		this.getBusAndPutEvent(
				new AddressCarryingEvent(
					eventQ,
					lowerCache.getLatencyDelay(),
					requestingCache,
					lowerCache,
					RequestType.Cache_Read, 
					address));
	}
	
	public void processWriteMiss(EventQueue eventQ, Cache requestingCache, long address)
	{
		for (int i = 0; i < upperLevel.size(); i++)
		{
			CacheLine cacheLine = upperLevel.get(i).access(address);
			if (cacheLine != null)
				switch (cacheLine.getState())
				{
				case MODIFIED:
					this.getBusAndPutEvent(
							new AddressCarryingEvent(
								eventQ,
								upperLevel.get(i).getLatencyDelay(),
								requestingCache,
								upperLevel.get(i),
								RequestType.Write_Modified_to_sharedmem, 
								address));
					return;
				case EXCLUSIVE:
					this.getBusAndPutEvent(
							new AddressCarryingEvent(
								eventQ,
								upperLevel.get(i).getLatencyDelay(),
								requestingCache,
								upperLevel.get(i),
								RequestType.MESI_Invalidate, 
								address));
					this.getBusAndPutEvent(
							new AddressCarryingEvent(
									eventQ,
									lowerCache.getLatencyDelay(),
									requestingCache,
									lowerCache,
									RequestType.Cache_Read, 
									address));
					return;
				case SHARED:
					ArrayList<Event> eventList = new ArrayList<Event>();
					eventList.add(
							new AddressCarryingEvent(
									eventQ,
									upperLevel.get(i).getLatencyDelay(),
									requestingCache,
									upperLevel.get(i),
									RequestType.MESI_Invalidate, 
									address));
					for (int j = i+1; j < upperLevel.size(); j++)
					{
						if (upperLevel.get(j).access(address) != null)
							eventList.add(
									new AddressCarryingEvent(
											eventQ,
											upperLevel.get(j).getLatencyDelay(),
											requestingCache,
											upperLevel.get(j),
											RequestType.MESI_Invalidate, 
											address));
					}
					this.getBusAndPutEvents(eventList);

					this.getBusAndPutEvent(
							new AddressCarryingEvent(
									eventQ,
									lowerCache.getLatencyDelay(),
									requestingCache,
									lowerCache,
									RequestType.Cache_Read, 
									address));
					return;
				}
		}
		
		//Store shared memory copy in the cache
		this.getBusAndPutEvent(
				new AddressCarryingEvent(
					eventQ,
					lowerCache.getLatencyDelay(),
					requestingCache,
					lowerCache,
					RequestType.Cache_Read, 
					address));
	}
	
	public void getBusAndPutEvents(ArrayList<Event> eventList)
	{
		int availableBusID = 0;
		for(int i=0; i<numBuses; i++)
		{
			if(busBusyUntil[i]< 
					busBusyUntil[availableBusID])
			{
				availableBusID = i;
			}
		}
		
		if (busBusyUntil[availableBusID] < GlobalClock.getCurrentTime())
		{
			busBusyUntil[availableBusID] = GlobalClock.getCurrentTime() + sharedMem.getStepSize();
			for (int i = 0; i < eventList.size(); i++)
			{
				eventList.get(i).addEventTime(sharedMem.getStepSize());
				eventList.get(i).getProcessingElement().getPort().put(eventList.get(i));
			}
		}
		else
		{
			busBusyUntil[availableBusID] += sharedMem.getStepSize();
			for (int i = 0; i < eventList.size(); i++)
			{
				eventList.get(i).addEventTime(busBusyUntil[availableBusID] - GlobalClock.getCurrentTime());
				eventList.get(i).getProcessingElement().getPort().put(eventList.get(i));
			}
		}
	}
	public void getBusAndPutEvent(Event event)
	{
		int availableBusID = 0;
		for(int i=0; i<numBuses; i++)
		{
			if(busBusyUntil[i]< 
					busBusyUntil[availableBusID])
			{
				availableBusID = i;
			}
		}
		
		if (busBusyUntil[availableBusID] < GlobalClock.getCurrentTime())
		{
			busBusyUntil[availableBusID] = GlobalClock.getCurrentTime() + (busOccupancy * sharedMem.getStepSize());
			event.addEventTime(busOccupancy * sharedMem.getStepSize());
			event.getProcessingElement().getPort().put(event);
		}
		else
		{
			busBusyUntil[availableBusID] += sharedMem.getStepSize();
			event.addEventTime(busBusyUntil[availableBusID] - GlobalClock.getCurrentTime());
			event.getProcessingElement().getPort().put(event);
		}
	}
}