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

import static org.junit.Assert.*;

import java.util.*;

import generic.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

import org.junit.Test;

import config.CacheConfig;
import config.SystemConfig;
import config.XMLParser;

public class PassiveMemSysTest 
{	
	private BufferedReader currentFile;
	boolean fileComplete = false;
	protected static Hashtable<String, Cache> cacheList;
	//public int NoOfLd = 0;
	//public LSQ lsq1;
	
	static void initializeRun(String args)
	{
		//Trace file
		try
		{
			Global.mainTraceFile = new BufferedReader(new FileReader(args));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		if(Global.mainTraceFile == null)
		{
			System.err.println("Error opening the specified file");
			System.exit(0);
		}

		//Read the XML file
		XMLParser.parse();
		
		new MemEventQueue();
		Global.commitErrors = 0;
		
		initialiseMemSys();
		
	}
	
	public static void initialiseMemSys()
	{
		Hashtable<String, Cache> cacheList;
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
		Global.memSys = new CoreMemorySystem[SystemConfig.NoOfCores];
		for (int i = 0; i < SystemConfig.NoOfCores; i++)
		{
			Global.memSys[i] = new CoreMemorySystem(i);
			Bus.upperLevels.add(Global.memSys[i].l1Cache);
			
			//Set the next levels of the L1 cache
			if (Global.memSys[i].l1Cache.isLastLevel == true) //If this is the last level, don't set anything
			{
				continue;
			}
			
			String nextLevelName = Global.memSys[i].l1Cache.nextLevelName;
			
			if (nextLevelName.isEmpty())
			{
				System.err.println("Memory system configuration error : The cache L["+ i +"] is not last level but the next level is not specified");
				System.exit(1);
			}
				
			if (cacheList.containsKey(nextLevelName)) 
			{
				//Point the cache to its next level
				Global.memSys[i].l1Cache.nextLevel = cacheList.get(nextLevelName);
				Global.memSys[i].l1Cache.nextLevel.prevLevel.add(Global.memSys[i].l1Cache);
			}
			else
			{
				System.err.println("Memory system configuration error : A cache specified as a next level does not exist");
				System.exit(1);
			}
		}
		
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
		
		//propagateCoherencyUpwards(Bus.upperLevels);
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

	@Test
	public void testCoreSpecificMemSystem()
	{
		initializeRun("jitter_pintrace.out");

		long starttime, stoptime;
		double elapsed;
		starttime = System.currentTimeMillis();
		
		start();
		
		stoptime = System.currentTimeMillis();
		elapsed = ((stoptime - starttime) / 1000);

		// print statistics 
		//System.out.println("L2 Hits " + MemorySystem.L2.GenericMemorySystem + ": L2 misses " + MemorySystem.L2.GenericMemorySystem);
		for (int i=0; i < SystemConfig.NoOfCores; i++) 
		{
			System.out.println("TLB[" + i + "] Hits " + Global.memSys[i].TLBuffer.tlbHits + "\t : TLB[" + i + "] misses " + Global.memSys[i].TLBuffer.tlbMisses);
			//System.out.println("L1[" + i + "] Hits " + Global.memSys[i].l1Cache.hits + " : L1[" + i + "] misses " + Global.memSys[i].l1Cache.misses);
		}
		System.out.println(" ");
		for (int i=0; i < SystemConfig.NoOfCores; i++) 
		{
			System.out.println("LSQ[" + i + "] Loads " + Global.memSys[i].lsqueue.NoOfLd + "\t : LSQ[" + i + "] forwards " + Global.memSys[i].lsqueue.NoOfForwards);
			//System.out.println("L1[" + i + "] Hits " + Global.memSys[i].l1Cache.hits + " : L1[" + i + "] misses " + Global.memSys[i].l1Cache.misses);
		}
		System.out.println(" ");
		for (int i=0; i < SystemConfig.NoOfCores; i++) 
		{
			//System.out.println("TLB[" + i + "] Hits " + Global.memSys[i].TLBuffer.tlbHits + " : TLB[" + i + "] misses " + Global.memSys[i].TLBuffer.tlbMisses);
			System.out.println("L1[" + i + "] Hits " + Global.memSys[i].l1Cache.hits + "\t : L1[" + i + "] misses " + Global.memSys[i].l1Cache.misses);
		}
		System.out.println(" ");
	//	for (int i=0; i < SystemConfig.NoOfCores; i++) 
	//	{
			//System.out.println("TLB[" + i + "] Hits " + Global.memSys[i].TLBuffer.tlbHits + " : TLB[" + i + "] misses " + Global.memSys[i].TLBuffer.tlbMisses);
			System.out.println("L2[" + 0 + "] Hits " + cacheList.get("L2").hits + "\t : L2[" + 0 + "] misses " + cacheList.get("L2").misses);
	//	}
		System.out.println(" ");
		//for (int i=0; i < SystemConfig.NoOfCores; i++) 
		//{
			//System.out.println("TLB[" + i + "] Hits " + Global.memSys[i].TLBuffer.tlbHits + " : TLB[" + i + "] misses " + Global.memSys[i].TLBuffer.tlbMisses);
		//	System.out.println("L3[" + i + "] Hits " + Global.memSys[i].l3Cache.hits + " : L3[" + i + "] misses " + Global.memSys[i].l3Cache.misses);
		//}
		//System.out.println(" ");
		//System.out.println("ROB load commit errors : " + Global.commitErrors);
		System.out.println(" ");
		System.out.println("Finished Successfully");
		System.out.println(" ");
		System.out.println("Running Time : " + elapsed);
		
		//fail("Not yet implemented");
	}
	
	/* methods */
	private CacheRequestPacket processLine(String sourceLine)
	{
		if (sourceLine.equals("#eof"))
		{
			return null;
		}
			
		// create an Entry
		CacheRequestPacket request = new CacheRequestPacket();

		// read the thread id
		String line;
		line = sourceLine;
		StrTok str = new StrTok(line, " ");
		String firstToken = str.next(" ");
		
		request.setThreadID(Integer.parseInt(firstToken));
		//System.out.print(entry.tid + " ");////////////////////////////////////

		// read the remaining ones
		int cnt = 0;
		while(true) 
		{
			String token = str.next(" \n");
			if(token == null)
				break;
			switch(cnt) 
			{
				case 0:
					request.setTime(Double.parseDouble(token));
					break;
				case 1:
					request.setType(getType(token));
					break;
				case 2:
					request.setAddr(getAddr(token));
					break;
			}
			cnt ++;
		}
		return request;
	}
	
	private long getAddr(String token)
	{
		token = token.substring(2, token.length());
		long value = Long.parseLong(token, 16);
		return value;
	}
	
	private MemoryAccessType getType(String token)
	{
		if(token.indexOf("R") != -1)
			return MemoryAccessType.READ;
		if(token.indexOf("W") != -1)
			return MemoryAccessType.WRITE;

		System.err.println("Undefined request type " + token);
		System.exit(1);
		return null;
	}
	
	private void run()
	{
		//define the line
		String line;
		//int index;
		Random generator = new Random();
		int randomIndex;
		
		//lsq1 = new LSQ(64);

		// keep reading and processing lines
		try
		{
			while(true) 
			{
				if (!fileComplete)
				{
					line = currentFile.readLine();
					CacheRequestPacket request = processLine(line);
					if (request == null)
						fileComplete = true;
					else
					{
					if (request.getType() == MemoryAccessType.READ)
						Global.memSys[request.getThreadID()].read(request.getAddr());
					else
						Global.memSys[request.getThreadID()].write(request.getAddr());
					}
				}
				else
				{
					int emptyCount = 0; 
					for (int i = 0; i < SystemConfig.NoOfCores; i++)
					{
						if (MemEventQueue.eventQueue/*.get(i)*/.isEmpty() && (Global.memSys[i].lsqueue.curSize == 0))
							emptyCount++;
						else
							break;
					}
					if (emptyCount == SystemConfig.NoOfCores)
						return;
				}
				
				//Increment the clock and possibly do some commits
				MemEventQueue.clock++;
				for (int i = 0; i < SystemConfig.NoOfCores; i++)
				{
					//MemEventQueue.clock[i]++;
					
					randomIndex = generator.nextInt(4);
					if (Global.memSys[i].lsqueue.curSize == Global.memSys[i].lsqueue.lsqSize)
					for (int j = 0; j < randomIndex; j++)
					{
						//if (!((Global.memSys[i].lsqueue.lsqueue[Global.memSys[i].lsqueue.head].getType() == LSQEntryType.LOAD) && (Global.memSys[i].lsqueue.lsqueue[Global.memSys[i].lsqueue.head].isForwarded() == false)))
							Global.memSys[i].lsqueue.processROBCommitForPerfectPipeline(Global.memSys[i].lsqueue.head);
					}
					else if (MemEventQueue.eventQueue/*.get(i)*/.isEmpty()  && (Global.memSys[i].lsqueue.curSize > 0))
						for (int j = 0; j < randomIndex; j++)
						{
							//if (!((Global.memSys[i].lsqueue.lsqueue[Global.memSys[i].lsqueue.head].getType() == LSQEntryType.LOAD) && (Global.memSys[i].lsqueue.lsqueue[Global.memSys[i].lsqueue.head].isForwarded() == false)))
								Global.memSys[i].lsqueue.processROBCommitForPerfectPipeline(Global.memSys[i].lsqueue.head);
								if (Global.memSys[i].lsqueue.curSize == 0)
									break;
						}
				}
				
				//Process the events
				//for (int i = 0; i <SystemConfig.NoOfCores; i++)
				//{
					if(MemEventQueue.eventQueue.isEmpty() == false)
					{
						LinkedList<Event> priority2_events;
						long eventTime;
						
						while(true)
						{
							if(MemEventQueue.eventQueue.isEmpty() == false)
							{
								eventTime  = MemEventQueue.eventQueue.peek().getEventTime();
							}
							else
							{
								break;
							}
						
							if(eventTime <= MemEventQueue.clock/*[i]*/)
							{
								priority2_events = new LinkedList<Event>();
							
								while(MemEventQueue.eventQueue.isEmpty() == false && MemEventQueue.eventQueue/*.get(i)*/.peek().getEventTime() == eventTime)
								{
									Event polledEvent = MemEventQueue.eventQueue.poll();
								
									if(polledEvent.getPriority() == 2)
									{
										priority2_events.add(polledEvent);
									}
									else
									{
										polledEvent.handleEvent();
									}
								}
								while(priority2_events.size() > 0)
								{
									priority2_events.remove().handleEvent(MemEventQueue.eventQueue);
								}
							}
							else
							{
								break;
							}
						}
					}
				//}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void start()
	{
		currentFile = Global.mainTraceFile;
		run();
	}
}
