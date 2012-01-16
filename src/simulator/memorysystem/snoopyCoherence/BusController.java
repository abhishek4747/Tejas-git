package memorysystem.snoopyCoherence;

import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

import java.util.ArrayList;

import memorysystem.AddressCarryingEvent;
import memorysystem.CacheLine;
import memorysystem.Cache;
import memorysystem.MESI;

public class BusController 
{
	protected ArrayList<Cache> upperLevel;
	protected Cache lowerCache;
	private Bus busSet[];
	
	public BusController(ArrayList<Cache> upperLevel, Cache lowerCache, int numberOfBuses) 
	{
		super();
		this.upperLevel = upperLevel;
		this.lowerCache = lowerCache;
		busSet = new Bus[numberOfBuses];
		for (int i = 0; i < numberOfBuses; i++)
		{
			busSet[i] = new Bus();
		}
	}
	
	private Bus getBus()
	{
		return busSet[0];
	}
	
	public void processWriteHit(Cache requestingCache, CacheLine cl, long address)
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
									EventQueue eventQ, 
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
	
	public void processReadMiss(CacheLine cl)
	{
		
	}
	
	public void processWriteMiss(CacheLine cl)
	{
		
	}
}