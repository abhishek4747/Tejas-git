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

import java.util.Stack;

import memorysystem.CacheLine.MESI;
import generic.*;

public class MainMemAccessEvent extends Event 
{
	CoreMemorySystem containingMemSys; // For Page table accesses
	Cache sourceCache; //Only for access from coherent cache
	MESI stateToSet; //Only for access from coherent cache
	int threadID;
	LSQEntry lsqEntry;
	TLB tlbuffer;
	LSQ lsqueue;
	long pageID;
	int lsqIndex;
	MainMemAccessSource AccessSource;
	long addr;
	Stack<CacheFillStackEntry> cacheFillStack;
	
	public static enum MainMemAccessSource {
		CacheGeneral,
		TLB,
		CoherentCache
	}
	
	//Access from cache
	public MainMemAccessEvent(int _threadID,
							LSQEntry _lsqEntry, 
							Stack<CacheFillStackEntry> _cacheFillStack, 
							long eventTime)
	{
		super(eventTime, 2, 0);
		
		AccessSource = MainMemAccessSource.CacheGeneral;
		threadID = _threadID;
		cacheFillStack = _cacheFillStack;
		lsqEntry = _lsqEntry;
	}
	
	//Access from the TLB for Page Table
	public MainMemAccessEvent(CoreMemorySystem _containingMemSys,
							TLB _tlbuffer, 
							long _pageID, 
							long _addr, 
							LSQ _lsqueue, 
							int _lsqIndex, 
							long eventTime)
	{
		super(eventTime, 2, 0);
		
		AccessSource = MainMemAccessSource.TLB;
		containingMemSys = _containingMemSys;
		tlbuffer = _tlbuffer;
		pageID = _pageID;
		lsqueue = _lsqueue;
		lsqIndex = _lsqIndex;
		addr = _addr;
	}
	
	//Fetch and set state(Access from a Source cache from cache coherence)
	public MainMemAccessEvent(int _threadID,
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
	
	
	@Override
	public void handleEvent()
	{
		/*Do nothing for the main memory*/
		
		//If the call was for a page table access from a TLB
		if (AccessSource == MainMemAccessSource.TLB)
		{
			//Add the entry into the TLB
			MemEventQueue.eventQueue/*.get(containingMemSys.threadID)*/.add(new TLBEvent(containingMemSys,
																					pageID,
																					lsqIndex,
																					addr,
																					MemEventQueue.clock
																					+ containingMemSys.TLBuffer.getLatency()));
		}
		else if (AccessSource == MainMemAccessSource.CacheGeneral)//If the call was from the cache
		{
			if (!cacheFillStack.isEmpty())
			{
				MemEventQueue.eventQueue/*.get(threadID)*/.add(new FillCacheStackEvent(threadID,
																					lsqEntry,
																					cacheFillStack,
																					MESI.EXCLUSIVE,//TODO
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
		else if (AccessSource == MainMemAccessSource.CoherentCache)//If the access is for a coherent cache (the access if below the bus)
		{
			MemEventQueue.eventQueue/*.get(threadID)*/.add(new FillCacheStackEvent(threadID,
																					cacheFillStack,
																					stateToSet,
																					MemEventQueue.clock
																					+ cacheFillStack.peek().cache.getLatency()));
		}
	}
}