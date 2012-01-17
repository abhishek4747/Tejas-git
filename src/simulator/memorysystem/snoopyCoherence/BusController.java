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
		busBusyUntil = new int[numberOfBuses];
//		for (int i = 0; i < numberOfBuses; i++)
//		{
//			busSet[i] = new Bus();
//		}
	}
	
//	private Bus getBus()
//	{
//		return busSet[0];
//	}
	
	public void processWriteHit(EventQueue eventQ, Cache requestingCache, CacheLine cl, long address)
	{
		if (cl.getState() == MESI.MODIFIED);
		else if (cl.getState() == MESI.EXCLUSIVE)
			cl.setState(MESI.MODIFIED);
		else if (cl.getState() == MESI.SHARED)
		{
//			getBus() and lock the bus for 1 cycle
			
			//Put the invalidate events for other cores
			for (int i = 0; i < upperLevel.size(); i++)
			{
				Cache destCache = upperLevel.get(i);
				if (destCache != requestingCache)
					destCache.getPort().put(
							new AddressCarryingEvent(
									eventQ, 
									destCache.getLatencyDelay() + 1/*For the bus*/,
									requestingCache,
									destCache,
									RequestType.MESI_Invalidate, 
									address));
			}
			
			//Don't wait for the replies and set the state to modified
			cl.setState(MESI.MODIFIED);
		}
		else
		{
			System.out.println("Forbidden condition reached");
		}
	}
	
	public void processReadMiss(CacheLine cl, long address)
	{
//		getBus() and lock the bus for 1 cycle
		
		for (int i = 0; i < upperLevel.size(); i++)
		{
			CacheLine cacheLine = upperLevel.get(i).processRequest(RequestType.Cache_Read, address);
			if (cacheLine != null)
				switch (cacheLine.getState())
				{
				case MODIFIED:
					return;
				case EXCLUSIVE:
					return;
				case SHARED:
					return;
				}
		}
		
		//Store shared memory copy in the cache
//		bus.
	}
	
	public void processWriteMiss(CacheLine cl)
	{
		
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
		}
		else
		{
			busBusyUntil[availableBusID] += sharedMem.getStepSize();
		}
	}
}