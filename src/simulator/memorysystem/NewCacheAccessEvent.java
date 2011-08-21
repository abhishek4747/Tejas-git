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
import config.CacheConfig;

public class NewCacheAccessEvent extends NewEvent 
{
	//int threadID;
	//LSQEntry lsqEntry;
	//Stack<CacheFillStackEntry> cacheFillStack;
	//int lsqIndex = LSQ.INVALID_INDEX;
	LSQEntry lsqEntry = null;
	Cache processingCache;
	CacheRequestPacket request;
	//MESI stateToSet;
	//boolean isBelowBus = false;


	public NewCacheAccessEvent(Time_t eventTime,
								SimulationElement requestingElement,
								Cache processingCache,
								LSQEntry lsqEntry, 
								long tieBreaker,
								CacheRequestPacket request)//, 
								//MESI stateToSet, 
								//boolean isBelowBus) 
	{
		super(eventTime, requestingElement, processingCache, tieBreaker,
				request.getType());
		this.lsqEntry = lsqEntry;
		this.processingCache = processingCache;
		this.request = request;
	}
	
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
				newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker, 
						1, //noOfSlots,
						new BlockReadyEvent(this.getRequestingElement().getLatencyDelay(), 
															this.processingCache,
															this.getRequestingElement(),
															0,//tieBreaker
															RequestType.MEM_BLOCK_READY,
															request.getAddr(),
															lsqEntry)));
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
						newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker, 
								1, //noOfSlots,
								new NewMainMemAccessEvent(MemorySystem.getMainMemLatencyDelay(),//FIXME :main memory latency is going to come here
																		processingCache, 
																		0, //tieBreaker,
																		request.getAddr(),
																		RequestType.MEM_WRITE)));
					else
						newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker,
								1, //noOfSlots,
								new NewCacheAccessEvent(processingCache.nextLevel.getLatencyDelay(),//FIXME
																		processingCache,
																		processingCache.nextLevel,
																		null, 
																		0, //tieBreaker,
																		request)));
				}	
			}
		}
		
		//IF MISS
		else
		{			
			//Add the request to the outstanding request buffer
			boolean alreadyRequested = processingCache.addOutstandingRequest(request.getAddr(), 
													request.getType(), 
													this.getRequestingElement(), 
													lsqEntry);
			
			if (!alreadyRequested)
			{
				// access the next level
				if (processingCache.isLastLevel)
				{
					//FIXME
					newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker, 
							1, //noOfSlots,
							new NewMainMemAccessEvent(MemorySystem.getMainMemLatencyDelay(), //FIXME 
																	processingCache, 
																	0, //tieBreaker,
																	request.getAddr(),
																	RequestType.MEM_READ)));
					return;
				}
				else
				{
					//Change the parameters of this event to forward it for scheduling next cache's access
					this.setEventTime(processingCache.nextLevel.getLatencyDelay());//FIXME
					this.setRequestingElement(processingCache);
					this.processingCache = processingCache.nextLevel;
					this.lsqEntry = null;
					this.request.setType(RequestType.MEM_READ);
					newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker, 
							1, //noOfSlots,
							this));
					return;
				}
			}
		}
	}
}
