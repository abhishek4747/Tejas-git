package memorysystem.snoopyCoherence;

import java.util.ArrayList;
import memorysystem.Cache;

public class BusSet 
{
	private ArrayList<Cache> upperLevel;
	private Cache lowerCache;
	private Bus busSet[];
	
	public BusSet(ArrayList<Cache> upperLevel, Cache lowerCache, int numberOfBuses) 
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
}