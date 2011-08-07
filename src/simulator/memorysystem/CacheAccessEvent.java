/*****************************************************************************
				BhartiSim Simulator
------------------------------------------------------------------------------------------------------------

   Copyright 2010 Indian Institute of Technology, Delhi
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

import generic.*;
import java.util.Stack;

import memorysystem.Bus.BusReqType;
import memorysystem.CacheLine.MESI;

import config.CacheConfig;
import config.SystemConfig;

public class CacheAccessEvent extends Event 
{
	int threadID;
	LSQEntry lsqEntry;
	Cache targetCache;
	CacheRequestPacket request;
	boolean isAccessFromLSQ = false;
	Stack<CacheFillStackEntry> cacheFillStack;
	MESI stateToSet;
	boolean isBelowBus = false;
	
	public CacheAccessEvent(int _threadID,
							LSQEntry _lsqEntry, //The LSQ entry 0 that initiated the cache access
							Cache _targetCache, //The cache to be accessed
							CacheRequestPacket _request, //The access request packet
							Stack<CacheFillStackEntry> _cacheFillStack, //The stack of all the caches in the way to here (so that their corresponding missed blocks can be filled)
							long eventTime)
	{
		super(eventTime, 2, 0);
		
		threadID = _threadID;
		lsqEntry = _lsqEntry;
		targetCache = _targetCache;
		request = _request;
		cacheFillStack = _cacheFillStack;
	}
	
	//Fetch and set state(Access from a Source cache from cache coherence)
	public CacheAccessEvent(int _threadID,
							Cache _targetCache, //The cache to be accessed
							CacheRequestPacket _request, //The access request packet
							MESI _stateToSet,
							Stack<CacheFillStackEntry> _cacheFillStack, //The stack of all the caches in the way to here (so that their corresponding missed blocks can be filled)
							long eventTime)
	{
		super(eventTime, 2, 0);

		isBelowBus = true;
		threadID = _threadID;
		targetCache = _targetCache;
		request = _request;
		cacheFillStack = _cacheFillStack;
		stateToSet = _stateToSet;
	}
	
	@Override
	public void handleEvent()
	{
		CacheLine cl = targetCache.processRequest(request);
		
		if (!isBelowBus)
		{
			if (cl != null) //Process the access and if it is a hit, proceed
			{
				//Handling Cache Coherence
				if ((request.getType() == MemoryAccessType.WRITE) && (targetCache.isCoherent) && (!targetCache.nextLevel.enforcesCoherence))
				{
					switch (cl.getState())
					{
					case MODIFIED:
						break; //No need to do anything
					case EXCLUSIVE:
						cl.setState(MESI.MODIFIED);
						break;
					case SHARED:
						MemEventQueue.eventQueue./*get(1 TODO threadID).*/add(new BusRequestEvent(threadID,
																							BusReqType.INVALIDATE, 
																							request.getAddr(),
																							targetCache,
																							cl,
																							cacheFillStack,
																							lsqEntry,
																							MemEventQueue.clock + 
																							SystemConfig.cacheBusLatency));
						return;
					}
				} //READ needs nothing to be done
			
				if (!cacheFillStack.isEmpty())
				{
					MemEventQueue.eventQueue/*.get(threadID)*/.add(new FillCacheStackEvent(threadID,
																					lsqEntry,
																					cacheFillStack,
																					cl.getState(),
																					MemEventQueue.clock
																					+ cacheFillStack.peek().cache.getLatency()));
				}
				//If the stack is empty, modify the LSQ value if needed.
				else if (lsqEntry != null)
				{
					MemEventQueue.eventQueue/*.get(threadID)*/.add(new FinallyTellLSQEvent(threadID,
																				lsqEntry,
																				MemEventQueue.clock
																				+ Global.memSys[threadID].lsqueue.getLatency()));
				}
			}
			else //The cache access is a miss
			{
				//Handle cache coherence
				if ((targetCache.isCoherent) && (!targetCache.nextLevel.enforcesCoherence))
				{
					if (request.getType() == MemoryAccessType.WRITE)
						MemEventQueue.eventQueue./*get(1 TODO threadID).*/add(new BusRequestEvent(threadID,
																						BusReqType.RWITM, 
																						request.getAddr(),
																						targetCache,
																						cl,
																						cacheFillStack,
																						lsqEntry,
																						MemEventQueue.clock 
																						+ SystemConfig.cacheBusLatency));
					else
						MemEventQueue.eventQueue./*get(1 TODO threadID).*/add(new BusRequestEvent(threadID,
																								BusReqType.MEM_ACCESS, 
																								request.getAddr(),
																								targetCache,
																								cl,
																								cacheFillStack,
																								lsqEntry,
																								MemEventQueue.clock 
																								+ SystemConfig.cacheBusLatency));
			
					return;
				}
			
				//Add this cache to the stack of caches to be filled when a hit is found
				cacheFillStack.add(new CacheFillStackEntry(targetCache, request));
			
				/* access the next level*/
				if (targetCache.isLastLevel)
				{
					MemEventQueue.eventQueue/*.get(threadID)*/.add(new MainMemAccessEvent(threadID,
																				lsqEntry, 
																				cacheFillStack, 
																				MemEventQueue.clock 
																				+ SystemConfig.mainMemoryLatency));
					return;
				}
			
				//Change the access type for the next level if the present cache is write back
				if (request.getType() == MemoryAccessType.WRITE && targetCache.writePolicy == CacheConfig.WritePolicy.WRITE_BACK)
				{
					CacheRequestPacket changedRequest = request.copy();
					changedRequest.setType(MemoryAccessType.READ);
					MemEventQueue.eventQueue/*.get(threadID)*/.add(new CacheAccessEvent(threadID,
																				lsqEntry, 
																				targetCache.nextLevel, 
																				changedRequest, 
																				cacheFillStack,
																				MemEventQueue.clock
																				+ targetCache.nextLevel.getLatency()));
				}
				else
				{
					MemEventQueue.eventQueue/*.get(threadID)*/.add(new CacheAccessEvent(threadID,
																				lsqEntry, 
																				targetCache.nextLevel, 
																				request, 
																				cacheFillStack, 
																				MemEventQueue.clock
																				+ targetCache.nextLevel.getLatency()));
				}
			}
		}
		
		/**
		 * Is the access is below the bus
		 */
		else 
		{
			if (cl != null) //Process the access and if it is a hit, proceed
			{	
				MemEventQueue.eventQueue/*.get(threadID)*/.add(new FillCacheStackEvent(threadID,
																					cacheFillStack,
																					stateToSet,
																					MemEventQueue.clock
																					+ cacheFillStack.peek().cache.getLatency()));
				
			}
			else //The cache access is a miss
			{			
				//Add this cache to the stack of caches to be filled when a hit is found
				cacheFillStack.add(new CacheFillStackEntry(targetCache, request));
			
				/* access the next level*/
				if (targetCache.isLastLevel)
				{
					MemEventQueue.eventQueue/*.get(threadID)*/.add(new MainMemAccessEvent(threadID,
																				cacheFillStack,
																				stateToSet,
																				MemEventQueue.clock 
																				+ SystemConfig.mainMemoryLatency));
					return;
				}
				MemEventQueue.eventQueue/*.get(threadID)*/.add(new CacheAccessEvent(threadID,
																				targetCache.nextLevel, 
																				request,  
																				stateToSet,
																				cacheFillStack,
																				MemEventQueue.clock
																				+ targetCache.nextLevel.getLatency()));
				
			}
		}
	}
}
