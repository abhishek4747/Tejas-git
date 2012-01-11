package memorysystem.snoopyCoherence;

import java.util.ArrayList;
import memorysystem.Cache;

public class Bus 
{
	private ArrayList<Cache> upperLevel;
	private Cache lowerCache;
	
	public Bus(ArrayList<Cache> upperLevel, Cache lowerCache) 
	{
		super();
		this.upperLevel = upperLevel;
		this.lowerCache = lowerCache;
	}
}
