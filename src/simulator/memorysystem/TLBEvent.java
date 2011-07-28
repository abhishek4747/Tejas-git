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

import generic.Event;

public class TLBEvent extends Event 
{
	long addr;
	CoreMemorySystem containingMemSys;
	int lsqIndex;
	long pageID;
	boolean addingEntry = false;
	
	//For TLB Access from LSQ
	public TLBEvent(CoreMemorySystem _containingMemSys, 
							int _lsqIndex, 
							long _addr, 
							long eventTime)
	{
		super(eventTime, 2, 0);
		
		addr = _addr;
		containingMemSys = _containingMemSys;
		lsqIndex = _lsqIndex;
	}
	
	//For adding an entry to TLB after fetching it from Main Memory
	public TLBEvent(CoreMemorySystem _containingMemSys,
							long _pageID,
							int _lsqIndex, 
							long _addr, 
							long eventTime)
	{
		super(eventTime, 2, 0);
		
		addingEntry = true;
		pageID = _pageID;
		addr = _addr;
		containingMemSys = _containingMemSys;
		lsqIndex = _lsqIndex;
	}
	
	@Override
	public void handleEvent()
	{
		if (addingEntry) //If the event call is to add the entry to TLB after fetching it from Main memory
		{
			//Add the entry into the TLB
			containingMemSys.TLBuffer.addTLBEntry(pageID);
			
			MemEventQueue.eventQueue/*.get(containingMemSys.threadID)*/.add(new LSQValidateEvent(containingMemSys,
																							lsqIndex,
																							addr,
																							MemEventQueue.clock
																							+ containingMemSys.lsqueue.getLatency()));
		}
		
		//Otherwsie Get address from TLB
		else if (containingMemSys.TLBuffer.getPhyAddrPage(addr, containingMemSys.lsqueue, lsqIndex)) // If Entry found in TLB
		{
			
			MemEventQueue.eventQueue/*.get(containingMemSys.threadID)*/.add(new LSQValidateEvent(containingMemSys,
																							lsqIndex,
																							addr,
																							MemEventQueue.clock
																							+ containingMemSys.lsqueue.getLatency()));
		}
	}
}
