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

import java.util.Stack;

import generic.*;

public class LSQCommitEventFromROB extends NewEvent
{
	LSQEntry lsqEntry;
	
	public LSQCommitEventFromROB(Time_t eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker,
			RequestType requestType, LSQEntry lsqEntry) 
	{
		super(eventTime, requestingElement, processingElement, tieBreaker,
				requestType);
		this.lsqEntry = lsqEntry;
	}

	public void handleEvent(NewEventQueue newEventQueue)
	{
		LSQ processingLSQ = (LSQ)(this.getProcessingElement());
		
		//Check the error condition
		if (lsqEntry.getIndexInQ() != processingLSQ.head)
		{
			System.err.println("Error in LSQ :  ROB sent commit for an instruction other than the one at the head");
			System.exit(1);
		}
		
//TODO : This needs to be moved some place especially for the store when it finally commits()
		// advance the head of the queue
		LSQEntry entry = lsqEntry;
		
		// if it is a store, send the request to the cache
		if(entry.getType() == LSQEntry.LSQEntryType.STORE) 
		{
			//TODO Write to the cache
			CacheRequestPacket request = new CacheRequestPacket();
			//request.setThreadID(0);
			request.setType(RequestType.MEM_WRITE);
			request.setAddr(entry.getAddr());
			processingLSQ.containingMemSys.l1Cache.getPort().put(new NewCacheAccessEvent(processingLSQ.containingMemSys.l1Cache.getLatencyDelay(), //FIXME
															processingLSQ,
															processingLSQ.containingMemSys.l1Cache,
															entry, 
															0, //tieBreaker,
															request));
			
			processingLSQ.head = processingLSQ.incrementQ(processingLSQ.head);
			processingLSQ.curSize--;
		}
		
		//If it is a LOAD which has received its value
		else if (entry.isForwarded())
		{
			processingLSQ.head = processingLSQ.incrementQ(processingLSQ.head);
			processingLSQ.curSize--;
		}
		
		//If it is a LOAD which has not yet received its value
		else
		{
			System.err.println("Error in LSQ " +processingLSQ.containingMemSys.coreID+ " :  ROB sent commit for a load which has not received its value");
			System.exit(1);
		}
	}
}
