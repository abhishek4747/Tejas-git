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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import pipeline.multi_issue_inorder.InorderCoreMemorySystem_MII;
import pipeline.outoforder.OutOrderCoreMemorySystem;

import net.optical.TopLevelTokenBus;

import main.ArchitecturalComponent;
import memorysystem.nuca.DNuca;
import memorysystem.nuca.NucaCache;
import memorysystem.nuca.SNuca;

import memorysystem.nuca.NucaCache.NucaType;

import memorysystem.Cache.CacheType;
import memorysystem.directory.CentralizedDirectoryCache;

import generic.*;
import config.CacheConfig;
import config.Interconnect;
import config.NocConfig;
import config.SimulationConfig;
import config.SystemConfig;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;

public class MemorySystem
{
	static Core[] cores;
	static Hashtable<String, Cache> cacheList = new Hashtable<String, Cache>();
	public static MainMemoryController mainMemoryController;
	public static CentralizedDirectoryCache centralizedDirectory;
	
	private static CoreMemorySystem coreMemSysArray[];
	public static CoreMemorySystem[] getCoreMemorySystems() {
		return coreMemSysArray;
	}
	
	public static Vector<Cache> getSharedCacheList() {
		return (new Vector<Cache>(cacheList.values()));
	}
	
	public static Hashtable<String, Cache> getCacheList() {
		return cacheList;
	}

	public static CoreMemorySystem[] initializeMemSys(Core[] cores, TopLevelTokenBus tokenBus)
	{
		MemorySystem.cores = cores;
		coreMemSysArray = new CoreMemorySystem[cores.length];
		
		System.out.println("initializing memory system...");
		// initialising the memory system
		
		//Set up the main memory properties
		
		
		/*-- Initialise the memory system --*/
		NucaType nucaType = NucaType.NONE;
				
		boolean flag = false;
		NucaCache nucaCache = null;
		for (CacheConfig cacheParameterObj : SystemConfig.declaredCacheConfigs)
		{
			String cacheName = cacheParameterObj.cacheName;
			
			if (!(cacheList.containsKey(cacheName))) //If not already present
			{
				//cacheParameterObj = SystemConfig.declaredCaches.get(cacheName);
				
				//Declare the new cache
				Cache newCache = null;
				if (cacheParameterObj.getNucaType() == NucaType.NONE) {
					// XXX : We are already creating such caches in the createSharedCaches function
					//newCache = new Cache(cacheParameterObj, null);
					continue;
				} else if (cacheParameterObj.getNucaType() == NucaType.S_NUCA)
				{	
					nucaType = NucaType.S_NUCA;
					flag = true;
					newCache = new SNuca(cacheParameterObj,null,tokenBus,nucaType);
					nucaCache = (NucaCache) newCache;
					Core.nucaCache = nucaCache;
				}
				else if (cacheParameterObj.getNucaType() == NucaType.D_NUCA)
				{	
					nucaType = NucaType.D_NUCA;
					flag = true;
					newCache = new DNuca(cacheParameterObj,null,tokenBus,nucaType);
					nucaCache = (NucaCache) newCache;
					Core.nucaCache = nucaCache;
				}
				
				
				//Put the newly formed cache into the new list of caches
				cacheList.put(cacheName, newCache);
				
				//add initial cachepull event
				//if(newCache.levelFromTop == CacheType.Lower)
				//{
				ArchitecturalComponent.getCores()[0].getEventQueue().addEvent(
					new CachePullEvent(ArchitecturalComponent.getCores()[0].getEventQueue(),
					0, newCache, newCache, RequestType.PerformPulls, -1));
				//}
			}
		}
		
		//Initialize centralized directory
//		int numCacheLines=262144;//FIXME 256KB in size. Needs to be fixed.
		if(SystemConfig.interconnect == Interconnect.Bus)
		{
			centralizedDirectory = new CentralizedDirectoryCache("Directory", 0, SystemConfig.directoryConfig, null, cores.length, 
				SystemConfig.dirNetworkDelay);
			mainMemoryController = new MainMemoryController(nucaType);
		}
		else if(SystemConfig.interconnect == Interconnect.Noc)
		{
			//mainMemoryController = new MainMemoryController(SystemConfig.memoryControllersLocations,nucaType);
			SystemConfig.nocConfig.nocElements.makeNocElements(tokenBus,nucaCache);
		}
		//Link all the initialised caches to their next levels

		createPrivateCaches();
		createSharedCaches();
		createLinksBetweenSharedCaches();
		createLinkFromPrivateCacheToSharedCache();
		
		for (Enumeration<String> cacheNameSet = cacheList.keys(); cacheNameSet.hasMoreElements(); /*Nothing*/)
		{
			String cacheName = cacheNameSet.nextElement();
			Cache cacheToSetConnectedMSHR = cacheList.get(cacheName);
			cacheToSetConnectedMSHR.populateConnectedMSHR();
		}
		
		return coreMemSysArray;
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
	

	private static void createSharedCaches() {
		// Creating a list of shared caches
		for (int i=0; i<SystemConfig.declaredCacheConfigs.size(); i++) {
			CacheConfig config = SystemConfig.declaredCacheConfigs.get(i);
			int numComponents = SystemConfig.declaredCacheConfigs.get(i).numComponents;
			
			for(int component=0; component<numComponents; component++) {
				String cacheName = null;
//				if(numComponents==1) {
//					cacheName = config.cacheName;
//				} else {
					cacheName = config.cacheName + "[" + component + "]";
//				}
				
				Cache c = new Cache(cacheName, component, config, null);
				cacheList.put(cacheName, c);
				
				ArchitecturalComponent.getCores()[0].getEventQueue().addEvent(
					new CachePullEvent(ArchitecturalComponent.getCores()[0].getEventQueue(),
						0, c, c, RequestType.PerformPulls, -1));
			}
		}
	}
	
	private static void createLinksBetweenSharedCaches() {
		ArrayList<Cache> sharedCacheList = new ArrayList<Cache>(cacheList.values());
		
		for(Cache c : sharedCacheList) {
			if(c.isLastLevel==false) {
				if(c.nextLevel!=null) {
					misc.Error.showErrorAndExit("Next level cache must not be set for this cache");
				}
				
				createLinkToNextLevelCache(c);
			}
		}
	}
	
	private static void createLinkToNextLevelCache(Cache c) {
		String cacheName = c.cacheName;
		int cacheId = c.id;
		String nextLevelName = c.cacheConfig.nextLevel;
		String nextLevelIdStrOrig = c.cacheConfig.nextLevelId;
		
		if(nextLevelIdStrOrig!=null && nextLevelIdStrOrig!="") {
			int nextLevelId = getNextLevelId(cacheName, cacheId, nextLevelIdStrOrig);
			nextLevelName += "[" + nextLevelId + "]";
		} else {
			nextLevelName += "[0]";
		}
		
		Cache nextLevelCache = cacheList.get(nextLevelName);
		if(nextLevelCache==null) {
			misc.Error.showErrorAndExit("Inside " + cacheName + ".\n" +
				"Could not find the next level cache. Name : " + nextLevelName);
		}
		
		c.createLinkToNextLevelCache(nextLevelCache);		
	}

	private static void createLinkFromPrivateCacheToSharedCache() {
			
		for(CoreMemorySystem  coreMemSys : coreMemSysArray) {
			ArrayList<Cache> coreCacheList = new ArrayList<Cache>(coreMemSys.getCacheList().values());
			for(Cache c : coreCacheList) {
				if(c.nextLevel==null && c.isLastLevel==false) {
					createLinkToNextLevelCache(c);
				}
			}
		}
	}
	
	private static int getNextLevelId(String cacheName, int cacheId,
			String nextLevelIdStrOrig) {
		
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");

		//Replace $i with the component id of cache
		String cacheIdStr = (new Integer(cacheId)).toString();
		String nextLevelIdStr = nextLevelIdStrOrig.replaceAll("\\$i", cacheIdStr);
		
		try {
			Double ret =  (Double)engine.eval(nextLevelIdStr);
			return ret.intValue();
		} catch (Exception e) {
			misc.Error.showErrorAndExit("Error in evaluating the formula " +
				"for the next level cache.\n" +
				"\nname : " + cacheName + "\tid : " + cacheId + 
				"\nnextLevelIdStrOrig : " + nextLevelIdStrOrig + 
				"\nnextLevelIdStrAfterTransformation : " + nextLevelIdStr + "\n" + e);
			return -1;
		}
	}

	private static CoreMemorySystem getCoreMemorySystem(int core) {
		return coreMemSysArray[core];
	}

	private static void createPrivateCaches() {
		for (int i = 0; i < SystemConfig.NoOfCores; i++) {
			
			if(cores[i].isPipelineInOrder()) {
				coreMemSysArray[i] = new InorderCoreMemorySystem_MII(cores[i]);
			} else if(cores[i].isPipelineOutOfOrder()) {
				coreMemSysArray[i] = new OutOrderCoreMemorySystem(cores[i]);
			} else {
				misc.Error.showErrorAndExit("pipeline type not defined !!");
			}
		}		
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
					"ITLB[" + i + "] Hits : " 
					+ cores[i].getExecEngine().getCoreMemorySystem().getiTLB().tlbHits 
					+ "\t ; ITLB[" + i + "] misses : " 
					+ cores[i].getExecEngine().getCoreMemorySystem().getiTLB().tlbMisses);
		}
		
		System.out.println(" ");
		
		for (int i = 0; i < SystemConfig.NoOfCores; i++)
		{
			System.out.println(
					"DTLB[" + i + "] Hits : " 
					+ cores[i].getExecEngine().getCoreMemorySystem().getdTLB().tlbHits 
					+ "\t ; DTLB[" + i + "] misses : " 
					+ cores[i].getExecEngine().getCoreMemorySystem().getdTLB().tlbMisses);
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
