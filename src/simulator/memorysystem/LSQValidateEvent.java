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

import memorysystem.CacheLine.MESI;
import memorysystem.LSQEntry.LSQEntryType;
import generic.*;

public class LSQValidateEvent extends NewEvent
{
	//CoreMemorySystem containingMemSys;
	int lsqIndex;
	long addr;
	
	public LSQValidateEvent(Time_t eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker,
			RequestType requestType, int lsqIndex, long addr) {
		super(eventTime, requestingElement, processingElement, tieBreaker,
				requestType);
		this.lsqIndex = lsqIndex;
		this.addr = addr;
	}

	public void handleEvent(NewEventQueue newEventQueue)
	{
		LSQ processingLSQ = (LSQ)(this.getProcessingElement());
		
		//If the LSQ entry is a load
		if (processingLSQ.lsqueue[lsqIndex].getType() == LSQEntryType.LOAD)
		{
			//If the value could not be forwarded
			if (!(processingLSQ.loadValidate(lsqIndex, addr)))
			{
				//TODO Read from the cache (CacheAccessEvent)
				CacheRequestPacket request = new CacheRequestPacket();
				//request.setThreadID(0);
				request.setType(RequestType.MEM_READ);
				request.setAddr(processingLSQ.lsqueue[lsqIndex].getAddr()); 
				//Direct address must not be set as it is pageID in some cases
				newEventQueue.addEvent(new NewCacheAccessEvent(new Time_t(GlobalClock.getCurrentTime() +
																		processingLSQ.containingMemSys.l1Cache.getLatency().getTime()),//FIXME
															this.getProcessingElement(),
															processingLSQ.containingMemSys.l1Cache,
															lsqIndex, 
															0, //tieBreaker,
															request));//, 
															//MESI stateToSet, 
															//false));
			}
		}
		else //If the LSQ entry is a store
		{
			processingLSQ.storeValidate(lsqIndex, addr);
		}
	}
}
