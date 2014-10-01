package pipeline.outoforder;

import java.io.FileWriter;
import java.io.IOException;

import config.EnergyConfig;
import generic.Core;
import generic.FunctionalUnitType;

/**
 * represents the functional units available
 * provides methods to request for an FU, and to find out when an FU will be available next
 */

public class FunctionalUnitSet {
	
	Core core;
	private int[] nUnits;
	private int[] latencies;
	private int[] reciprocalsOfThroughputs;
	
	//usage : if timeWhenFUAvailable <= current_time, then FU available for use
	//absolute time -- in terms of GlobalClock
	private long[][] timeWhenFUAvailable;
	
	long numIntALUAccesses;
	long numFloatALUAccesses;
	long numComplexALUAccesses;
	
	
	public FunctionalUnitSet(Core core, int[] _nUnits, int[] _latencies, int[] _reciprocalsOfThroughputs)
	{
		this.core = core;
		nUnits = new int[FunctionalUnitType.no_of_types.ordinal()];
		latencies = new int[FunctionalUnitType.no_of_types.ordinal()];
		reciprocalsOfThroughputs = new int[FunctionalUnitType.no_of_types.ordinal()];
		
		for(int i = 0; i < FunctionalUnitType.no_of_types.ordinal(); i++)
		{
			nUnits[i] = _nUnits[i];
			latencies[i] = _latencies[i];
			reciprocalsOfThroughputs[i] = _reciprocalsOfThroughputs[i];
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
				timeWhenFUAvailable[FUType.ordinal()][i] = current_time + reciprocalsOfThroughputs[FUType.ordinal()]*stepSize;
				
				if(FUType == FunctionalUnitType.integerALU)
				{
					//TODO this is overcounting in case of pipelined FUs
					incrementIntALUAccesses(latencies[FUType.ordinal()]);
				}
				else if(FUType == FunctionalUnitType.floatALU)
				{
					incrementFloatALUAccesses(latencies[FUType.ordinal()]);
				}
				else
				{
					incrementComplexALUAccesses(latencies[FUType.ordinal()]);
				}
				
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
	
	void incrementIntALUAccesses(int incrementBy)
	{
		numIntALUAccesses += incrementBy;
	}
	
	void incrementFloatALUAccesses(int incrementBy)
	{
		numFloatALUAccesses += incrementBy;
	}
	
	void incrementComplexALUAccesses(int incrementBy)
	{
		numComplexALUAccesses += incrementBy;
	}
	
	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException
	{
		EnergyConfig totalPower = new EnergyConfig(0, 0);
		EnergyConfig intALUPower = new EnergyConfig(core.getIntALUPower(), numIntALUAccesses);
		totalPower.add(totalPower, intALUPower);
		EnergyConfig floatALUPower = new EnergyConfig(core.getFloatALUPower(), numFloatALUAccesses);
		totalPower.add(totalPower, floatALUPower);
		EnergyConfig complexALUPower = new EnergyConfig(core.getComplexALUPower(), numComplexALUAccesses);
		totalPower.add(totalPower, complexALUPower);
		
		intALUPower.printEnergyStats(outputFileWriter, componentName + ".intALU");
		floatALUPower.printEnergyStats(outputFileWriter, componentName + ".floatALU");
		complexALUPower.printEnergyStats(outputFileWriter, componentName + ".complexALU");
		
		return totalPower;
	}

}