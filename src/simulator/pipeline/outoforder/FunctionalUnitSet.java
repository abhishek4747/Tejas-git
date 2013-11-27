package pipeline.outoforder;

import java.io.FileWriter;
import java.io.IOException;

import config.PowerConfigNew;
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
	
	//usage : if timeWhenFUAvailable <= current_time, then FU available for use
	//absolute time -- in terms of GlobalClock
	private long[][] timeWhenFUAvailable;
	
	long numIntALUAccesses;
	long numFloatALUAccesses;
	long numComplexALUAccesses;
	
	
	public FunctionalUnitSet(Core core, int[] _nUnits, int[] _latencies)
	{
		this.core = core;
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
				
				if(FUType == FunctionalUnitType.integerALU)
				{
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
		numIntALUAccesses += incrementBy * core.getStepSize();
	}
	
	void incrementFloatALUAccesses(int incrementBy)
	{
		numFloatALUAccesses += incrementBy * core.getStepSize();
	}
	
	void incrementComplexALUAccesses(int incrementBy)
	{
		numComplexALUAccesses += incrementBy * core.getStepSize();
	}
	
	public PowerConfigNew calculateAndPrintPower(FileWriter outputFileWriter, String componentName) throws IOException
	{
		double intALUleakagePower = core.getIntALUPower().leakagePower;
		double intALUdynamicPower = core.getIntALUPower().dynamicPower;
		double floatALUleakagePower = core.getFloatALUPower().leakagePower;
		double floatALUdynamicPower = core.getFloatALUPower().dynamicPower;
		double complexALUleakagePower = core.getComplexALUPower().leakagePower;
		double complexALUdynamicPower = core.getComplexALUPower().dynamicPower;
		
		double intALUactivityFactor = (double)numIntALUAccesses
										/(double)core.getCoreCyclesTaken();
		double floatALUactivityFactor = (double)numFloatALUAccesses
										/(double)core.getCoreCyclesTaken();
		double complexALUactivityFactor = (double)numComplexALUAccesses
										/(double)core.getCoreCyclesTaken();
		
		PowerConfigNew intALUpower = new PowerConfigNew(intALUleakagePower,
															intALUdynamicPower * intALUactivityFactor);
		PowerConfigNew floatALUpower = new PowerConfigNew(floatALUleakagePower,
															floatALUdynamicPower * floatALUactivityFactor);
		PowerConfigNew complexALUpower = new PowerConfigNew(complexALUleakagePower,
															complexALUdynamicPower * complexALUactivityFactor);
		
		PowerConfigNew power = new PowerConfigNew(0,0);
		power.add(intALUpower);
		power.add(floatALUpower);
		power.add(complexALUpower);
		
		intALUpower.printPowerStats(outputFileWriter, componentName + ".intALU");
		floatALUpower.printPowerStats(outputFileWriter, componentName + ".floatALU");
		complexALUpower.printPowerStats(outputFileWriter, componentName + ".complexALU");
		
		return power;
	}

}