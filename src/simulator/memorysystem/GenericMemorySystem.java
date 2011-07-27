/*****************************************************************************
				BhartiSim Simulator
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

	Contributors:  Moksh Upadhyay
*****************************************************************************/
package memorysystem;

import config.SystemConfig;
import config.CacheConfig;
import java.util.*;

public class GenericMemorySystem
{
	protected Cache[] L1;
	protected TLB[] TLBuffer;
	
	protected Hashtable<String, Cache> cacheList;
	
	public GenericMemorySystem()
	{
		/*-- Initialize the memory system --*/
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
		for (Enumeration<String> cacheNameSet = cacheList.keys(); cacheNameSet.hasMoreElements(); )
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
			}
			else
			{
				System.err.println("Memory system configuration error : A cache specified as a next level does not exist");
				System.exit(1);
			}
		}		
		
		/*Initialise the core elements*/
		L1 = new Cache[SystemConfig.NoOfCores];
		TLBuffer = new TLB[SystemConfig.NoOfCores];
		for (int i = 0; i < SystemConfig.NoOfCores; i++)
		{
			//Initialise the i'th L1 cache
			cacheParameterObj = SystemConfig.core[i].l1Cache;
			L1[i] = new Cache(cacheParameterObj); 
			
			//Set the next levels of the L1 cache
			if (L1[i].isLastLevel == true) //If this is the last level, don't set anything
			{
				continue;
			}
			
			String nextLevelName = L1[i].nextLevelName;
			
			if (nextLevelName.isEmpty())
			{
				System.err.println("Memory system configuration error : The cache L["+ i +"] is not last level but the next level is not specified");
				System.exit(1);
			}
				
			if (cacheList.containsKey(nextLevelName)) 
			{
				//Point the cache to its next level
				L1[i].nextLevel = cacheList.get(nextLevelName);
			}
			else
			{
				System.err.println("Memory system configuration error : A cache specified as a next level does not exist");
				System.exit(1);
			}
			
			//Initialise the i'th TLB
			TLBuffer[i] = new TLB(SystemConfig.core[i].TLBSize);
		}
	}
	
	public void read(int coreID, long addr)
	{
		TLBuffer[coreID].getPhyAddrPage(addr);
		CacheRequestPacket request = new CacheRequestPacket();
		request.setThreadID(coreID);
		request.setType(CacheRequestPacket.readWrite.CacheRequestPacket);
		request.setAddr(addr);
		L1[coreID].processRequest(request);
	}
	
	public void write(int coreID, long addr)
	{
		TLBuffer[coreID].getPhyAddrPage(addr);
		CacheRequestPacket request = new CacheRequestPacket();
		request.setThreadID(coreID);
		request.setType(CacheRequestPacket.readWrite.CacheRequestPacket);
		request.setAddr(addr);
		L1[coreID].processRequest(request);
	}
}
