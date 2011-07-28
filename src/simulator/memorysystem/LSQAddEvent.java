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

public class LSQAddEvent extends Event 
{
	boolean isLoad;
	long addr;
	CoreMemorySystem containingMemSys;
	
	public LSQAddEvent(CoreMemorySystem _containingMemSys, boolean _isLoad, long _addr, long eventTime)
	{
		super(eventTime, 2, 0);
		
		isLoad = _isLoad;
		addr = _addr;
		containingMemSys = _containingMemSys;
	}
	
	@Override
	public void handleEvent()
	{
		//Try to allocate the entry
		int index = containingMemSys.lsqueue.addEntry(isLoad, addr);
		
		//If QUEUE_FULL, schedule to try again
		if (index == LSQ.QUEUE_FULL)
			MemEventQueue.eventQueue/*.get(containingMemSys.threadID)*/.add(new LSQAddEvent(containingMemSys,
																			isLoad,
																			addr,
																			MemEventQueue.clock
																			+ containingMemSys.lsqueue.getLatency()));
																								//TODO Check this latency finally
		//Otherwise, check the TLB for address resolution
		else
			MemEventQueue.eventQueue/*.get(containingMemSys.threadID)*/.add(new TLBEvent(containingMemSys,
																					index,
																					addr,
																					MemEventQueue.clock 
																					+ containingMemSys.TLBuffer.getLatency()));
	}
}
