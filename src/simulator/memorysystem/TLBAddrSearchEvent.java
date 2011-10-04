/*****************************************************************************
				BhartiSim Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
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
import generic.GlobalClock;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;
import generic.Time_t;

public class TLBAddrSearchEvent extends Event
{
	long address;
	LSQEntry lsqEntry;
	
	public TLBAddrSearchEvent(Time_t eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker,
			RequestType requestType, long address,
			LSQEntry lsqEntry) 
	{
		super(eventTime, requestingElement, processingElement, tieBreaker,
				requestType);
		this.address = address;
		this.lsqEntry = lsqEntry;
	}

	public void handleEvent(EventQueue eventQueue)
	{
		TLB processingTLB = (TLB)(this.getProcessingElement());
		
		// If Entry found in TLB
		if (processingTLB.searchTLBForPhyAddr(address)) 
		{
			//Validate the address in the LSQ right away
			this.getRequestingElement().getPort().put(new LSQValidateEvent(this.getRequestingElement().getLatencyDelay(),//FIXME
														processingTLB,
														this.getRequestingElement(), 
														0, //tieBreaker,
														RequestType.VALIDATE_LSQ_ENTRY, 
														lsqEntry, 
														address));
		}
		else
		{
			//Add the requesting LSQ Index to Outstanding Request table
			boolean alreadyRequested = processingTLB.addOutstandingRequest(TLB.getPageID(address), lsqEntry);
			
			if (!alreadyRequested)
				//Fetch the physical address from from Page table
				MemorySystem.mainMemPort.put(new MainMemAccessForTLBEvent(MemorySystem.getMainMemLatencyDelay(),//FIXME
																	this.getProcessingElement(), 
																	0, //tieBreaker,
																	TLB.getPageID(address),
																	RequestType.MAIN_MEM_ACCESS_TLB));
		}
	}
}
