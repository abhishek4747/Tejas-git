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

import java.util.Stack;

import config.CacheConfig;
import config.SystemConfig;
import generic.*;
import memorysystem.CacheLine.MESI;

public class FillCacheStackEvent extends Event
{
	int threadID;
	LSQEntry lsqEntry;
	Stack<CacheFillStackEntry> cacheFillStack;
	MESI stateToSet;
	boolean belowCoherentLevel = false;
	
	//Above the Bus
	public FillCacheStackEvent(int _threadID,
								LSQEntry _lsqEntry, //The LSQ entry that initiated the cache access
								Stack<CacheFillStackEntry> _cacheFillStack,
								MESI _stateToSet,
								long eventTime)
	{
		super(eventTime, 2, 0);
		
		threadID = _threadID;
		lsqEntry = _lsqEntry;
		cacheFillStack = _cacheFillStack;
	}
	
	//Starting from Bus downwards
	public FillCacheStackEvent(int _threadID,
								Stack<CacheFillStackEntry> _cacheFillStack,
								MESI _stateToSet,
								long eventTime)
	{
		super(eventTime, 2, 0);

		belowCoherentLevel = true;
		threadID = _threadID;
		stateToSet = _stateToSet;
		cacheFillStack = _cacheFillStack;
	}
	
	public void handleEvent()
	{
		CacheLine evictedLine = null;
		CacheFillStackEntry stackEntry = cacheFillStack.pop();
		
		if (belowCoherentLevel)
		{
			if (cacheFillStack.isEmpty())
				evictedLine = stackEntry.cache.fill(stackEntry.request, stateToSet);
			else
				evictedLine = stackEntry.cache.fill(stackEntry.request, MESI.EXCLUSIVE); 
				//Just to show that its VALID (for levels below bus)

			//Write the evicted line(if any) to lower levels if dirty
			if ((evictedLine != null) && (stackEntry.cache.writeMode == CacheConfig.writeModes.WRITE_BACK) && (evictedLine.isModified()))
			{					
				if(stackEntry.cache.isLastLevel)
				{
					//TODO Code to write to main memory
					MemEventQueue.eventQueue/*.get(threadID)*/.add(new MainMemAccessEvent(threadID,
																					null, 
																					new Stack<CacheFillStackEntry>(), 
																					MemEventQueue.clock
																					+ SystemConfig.mainMemoryLatency));
				}
				else
				{
					//Write to the next level
					CacheRequestPacket evictedRequest = new CacheRequestPacket();
					evictedRequest.setThreadID(stackEntry.request.getThreadID());
					
					//This is based on the assumption that the lower level will always have higher block size
					evictedRequest.setAddr(evictedLine.getTag() << stackEntry.cache.blockSizeBits);
					
					evictedRequest.setType(MemoryAccessType.WRITE);
					
					//this.nextLevel.processEntry(evictedRequest);
					MemEventQueue.eventQueue/*.get(threadID)*/.add(new CacheAccessEvent(threadID,
																					null,
																					stackEntry.cache.nextLevel, 
																					evictedRequest, 
																					new Stack<CacheFillStackEntry>(), 
																					MemEventQueue.clock
																					+ stackEntry.cache.nextLevel.getLatency()));
				}
			}
			
			//Call next iteration of the FillCacheStackEvent
			if (!cacheFillStack.isEmpty())
			{
				MemEventQueue.eventQueue/*.get(threadID)*/.add(new FillCacheStackEvent(threadID,
																				cacheFillStack,
																				stateToSet,
																				MemEventQueue.clock
																				+ cacheFillStack.peek().cache.getLatency()));
			}
			else
			{
				Bus.endRequest();
			}
		}
		
		else //if this is the stack of caches above the bus
		{
			evictedLine = stackEntry.cache.fill(stackEntry.request, stateToSet);

			//Write the evicted line(if any) to lower levels if dirty
			if ((evictedLine != null) && (stackEntry.cache.writeMode == CacheConfig.writeModes.WRITE_BACK) && (evictedLine.isModified()))
			{					
				if(stackEntry.cache.isLastLevel)
				{
					//TODO Code to write to main memory
					MemEventQueue.eventQueue/*.get(threadID)*/.add(new MainMemAccessEvent(threadID,
																					null, 
																					new Stack<CacheFillStackEntry>(), 
																					MemEventQueue.clock
																					+ SystemConfig.mainMemoryLatency));
				}
				else
				{
					//Write to the next level
					CacheRequestPacket evictedRequest = new CacheRequestPacket();
					evictedRequest.setThreadID(stackEntry.request.getThreadID());
					
					//This is based on the assumption that the lower level will always have higher block size
					evictedRequest.setAddr(evictedLine.getTag() << stackEntry.cache.blockSizeBits);
					
					evictedRequest.setType(MemoryAccessType.WRITE);
					
					//this.nextLevel.processEntry(evictedRequest);
					MemEventQueue.eventQueue/*.get(threadID)*/.add(new CacheAccessEvent(threadID,
																					null,
																					stackEntry.cache.nextLevel, 
																					evictedRequest, 
																					new Stack<CacheFillStackEntry>(), 
																					MemEventQueue.clock
																					+ stackEntry.cache.nextLevel.getLatency()));
				}
			}
			
			//Call next iteration of the FillCacheStackEvent
			if (!cacheFillStack.isEmpty())
			{
				MemEventQueue.eventQueue/*.get(threadID)*/.add(new FillCacheStackEvent(threadID,
																				lsqEntry,
																				cacheFillStack,
																				stateToSet,
																				MemEventQueue.clock
																				+ cacheFillStack.peek().cache.getLatency()));
			}
			//If the stack is empty, modify the LSQ value if needed.
			else  if (lsqEntry != null)
			{
				MemEventQueue.eventQueue/*.get(threadID)*/.add(new FinallyTellLSQEvent(threadID,
																		lsqEntry,
																		MemEventQueue.clock
																		+ Global.memSys[threadID].lsqueue.getLatency()));
			}
		}
			
	}
}
