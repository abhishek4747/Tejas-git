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

import memorysystem.CacheLine.MESI;
import generic.*;

public class NewMainMemAccessEvent extends NewEvent 
{
	long address;
	RequestType requestType;

	MainMemAccessSource AccessSourceType;//To tell what type of element triggered the access
	
	public static enum MainMemAccessSource {
		CacheGeneral,
		CoherentCache
	}
	
	//Access from cache
	public NewMainMemAccessEvent(Time_t eventTime,
			SimulationElement requestingElement, long tieBreaker,
			long address,
			RequestType requestType) 
	{
		super(eventTime, requestingElement, null, tieBreaker,
				requestType);
		AccessSourceType = MainMemAccessSource.CacheGeneral;
		this.address = address;
		this.requestType = requestType;
	}
/*
	//Fetch and set state(Access from a Source cache from cache coherence)
	public NewMainMemAccessEvent(int _threadID,
								Stack<CacheFillStackEntry> _cacheFillStack,
								MESI _stateToSet,
								long eventTime)
	{
		super(eventTime, 2, 0);

		AccessSource = MainMemAccessSource.CoherentCache;
		threadID = _threadID;
		cacheFillStack = _cacheFillStack;
		stateToSet = _stateToSet;
	}
*/
	
	@Override
	public void handleEvent(NewEventQueue newEventQueue)
	{
		/*Do nothing for the main memory*/
		if (AccessSourceType == MainMemAccessSource.CacheGeneral)//If the call was from the cache
		{
			if (requestType == RequestType.MEM_READ)
			{
				newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker, 
						1, //noOfSlots,
						new BlockReadyEvent(this.getRequestingElement().getLatencyDelay(), //FIXME
															null,
															this.getRequestingElement(), 
															0, //tie-breaker
															RequestType.MEM_BLOCK_READY,
															address,
															null)));
			}
			else if (requestType == RequestType.MEM_WRITE)
			{
				//TODO : If we have to simulate the write timings also, then the code will come here
				//Just to tell the requesting things that the write is completed
				Core.outstandingMemRequests--;
			}
		}
/*		else if (AccessSource == MainMemAccessSource.CoherentCache)//If the access is for a coherent cache (the access if below the bus)
		{
			MemEventQueue.eventQueue.add(new FillCacheStackEvent(threadID,
																					cacheFillStack,
																					stateToSet,
																					MemEventQueue.clock
																					+ cacheFillStack.peek().cache.getLatency()));
		}*/
	}
}