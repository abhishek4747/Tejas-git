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

import config.SystemConfig;

import memorysystem.CacheLine.MESI;
import generic.*;

public class SearchLineCopiesEvent extends Event
{
	int requestingThreadID;
	Cache targetCache;
	long address;
	Cache sourceCache;
	
	public SearchLineCopiesEvent(int requestingThreadID,
								Cache _targetCache, 
								long _address, 
								Cache _sourceCache,
								long eventTime)
	{
		super(eventTime, 4);
		
		targetCache = _targetCache;
		address = _address;
		sourceCache = _sourceCache;
	}
	
	public void handleEvent()
	{
		Bus.snoopingCoresProcessed++;
		
		// If the request is RWITM (For write miss)
		if (Bus.requestType == Bus.BusReqType.RWITM)
		{
			if (Bus.blockRWITM == false)
			{
				CacheLine cl = targetCache.access(address);
				
				//Copy found
				if (cl != null)
				{
					Bus.copiesFound++;
					if (cl.getState() == MESI.MODIFIED) //MODIFIED
					{
						Bus.blockRWITM = true;
					
						//TODO Write modified version to next level
						CacheRequestPacket request = new CacheRequestPacket();
						request.setThreadID(requestingThreadID);
						request.setAddr(Bus.address);
						request.setType(MemoryAccessType.WRITE);
						Stack<CacheFillStackEntry> cacheFillStack = new Stack<CacheFillStackEntry>();
						cacheFillStack.add(new CacheFillStackEntry(targetCache, request));
					
						if (targetCache.isLastLevel)	
							MemEventQueue.eventQueue.add(new MainMemAccessEvent(requestingThreadID,  
																			cacheFillStack,
																			MESI.INVALID,
																			MemEventQueue.clock
																			+ SystemConfig.mainMemoryLatency));
					
						else
							MemEventQueue.eventQueue.add(new CacheAccessEvent(requestingThreadID,
																			targetCache.nextLevel,
																			request,
																			MESI.INVALID,
																			cacheFillStack,
																			MemEventQueue.clock
																			+ targetCache.nextLevel.getLatency()));
						cl.setState(MESI.INVALID);
					}
					else //EXCLUSIVE or SHARED
					{
						//Invalidate the copy
						cl.setState(MESI.INVALID);
					}
				}
			}

			//if all the snooping caches are processed
			if (Bus.snoopingCoresProcessed >= (Bus.upperLevels.size() - 1))
			{
				//TODO Read from main memory into the source cache
				AccessLowerFromBus.access(requestingThreadID, sourceCache, MESI.MODIFIED);
			}
		}
		
		//If the request is memory access (for Read miss)
		else if (Bus.requestType == Bus.BusReqType.MEM_ACCESS)
		{
			CacheLine cl = targetCache.access(address);
			Bus.snoopingCoresProcessed++;
			
			//Copy found
			if (cl != null)
			{
				Bus.copiesFound++;
				if (Bus.copiesFound == 1)
				{
					Bus.singleFoundCopy = cl;
					Bus.cacheContainingTheCopy = targetCache;
				}
			}
			
			//if all the snooping caches are processed
			if (Bus.snoopingCoresProcessed >= (Bus.upperLevels.size() - 1))
			{
				if (Bus.copiesFound == 0) //No copies found (may be invalid)
				{
					//TODO Store main mem value in the cache
					AccessLowerFromBus.access(requestingThreadID, sourceCache, MESI.EXCLUSIVE);
				}
				else if (Bus.copiesFound == 1)	// A single copy found (E/M)
				{
					if (Bus.singleFoundCopy.getState() == MESI.EXCLUSIVE)
					{
						//TODO singleFoundCopy is put to bus
						//TODO state set OF BOTH to S
						MemEventQueue.eventQueue.add(new BorrowSharedEvent(sourceCache,
																			address,
																			MemEventQueue.clock
																			+ SystemConfig.cacheBusLatency
																			+ sourceCache.getLatency()));
						Bus.singleFoundCopy.setState(MESI.SHARED);
					}
					else if (Bus.singleFoundCopy.getState() == MESI.MODIFIED)
					{
						//TODO singleFoundCopy is put to bus
						//TODO singleFoundCopy written back to main memory
						//TODO state OF BOTH set to S
						MemEventQueue.eventQueue.add(new BorrowSharedEvent(sourceCache,
																			address,
																			MemEventQueue.clock
																			+ SystemConfig.cacheBusLatency
																			+ sourceCache.getLatency()));
						
						CacheRequestPacket request = new CacheRequestPacket();
						request.setThreadID(requestingThreadID);
						request.setAddr(Bus.address);
						request.setType(MemoryAccessType.WRITE);
						if (targetCache.isLastLevel)	
							MemEventQueue.eventQueue.add(new MainMemAccessEvent(requestingThreadID, 
																			null,
																			new Stack<CacheFillStackEntry>(),
																			MemEventQueue.clock
																			+ SystemConfig.cacheBusLatency
																			+ SystemConfig.mainMemoryLatency));
					
						else
							MemEventQueue.eventQueue.add(new CacheAccessEvent(requestingThreadID,
																			null,
																			targetCache.nextLevel,
																			request,
																			new Stack<CacheFillStackEntry>(),
																			MemEventQueue.clock
																			+ SystemConfig.cacheBusLatency
																			+ targetCache.nextLevel.getLatency()));
						Bus.singleFoundCopy.setState(MESI.SHARED);
					}
				}
				else //Multiple copies found (Shared)
				{
					//TODO singleFoundCopy is put to bus
					//TODO state set to S
					MemEventQueue.eventQueue.add(new BorrowSharedEvent(sourceCache,
																		address,
																		MemEventQueue.clock
																		+ SystemConfig.cacheBusLatency
																		+ sourceCache.getLatency()));
				}
			}
		}
	}
}
