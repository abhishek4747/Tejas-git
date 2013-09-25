package pipeline.outoforder;

import generic.FunctionalUnitType;

/**
 * represents the functional units available
 * provides methods to request for an FU, and to find out when an FU will be available next
 */

public class FunctionalUnitSet {
	
	private int[] nUnits;
	private int[] latencies;
	
	//usage : if timeWhenFUAvailable <= current_time, then FU available for use
	//absolute time -- in terms of GlobalClock
	private long[][] timeWhenFUAvailable;
	
	
	public FunctionalUnitSet(int[] _nUnits, int[] _latencies)
	{
		nUnits = new int[FunctionalUnitType.no_of_types.ordinal()];
		latencies = new int[FunctionalUnitType.no_of_types.ordinal()];
		
		for(int i = 0; i < FunctionalUnitType.no_of_types.ordinal(); i++)
		{
			nUnits[i] = _nUnits[i];
			latencies[i] = _latencies[i];
		}
		
		timeWhenFUAvailable = new long[FunctionalUnitType.no_of_types.ordinal()][];
		for(FunctionalUnitType f : FunctionalUnitType.values())
		{
			if(f != FunctionalUnitType.no_of_types)
			{
				timeWhenFUAvailable[f.ordinal()] = new long[nUnits[f.ordinal()]];
			}
		}
	}
	
	//if an FU is available, it is assigned (timeTillFUAvailable is updated);
	//						negative of the FU instance is returned
	//else, the earliest time, at which an FU of the type becomes available, is returned
	
	public long requestFU(FunctionalUnitType FUType, long current_time, int stepSize)
	{
		long timeTillAvailable = timeWhenFUAvailable[FUType.ordinal()][0];
		
		for(int i = 0; i < nUnits[FUType.ordinal()]; i++)
		{
			if(timeWhenFUAvailable[FUType.ordinal()][i] <= current_time)
			{
				timeWhenFUAvailable[FUType.ordinal()][i] = current_time + latencies[FUType.ordinal()]*stepSize;
				return i * (-1);
			}
			if(timeWhenFUAvailable[FUType.ordinal()][i] < timeTillAvailable)
			{
				timeTillAvailable = timeWhenFUAvailable[FUType.ordinal()][i];
			}
		}
		
		return timeTillAvailable;
	}
	
	public int getFULatency(FunctionalUnitType FUType)
	{
		return latencies[FUType.ordinal()];
	}
	
	public int getNumberOfUnits(FunctionalUnitType FUType)
	{
		return nUnits[FUType.ordinal()];
	}
	
	public long getTimeWhenFUAvailable(FunctionalUnitType _FUType, int _FUInstance)
	{
		return timeWhenFUAvailable[_FUType.ordinal()][_FUInstance];
	}

}