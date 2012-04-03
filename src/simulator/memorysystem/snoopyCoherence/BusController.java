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
import misc.Util;

public class BusController 
{
	
	private int busOccupancy;
	private int numBuses;
	private long busMask;
	private Cache sharedMem;
	protected ArrayList<Cache> upperLevel;
	protected Cache lowerCache;
//	private Bus busSet[];
	private long busBusyUntil[];
	
	public BusController(ArrayList<Cache> upperLevel, Cache lowerCache, int numberOfBuses, Cache sharedMem, int occupancy) 
	{
		super();
		this.numBuses = numberOfBuses;
		this.busMask = numberOfBuses-1;
//		System.out.println("BusMask = "+ busMask);
		this.busOccupancy = occupancy;
		this.sharedMem = sharedMem;
		this.upperLevel = upperLevel;
		this.lowerCache = lowerCache;
		busBusyUntil = new long[numberOfBuses];
		for (int i = 0; i < numberOfBuses; i++)
		{
			busBusyUntil[i] = 0;
		}
	}
	
	public void processWriteHit(EventQueue eventQ, Cache requestingCache, CacheLine cl, long address,int coreId)
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
									address,
									coreId));
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
	
	public void processReadMiss(EventQueue eventQ, Cache requestingCache, long address,int coreId)
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
								address,
								coreId));
					return;
				case EXCLUSIVE:
					this.getBusAndPutEvent(
							new AddressCarryingEvent(
								eventQ,
								upperLevel.get(i).getLatencyDelay(),
								requestingCache,
								upperLevel.get(i),
								RequestType.Request_for_copy, 
								address,
								coreId));
					return;
				case SHARED:
					this.getBusAndPutEvent(
							new AddressCarryingEvent(
								eventQ,
								upperLevel.get(i).getLatencyDelay(),
								requestingCache,
								upperLevel.get(i),
								RequestType.Request_for_copy, 
								address,
								coreId));
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
					address,
					coreId));
	}
	
	public void processWriteMiss(EventQueue eventQ, Cache requestingCache, long address,int coreId)
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
								address,
								coreId));
					return;
				case EXCLUSIVE:
					this.getBusAndPutEvent(
							new AddressCarryingEvent(
								eventQ,
								upperLevel.get(i).getLatencyDelay(),
								requestingCache,
								upperLevel.get(i),
								RequestType.MESI_Invalidate, 
								address,
								coreId));
					this.getBusAndPutEvent(
							new AddressCarryingEvent(
									eventQ,
									lowerCache.getLatencyDelay(),
									requestingCache,
									lowerCache,
									RequestType.Cache_Read, 
									address,
									coreId));
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
									address,
									coreId));
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
											address,
											coreId));
					}
					this.getBusAndPutEvents(eventList);

					this.getBusAndPutEvent(
							new AddressCarryingEvent(
									eventQ,
									lowerCache.getLatencyDelay(),
									requestingCache,
									lowerCache,
									RequestType.Cache_Read, 
									address,
									coreId));
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
					address,
					coreId));
	}
	
	public void getBusAndPutEvents(ArrayList<Event> eventList)
	{
		if (eventList.isEmpty())
			return;
		
		int busID = 0;

		//Address multiplexing
		long address = ((AddressCarryingEvent)(eventList.get(0))).getAddress();
		busID = (int)(address & busMask);
//		System.out.println("Bus ID : "+busID);
		
		
//		for(int i=0; i<numBuses; i++)
//		{
//			if(busBusyUntil[i]< 
//					busBusyUntil[busID])
//			{
//				busID = i;
//			}
//		}
		
		if (busBusyUntil[busID] < GlobalClock.getCurrentTime())
		{
			busBusyUntil[busID] = GlobalClock.getCurrentTime() + (busOccupancy * sharedMem.getStepSize());
			for (int i = 0; i < eventList.size(); i++)
			{
				eventList.get(i).addEventTime(busOccupancy * sharedMem.getStepSize());
				eventList.get(i).getProcessingElement().getPort().put(eventList.get(i));
			}
		}
		else
		{
			busBusyUntil[busID] += busOccupancy * sharedMem.getStepSize();
			for (int i = 0; i < eventList.size(); i++)
			{
				eventList.get(i).addEventTime(busBusyUntil[busID] - GlobalClock.getCurrentTime() + (busOccupancy * sharedMem.getStepSize()));
				eventList.get(i).getProcessingElement().getPort().put(eventList.get(i));
			}
		}
	}
	public void getBusAndPutEvent(Event event)
	{
		int busID = 0;

		//Address multiplexing
		long address = ((AddressCarryingEvent)event).getAddress();
		busID = (int)(address & busMask);
//		System.out.println("Bus ID : "+busID);
		
		
//		for(int i=0; i<numBuses; i++)
//		{
//			if(busBusyUntil[i]< 
//					busBusyUntil[busID])
//			{
//				busID = i;
//			}
//		}
		
		if (busBusyUntil[busID] < GlobalClock.getCurrentTime())
		{
			busBusyUntil[busID] = GlobalClock.getCurrentTime() + (busOccupancy * sharedMem.getStepSize());
			event.addEventTime(busOccupancy * sharedMem.getStepSize());
			event.getProcessingElement().getPort().put(event);
		}
		else
		{
			busBusyUntil[busID] += busOccupancy * sharedMem.getStepSize();
			event.addEventTime(busBusyUntil[busID] - GlobalClock.getCurrentTime() + (busOccupancy * sharedMem.getStepSize()));
			event.getProcessingElement().getPort().put(event);
		}
	}
}