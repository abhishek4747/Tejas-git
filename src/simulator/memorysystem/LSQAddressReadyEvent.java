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

import generic.GlobalClock;
import generic.NewEventQueue;
import generic.NewEvent;
import generic.RequestType;
import generic.SimulationElement;
import generic.Time_t;

public class LSQAddressReadyEvent extends NewEvent 
{
	LSQEntry lsqEntry;
	
	public LSQAddressReadyEvent(Time_t eventTime, SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker,
			RequestType requestType, LSQEntry lsqEntry)
	{
		super(eventTime, requestingElement, processingElement, tieBreaker,
				requestType);
		
		this.lsqEntry =lsqEntry;
	}

	@Override
	public void handleEvent(NewEventQueue newEventQueue)
	{
		LSQ processingLSQ = (LSQ)(this.getProcessingElement());
		
		processingLSQ.containingMemSys.TLBuffer.getPort().put(new TLBAddrSearchEvent(processingLSQ.containingMemSys.TLBuffer.getLatencyDelay(), //FIXME
														processingLSQ,
														processingLSQ.containingMemSys.TLBuffer, 
														0, //tieBreaker,
														RequestType.TLB_SEARCH, 
														lsqEntry.getAddr(),
														lsqEntry));
	}
}
