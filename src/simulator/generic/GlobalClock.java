package generic;

import java.util.Enumeration;
import java.util.Hashtable;

import memorysystem.Cache;
import memorysystem.MemorySystem;
import config.SystemConfig;

public class GlobalClock {
	
	static long currentTime;
	static int stepSize;
	static double stepValue;

	public static void systemTimingSetUp(Core[] cores, Hashtable<String, Cache> cacheList)
	{
		currentTime = 0;
		stepSize = 1;
		
		//TODO setting up of a heterogeneous clock environment
		
		//populate time_periods[]
		int[] time_periods = new int[SystemConfig.NoOfCores + SystemConfig.declaredCaches.size() + 1];
		int i = 0;
		int seed = Integer.MAX_VALUE;
		String cacheName;
		Cache cache;
		
		for(i = 0; i < SystemConfig.NoOfCores; i++)
		{
			time_periods[i] = Math.round(100000/cores[i].getFrequency());
			if(time_periods[i] < seed)
			{
				seed = time_periods[i];
			}
		}
		for (Enumeration<String> cacheNameSet = cacheList.keys(); cacheNameSet.hasMoreElements(); /*Nothing*/)
		{
			cacheName = cacheNameSet.nextElement();
			cache = cacheList.get(cacheName);
			time_periods[i] = Math.round(100000/cache.getFrequency());
			if(time_periods[i] < seed)
			{
				seed = time_periods[i];
			}
			i++;
		}
		time_periods[i] = Math.round(100000/MemorySystem.mainMemory.getFrequency());
		if(time_periods[i] < seed)
		{
			seed = time_periods[i];
		}
				
		//compute HCF
		//TODO look for a better algorithm
		int j;
		boolean flag;
		int HCF = 1;
		for(i = 1; seed/i > 1; i++)
		{
			if(seed%i == 0)
			{
				flag = true;
				for(j = 0; j < SystemConfig.NoOfCores + SystemConfig.declaredCaches.size() + 1; j++)
				{
					if(time_periods[j]%(seed/i) != 0)
					{
						flag = false;
						break;
					}
				}
				if(flag == true)
				{
					HCF = (seed/i);
					break;
				}
			}
		}
		
		//set step sizes of components
		for(i = 0; i < SystemConfig.NoOfCores; i++)
		{
			cores[i].setStepSize(time_periods[i]/HCF);
			cores[i].getExecEngine().coreMemSys.getL1Cache().setStepSize(cores[i].getStepSize());
			cores[i].getExecEngine().coreMemSys.getLsqueue().setStepSize(cores[i].getStepSize());
			cores[i].getExecEngine().coreMemSys.getTLBuffer().setStepSize(cores[i].getStepSize());
			//System.out.println(cores[i].getStepSize());
		}
		for (Enumeration<String> cacheNameSet = cacheList.keys(); cacheNameSet.hasMoreElements(); /*Nothing*/)
		{
			cacheName = cacheNameSet.nextElement();
			cache = cacheList.get(cacheName);
			cache.setStepSize(time_periods[i++]/HCF);
			//System.out.println(cache.getStepSize());
		}
		MemorySystem.mainMemory.setStepSize(time_periods[i]/HCF);
		//System.out.println(MemorySystem.mainMemStepSize);
		
		stepValue = HCF/100000.0;
		
	}

	public static long getCurrentTime() {
		return GlobalClock.currentTime;
	}

	public static void setCurrentTime(long currentTime) {
		GlobalClock.currentTime = currentTime;
	}
	
	public static void incrementClock()
	{
		GlobalClock.currentTime += GlobalClock.stepSize;
	}

	public static int getStepSize() {
		return GlobalClock.stepSize;
	}

	public static void setStepSize(int stepSize) {
		GlobalClock.stepSize = stepSize;
	}
	
	public static double getStepValue() {
		return stepValue;
	}

}
