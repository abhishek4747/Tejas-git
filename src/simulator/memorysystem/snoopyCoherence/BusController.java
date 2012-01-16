package memorysystem.snoopyCoherence;

import java.util.ArrayList;
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
	
	public void processWriteHit(Cache requestingCache, CacheLine cl)
	{
		if (cl.getState() == MESI.MODIFIED);
		else if (cl.getState() == MESI.EXCLUSIVE)
			cl.setState(MESI.MODIFIED);
		else if (cl.getState() == MESI.SHARED)
		{
			getBus().
		}
		else
		{
			System.out.println("Forbidden code reached");
		}
	}
	
	public void processReadMiss(CacheLine cl)
	{
		
	}
	
	public void processWriteMiss(CacheLine cl)
	{
		
	}
}