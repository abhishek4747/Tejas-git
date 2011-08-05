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

import generic.NewEventQueue;
import generic.NewEvent;
import generic.RequestType;
import generic.SimulationElement;
import generic.Time_t;

public class LSQAddEvent extends NewEvent 
{
	boolean isLoad;
	long addr;
	//CoreMemorySystem containingMemSys;
/*
	public LSQAddEvent(CoreMemorySystem _containingMemSys, boolean _isLoad, long _addr, long eventTime)
	{
		super(eventTime, 2, 0);
		
		isLoad = _isLoad;
		addr = _addr;
		containingMemSys = _containingMemSys;
	}
*/	
	public LSQAddEvent(Time_t eventTime, SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker,
			RequestType requestType, boolean isLoad, long addr) 
	{
		super(eventTime, requestingElement, processingElement, tieBreaker,
				requestType);
		
		this.isLoad = isLoad;
		this.addr = addr;
	}

	@Override
	public void handleEvent(NewEventQueue newEventQueue)
	{
		//Try to allocate the entry
		int index = ((LSQ)(this.getProcessingElement())).addEntry(isLoad, addr);
		
		//If QUEUE_FULL, schedule to try again
		if (index == LSQ.QUEUE_FULL)
		{
			this.setEventTime(this.getEventTime());//FIXME 
			//TODO : Also change the priority if needed
			
			//Save one object creation and add this event to the event queue again
			newEventQueue.addEvent(this);
		}
			
		//Otherwise, check the TLB for address resolution
		else
			newEventQueue.addEvent.add(new TLBAddrSearchEvent(containingMemSys,
													index,
													addr,
													MemEventQueue.clock 
														+ containingMemSys.TLBuffer.getLatency()));
	}
}
