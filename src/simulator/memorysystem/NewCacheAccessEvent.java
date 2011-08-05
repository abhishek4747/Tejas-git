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
		CacheLine cl = processingCache.processRequest(request);

		//if (!isBelowBus)
		{
			if (cl != null) //Process the access and if it is a hit, proceed
			{
				//Schedule the requesting element to receive the block
	/*			if (lsqIndex == LSQ.INVALID_INDEX)
					newEventQueue.addEvent(new BlockReadyEvent(this.getRequestingElement().getLatency(), 
															this.processingCache,
															this.getRequestingElement(),
															0,//tieBreaker
															RequestType.MEM_BLOCK_READY));
				else*/
					newEventQueue.addEvent(new BlockReadyEvent(this.getRequestingElement().getLatency(), 
															this.processingCache,
															this.getRequestingElement(),
															0,//tieBreaker
															RequestType.MEM_BLOCK_READY,
															request.getAddr(),
															lsqIndex));
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
			else //The cache access is a miss
			{
/*
				//Handle cache coherence
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
					newEventQueue.addEvent(new NewMainMemAccessEvent(processingCache.getLatency(), //FIXME : this is COMPLETELY incorrect
																	processingCache, 
																	0, //tieBreaker,
																	request.getAddr(),
																	RequestType.MEM_READ));
					return;
				}
				else
				{
					//Change the parameters of this event to forward it for scheduling next cache's access
					this.setRequestingElement(processingCache);
					this.processingCache = processingCache.nextLevel;
					this.lsqIndex = LSQ.INVALID_INDEX;
					this.request.setType(RequestType.MEM_READ);
					this.setEventTime(processingCache.getLatency());//FIXME
					newEventQueue.addEvent(this);
				}
			}
		}
		
		/**
		 * Is the access is below the bus
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
}
