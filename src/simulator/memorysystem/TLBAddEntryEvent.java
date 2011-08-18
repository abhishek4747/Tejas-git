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

import java.util.ArrayList;

import generic.GlobalClock;
import generic.NewEvent;
import generic.NewEventQueue;
import generic.PortRequestEvent;
import generic.RequestType;
import generic.SimulationElement;
import generic.Time_t;

public class TLBAddEntryEvent extends NewEvent
{
	long pageID;
	
	public TLBAddEntryEvent(Time_t eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker,
			RequestType requestType, long pageID) 
	{
		super(eventTime, requestingElement, processingElement, tieBreaker,
				requestType);
		this.pageID = pageID;
	}
	
	public void handleEvent(NewEventQueue newEventQueue)
	{
		TLB processingTLB = (TLB)(this.getProcessingElement());
		
		//Add the entry into the TLB
		processingTLB.addTLBEntry(pageID);
		
		//TODO : Pickup all outstanding requests and LSQValidate them
		ArrayList<LSQEntry> outstandingRequestList = processingTLB.outstandingRequestTable.get(pageID);
		
		while (!outstandingRequestList.isEmpty())
		{
			newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker, 
					1, //noOfSlots,
					new LSQValidateEvent(processingTLB.containingMemSys.lsqueue.getLatencyDelay(), //FIXME
														processingTLB,
														processingTLB.containingMemSys.lsqueue, 
														0, //tieBreaker,
														RequestType.VALIDATE_LSQ_ENTRY, 
														outstandingRequestList.remove(0), 
														pageID))); //FIXME : Right now, we are passing pageID in place of ADDRESS
		}
	}
}
