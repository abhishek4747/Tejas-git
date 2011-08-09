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

import emulatorinterface.Newmain;
import generic.*;

import java.util.ArrayList;
import java.util.Stack;

import memorysystem.Bus.BusReqType;
import memorysystem.CacheLine.MESI;

import config.CacheConfig;
import config.SystemConfig;

public class NewCacheAccessEvent extends NewEvent 
{
	//int threadID;
	//LSQEntry lsqEntry;
	//Stack<CacheFillStackEntry> cacheFillStack;
	int lsqIndex = LSQ.INVALID_INDEX;
	Cache processingCache;
	CacheRequestPacket request;
	//MESI stateToSet;
	//boolean isBelowBus = false;


	public NewCacheAccessEvent(Time_t eventTime,
								SimulationElement requestingElement,
								Cache processingCache,
								int lsqIndex, 
								long tieBreaker,
								CacheRequestPacket request)//, 
								//MESI stateToSet, 
								//boolean isBelowBus) 
	{
		super(eventTime, requestingElement, processingCache, tieBreaker,
				request.getType());
		this.lsqIndex = lsqIndex;
		this.processingCache = processingCache;
		this.request = request;
		//this.stateToSet = stateToSet;
		//this.isBelowBus = isBelowBus;
	}
/*
	//Fetch and set state(Access from a Source cache from cache coherence)
	public NewCacheAccessEvent(int _threadID,
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
*/
	
	@Override
	public void handleEvent(NewEventQueue newEventQueue)
	{
		//Process the access
		CacheLine cl = processingCache.processRequest(request);

		//IF HIT
		if (cl != null)
		{
			//Schedule the requesting element to receive the block TODO (for LSQ)
			if (request.getType() == RequestType.MEM_READ)
				//Just return the read block
				newEventQueue.addEvent(new BlockReadyEvent(new Time_t(GlobalClock.getCurrentTime() +
																	this.getRequestingElement().getLatency().getTime()), 
															this.processingCache,
															this.getRequestingElement(),
															0,//tieBreaker
															RequestType.MEM_BLOCK_READY,
															request.getAddr(),
															lsqIndex));
			else if (request.getType() == RequestType.MEM_WRITE)
			{
				//Write the data to the cache block (Do Nothing)
/*				//Tell the LSQ (if this is L1) that write is done
				if (lsqIndex != LSQ.INVALID_INDEX)
				{
					newEventQueue.addEvent(new BlockReadyEvent(new Time_t(GlobalClock.getCurrentTime() +
																	this.getRequestingElement().getLatency().getTime()), 
																this.processingCache,
																this.getRequestingElement(),
																0,//tieBreaker
																RequestType.LSQ_WRITE_COMMIT,
																request.getAddr(),
																lsqIndex));
				}	
*/
				
				if (processingCache.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
				{
					//Handle in any case (Whether requesting element is LSQ or cache)
					//TODO : handle write-value forwarding (for Write-Through and Coherent caches)
					if (processingCache.isLastLevel)
						newEventQueue.addEvent(new NewMainMemAccessEvent(new Time_t(GlobalClock.getCurrentTime() +
																				Newmain.mainMemoryLatency.getTime()),//FIXME :main memory latency is going to come here
																		processingCache, 
																		0, //tieBreaker,
																		request.getAddr(),
																		RequestType.MEM_WRITE));
					else
						newEventQueue.addEvent(new NewCacheAccessEvent(new Time_t(GlobalClock.getCurrentTime() +
																				processingCache.nextLevel.getLatency().getTime()),//FIXME
																		processingCache,
																		processingCache.nextLevel,
																		LSQ.INVALID_INDEX, 
																		0, //tieBreaker,
																		request));
				}	
			}
/*				
				//Handling Cache Coherence
				if ((request.getType() == MemoryAccessType.WRITE) && (processingCache.isCoherent) && (!processingCache.nextLevel.enforcesCoherence))
				{
					switch (cl.getState())
					{
					case MODIFIED:
						break; //No need to do anything
					case EXCLUSIVE:
						cl.setState(MESI.MODIFIED);
						break;
					case SHARED:
						MemEventQueue.eventQueue.add(new BusRequestEvent(threadID,
																							BusReqType.INVALIDATE, 
																							request.getAddr(),
																							processingCache,
																							cl,
																							cacheFillStack,
																							lsqEntry,
																							MemEventQueue.clock + 
																							SystemConfig.cacheBusLatency));
						return;
					}
				} //READ needs nothing to be done
*/
		}
		
		//IF MISS
		else
		{
/*				//Handle cache coherence
				if ((processingCache.isCoherent) && (!processingCache.nextLevel.enforcesCoherence))
				{
					if (request.getType() == MemoryAccessType.WRITE)
						MemEventQueue.eventQueue.add(new BusRequestEvent(threadID,
																		BusReqType.RWITM, 
																		request.getAddr(),
																		processingCache,
																		cl,
																		cacheFillStack,
																		lsqEntry,
																		MemEventQueue.clock 
																			+ SystemConfig.cacheBusLatency));
					else
						MemEventQueue.eventQueue.add(new BusRequestEvent(threadID,
																		usReqType.MEM_ACCESS, 
																		request.getAddr(),
																		processingCache,
																		cl,
																		cacheFillStack,
																		lsqEntry,
																		MemEventQueue.clock 
																			+ SystemConfig.cacheBusLatency));
			
					return;
				}
*/
			
			//Add the request to the outstanding request buffer
			processingCache.addOutstandingRequest(request.getAddr(), 
													request.getType(), 
													this.getRequestingElement(), 
													lsqIndex);
			
			// access the next level
			if (processingCache.isLastLevel)
			{
				//FIXME
				newEventQueue.addEvent(new NewMainMemAccessEvent(new Time_t(GlobalClock.getCurrentTime() +
																		Newmain.mainMemoryLatency.getTime()), //FIXME 
																processingCache, 
																0, //tieBreaker,
																request.getAddr(),
																RequestType.MEM_READ));
				return;
			}
			else
			{
				//Change the parameters of this event to forward it for scheduling next cache's access
				this.setEventTime(new Time_t(GlobalClock.getCurrentTime() + processingCache.nextLevel.getLatency().getTime()));//FIXME
				this.setRequestingElement(processingCache);
				this.processingCache = processingCache.nextLevel;
				this.lsqIndex = LSQ.INVALID_INDEX;
				this.request.setType(RequestType.MEM_READ);
				newEventQueue.addEvent(this);
				return;
			}
		}
	}
	
		/**
		 * If the access is below the bus
		 */
/*		else 
		{
			if (cl != null) //Process the access and if it is a hit, proceed
			{	
				MemEventQueue.eventQueue.add(new FillCacheStackEvent(threadID,
																					cacheFillStack,
																					stateToSet,
																					MemEventQueue.clock
																					+ cacheFillStack.peek().cache.getLatency()));
				
			}
			else //The cache access is a miss
			{			
				//Add this cache to the stack of caches to be filled when a hit is found
				cacheFillStack.add(new CacheFillStackEntry(processingCache, request));
			
				// access the next level
				if (processingCache.isLastLevel)
				{
					MemEventQueue.eventQueue.add(new MainMemAccessEvent(threadID,
																				cacheFillStack,
																				stateToSet,
																				MemEventQueue.clock 
																				+ SystemConfig.mainMemoryLatency));
					return;
				}
				MemEventQueue.eventQueue.add(new NewCacheAccessEvent(threadID,
																				processingCache.nextLevel, 
																				request,  
																				stateToSet,
																				cacheFillStack,
																				MemEventQueue.clock
																				+ processingCache.nextLevel.getLatency()));
				
			}
		}
		*/
}
