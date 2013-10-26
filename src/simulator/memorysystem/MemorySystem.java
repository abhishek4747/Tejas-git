/*****************************************************************************
				Tejas Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
------------------------------------------------------------------------------------------------------------

	Contributors:  Moksh Upadhyay, Mayur Harne
*****************************************************************************/
package memorysystem;

import java.util.Enumeration;
import java.util.Hashtable;

import pipeline.multi_issue_inorder.InorderCoreMemorySystem_MII;
import pipeline.outoforder.OutOrderCoreMemorySystem;

import net.optical.TopLevelTokenBus;

import main.ArchitecturalComponent;
import memorysystem.nuca.CBDNuca;
import memorysystem.nuca.NucaCache;
import memorysystem.nuca.NucaCacheLine;

import memorysystem.nuca.NucaCache.NucaType;

import memorysystem.Cache.CacheType;
import memorysystem.directory.CentralizedDirectoryCache;

import generic.*;
import config.CacheConfig;
import config.SimulationConfig;
import config.SystemConfig;


public class MemorySystem
{
	static Core[] cores;
	static Hashtable<String, Cache> cacheList;
	public static MainMemory mainMemory;
	public static CentralizedDirectoryCache centralizedDirectory;
	
	public static boolean bypassLSQ = false;
	
	public static Hashtable<String, Cache> getCacheList() {
		return cacheList;
	}

	public static void initializeMemSys(Core[] cores, TopLevelTokenBus tokenBus)
	{
		MemorySystem.cores = cores;
		
		System.out.println("initializing memory system...");
		// initialising the memory system
		
		//Set up the main memory properties
		
		
		if (SimulationConfig.isPipelineInorder)
			bypassLSQ = true;
		
		/*-- Initialise the memory system --*/
		CacheConfig cacheParameterObj;
		NucaType nucaType = NucaType.NONE;
		/*First initialise the L2 and greater caches (to be linked with L1 caches and among themselves)*/
		cacheList = new Hashtable<String, Cache>(); //Declare the hash table for level 2 or greater caches
		boolean flag = false;
		for (Enumeration<String> cacheNameSet = SystemConfig.declaredCaches.keys(); cacheNameSet.hasMoreElements(); )
		{
			String cacheName = cacheNameSet.nextElement();
			
			if (!(cacheList.containsKey(cacheName))) //If not already present
			{
				cacheParameterObj = SystemConfig.declaredCaches.get(cacheName);
				
				//Declare the new cache
				Cache newCache = null;
				if (cacheParameterObj.getNucaType() == NucaType.NONE)
					newCache = new Cache(cacheParameterObj, null);
				else if (cacheParameterObj.getNucaType() == NucaType.S_NUCA)
				{	
					nucaType = NucaType.S_NUCA;
					flag = true;
					newCache = new NucaCache(cacheParameterObj,null,tokenBus);
				}
				else if (cacheParameterObj.getNucaType() == NucaType.D_NUCA)
				{	
					nucaType = NucaType.D_NUCA;
					flag = true;
					newCache = new NucaCache(cacheParameterObj,null,tokenBus);
				}
				
				else if (cacheParameterObj.getNucaType() == NucaType.CB_D_NUCA)
				{	
					nucaType = NucaType.CB_D_NUCA;
					flag = true;
					newCache = new CBDNuca(cacheParameterObj,null,tokenBus);
				}
				//Put the newly formed cache into the new list of caches
				cacheList.put(cacheName, newCache);
				
				//add initial cachepull event
				if(newCache.levelFromTop == CacheType.Lower)
				{
					ArchitecturalComponent.getCores()[0].getEventQueue().addEvent(
											new CachePullEvent(
													ArchitecturalComponent.getCores()[0].getEventQueue(),
													0,
													newCache,
													newCache,
													RequestType.PerformPulls,
													-1));
				}
			}
		}
		mainMemory = new MainMemory(nucaType);
		//Initialize centralized directory
//		int numCacheLines=262144;//FIXME 256KB in size. Needs to be fixed.
		centralizedDirectory = new CentralizedDirectoryCache(SystemConfig.directoryConfig, null, cores.length, 
				SystemConfig.dirNetworkDelay);
		//Link all the initialised caches to their next levels

		
		//Initialise the core memory systems
		//Global.memSys = new CoreMemorySystem[SystemConfig.NoOfCores];
		for (int i = 0; i < SystemConfig.NoOfCores; i++)
		{
			CoreMemorySystem coreMemSys = null;
			
			if(cores[i].isPipelineInorder)
			{
				coreMemSys = new InorderCoreMemorySystem_MII(cores[i]);
			}
			else if (cores[i].isPipelineStatistical)
			{
				//cores[i].getStatisticalPipeline().coreMemSys = coreMemSys;
			}
			else
			{
				//TODO
				coreMemSys = new OutOrderCoreMemorySystem(cores[i]);
				//TODO set corememsys of cores[i] to the one jus created in outordercorememsys constructor
			}
			
			//			Bus.upperLevels.add(cores[i].getExecEngine().coreMemSys.l1Cache);
			
			//Set the next levels of the L1 data cache
			if (coreMemSys.l1Cache.isLastLevel == true) //If this is the last level, don't set anything
			{
				continue;
			}
			
			String nextLevelName = coreMemSys.l1Cache.nextLevelName;
			
			if (nextLevelName.isEmpty())
			{
				System.err.println("Memory system configuration error : The cache L["+ i +"] is not last level but the next level is not specified");
				System.exit(1);
			}
				
			if (cacheList.containsKey(nextLevelName)) 
			{
				//Point the cache to its next level
				coreMemSys.l1Cache.nextLevel = cacheList.get(nextLevelName);
				coreMemSys.l1Cache.nextLevel.prevLevel.add(coreMemSys.l1Cache);
				
				//if mode3mshr
				//coreMemSys.l1Cache.connectedMSHR.add(coreMemSys.getL1MSHR());
			}
			else
			{
				System.err.println("Memory system configuration error : A cache specified as a next level does not exist");
				System.exit(1);
			}
			
			//Set the next levels of the instruction cache
			if (coreMemSys.iCache.isLastLevel == true) //If this is the last level, don't set anything
			{
				continue;
			}
			
			nextLevelName = coreMemSys.iCache.nextLevelName;
			
			if (nextLevelName.isEmpty())
			{
				System.err.println("Memory system configuration error : The iCache is not last level but the next level is not specified");
				System.exit(1);
			}
				
			if (cacheList.containsKey(nextLevelName)) 
			{
				//Point the cache to its next level
				coreMemSys.iCache.nextLevel = cacheList.get(nextLevelName);
				coreMemSys.iCache.nextLevel.prevLevel.add(coreMemSys.iCache);
				
				//if mode3mshr
				//coreMemSys.iCache.connectedMSHR.add(coreMemSys.getiMSHR());
			}
			else
			{
				System.err.println("Memory system configuration error : A cache specified as a next level does not exist");
				System.exit(1);
			}
			
						
		}
		
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
		
		for (Enumeration<String> cacheNameSet = cacheList.keys(); cacheNameSet.hasMoreElements(); /*Nothing*/)
		{
			String cacheName = cacheNameSet.nextElement();
			Cache cacheToSetConnectedMSHR = cacheList.get(cacheName);
			cacheToSetConnectedMSHR.populateConnectedMSHR();
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
		/*for (Enumeration<String> cacheNameSet = cacheList.keys(); cacheNameSet.hasMoreElements(); )
		{
			String cacheName = cacheNameSet.nextElement();
			Cache cacheToSetNextLevel = cacheList.get(cacheName);
			
			System.out.println(cacheName + " : " + cacheToSetNextLevel.connectedMSHR.size());
		}*/
	}
	

	/**
	 * Recursive method to mark all the caches above the bus as COHERENT
	 * @param list : Initial input is an Arraylist of Caches juat above the Bus and then works recursively upwards
	 */
//	public static void propagateCoherencyUpwards(ArrayList<Cache> list)
//	{
//		if (list.isEmpty())
//			return;
//		for (int i = 0; i < list.size(); i++)
//		{
//			list.get(i).isCoherent = true;
//			propagateCoherencyUpwards(list.get(i).prevLevel);
//		}
//	}
	
	public static   CentralizedDirectoryCache getDirectoryCache()
	{
		return centralizedDirectory;
	}
	
	public static void printMemSysResults()
	{
		System.out.println("\n Memory System results\n");
		for (int i = 0; i < SystemConfig.NoOfCores; i++)
		{
			System.out.println(
					"LSQ[" + i + "] Loads : "
					+ cores[i].getExecEngine().getCoreMemorySystem().lsqueue.NoOfLd 
					+ "\t ; LSQ[" + i + "] Stores : " 
					+ cores[i].getExecEngine().getCoreMemorySystem().lsqueue.NoOfSt
					+ "\t ; LSQ[" + i + "] Value Forwards : " 
					+ cores[i].getExecEngine().getCoreMemorySystem().lsqueue.NoOfForwards);
		}
		
		System.out.println(" ");
		
		for (int i = 0; i < SystemConfig.NoOfCores; i++)
		{
			System.out.println(
					"TLB[" + i + "] Hits : " 
					+ cores[i].getExecEngine().getCoreMemorySystem().TLBuffer.tlbHits 
					+ "\t ; TLB[" + i + "] misses : " 
					+ cores[i].getExecEngine().getCoreMemorySystem().TLBuffer.tlbMisses);
		}
		
		System.out.println(" ");
		
		for (int i = 0; i < SystemConfig.NoOfCores; i++)
		{
			System.out.println(
					"L1[" + i + "] Hits : " 
					+ cores[i].getExecEngine().getCoreMemorySystem().l1Cache.hits 
					+ "\t ; L1[" + i + "] misses : " 
					+ cores[i].getExecEngine().getCoreMemorySystem().l1Cache.misses);
		}
		
		System.out.println(" ");
		System.out.println(" Results of other caches");
		
		for (Enumeration<String> cacheNameSet = cacheList.keys(); cacheNameSet.hasMoreElements(); /*Nothing*/)
		{
			String cacheName = cacheNameSet.nextElement();
			Cache cache = cacheList.get(cacheName);
			
			System.out.println(
					cacheName + " Hits : " 
					+ cache.hits 
					+ "\t ; " + cacheName + " misses : " 
					+ cache.misses);
		}
	}
}
