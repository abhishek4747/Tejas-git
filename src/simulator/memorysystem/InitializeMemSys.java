package memorysystem;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import generic.Core;

import config.CacheConfig;
import config.SystemConfig;

public class InitializeMemSys 
{
	static Hashtable<String, Cache> cacheList;
	
	public static Hashtable<String, Cache> getCacheList() {
		return cacheList;
	}

	public static void initializeMemSys(Core[] cores)
	{
		System.out.println("initializing memory system...");
		// initialising the memory system
		
		/*-- Initialise the memory system --*/
		CacheConfig cacheParameterObj;
		
		/*First initialise the L2 and greater caches (to be linked with L1 caches and among themselves)*/
		cacheList = new Hashtable<String, Cache>(); //Declare the hash table for level 2 or greater caches
		for (Enumeration<String> cacheNameSet = SystemConfig.declaredCaches.keys(); cacheNameSet.hasMoreElements(); )
		{
			String cacheName = cacheNameSet.nextElement();
			
			if (!(cacheList.containsKey(cacheName))) //If not already present
			{
				cacheParameterObj = SystemConfig.declaredCaches.get(cacheName);
				
				//Declare the new cache
				Cache newCache = new Cache(cacheParameterObj);
				
				//Put the newly formed cache into the new list of caches
				cacheList.put(cacheName, newCache);
			}
		}
		//Link all the initialised caches to their next levels
		for (Enumeration<String> cacheNameSet = cacheList.keys(); cacheNameSet.hasMoreElements(); /*Nothing*/)
		{
			String cacheName = cacheNameSet.nextElement();
			Cache cacheToSetNextLevel = cacheList.get(cacheName);
				
			if (cacheToSetNextLevel.isLastLevel == true) //If this is the last level, don't set anything
			{
				continue;
			}
			
			String nextLevelName = cacheToSetNextLevel.nextLevelName;
			
			if (nextLevelName.isEmpty())
			{
				System.err.println("Memory system configuration error : The cache \""+ cacheName +"\" is not last level but the next level is not specified");
				System.exit(1);
			}
			if (cacheName.equals(nextLevelName)) //If the cache is itself given as its next level
			{
				System.err.println("Memory system configuration error : The cache \""+ cacheName +"\" is specified as a next level of itself");
				System.exit(1);
			}
				
			if (cacheList.containsKey(nextLevelName)) 
			{
				//Point the cache to its next level
				cacheToSetNextLevel.nextLevel = cacheList.get(nextLevelName);
				cacheToSetNextLevel.nextLevel.prevLevel.add(cacheToSetNextLevel);
			}
			else
			{
				System.err.println("Memory system configuration error : A cache specified as a next level does not exist");
				System.exit(1);
			}
		}
		
		//Initialise the core memory systems
		//Global.memSys = new CoreMemorySystem[SystemConfig.NoOfCores];
		for (int i = 0; i < SystemConfig.NoOfCores; i++)
		{
			cores[i].getExecEngine().coreMemSys = new CoreMemorySystem(cores[i]);
//			Bus.upperLevels.add(cores[i].getExecEngine().coreMemSys.l1Cache);
			
			//Set the next levels of the L1 cache
			if (cores[i].getExecEngine().coreMemSys.l1Cache.isLastLevel == true) //If this is the last level, don't set anything
			{
				continue;
			}
			
			String nextLevelName = cores[i].getExecEngine().coreMemSys.l1Cache.nextLevelName;
			
			if (nextLevelName.isEmpty())
			{
				System.err.println("Memory system configuration error : The cache L["+ i +"] is not last level but the next level is not specified");
				System.exit(1);
			}
				
			if (cacheList.containsKey(nextLevelName)) 
			{
				//Point the cache to its next level
				cores[i].getExecEngine().coreMemSys.l1Cache.nextLevel = cacheList.get(nextLevelName);
				cores[i].getExecEngine().coreMemSys.l1Cache.nextLevel.prevLevel.add(cores[i].getExecEngine().coreMemSys.l1Cache);
			}
			else
			{
				System.err.println("Memory system configuration error : A cache specified as a next level does not exist");
				System.exit(1);
			}
		}
/*		
		//Initialising the BUS for cache coherence
		if (!cacheList.containsKey(SystemConfig.coherenceEnforcingCache))
		{
			System.err.println("Memory system configuration error : A cache specified as coherence enforcing cache does not exist");
			System.exit(1);
		}
		else
		{
			Bus.lowerLevel = cacheList.get(SystemConfig.coherenceEnforcingCache);
			Bus.upperLevels = Bus.lowerLevel.prevLevel;
		}
		Bus.lowerLevel.enforcesCoherence = true;
		
		propagateCoherencyUpwards(Bus.upperLevels);*/
	}
	
	/**
	 * Recursive method to mark all the caches above the bus as COHERENT
	 * @param list : Initial input is an Arraylist of Caches juat above the Bus and then works recursively upwards
	 */
	public static void propagateCoherencyUpwards(ArrayList<Cache> list)
	{
		if (list.isEmpty())
			return;
		for (int i = 0; i < list.size(); i++)
		{
			list.get(i).isCoherent = true;
			propagateCoherencyUpwards(list.get(i).prevLevel);
		}
	}
}
