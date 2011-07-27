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

import memorysystem.Bus.BusReqType;
import generic.*;

public class BusRequestEvent extends Event
{
	int requestingThreadID;
	BusReqType requestType; 
	long addr;
	Cache sourceCache;
	CacheLine sourceLine;
	Stack<CacheFillStackEntry> cacheFillStack;
	LSQEntry lsqEntry;
	
	public BusRequestEvent(int _requestingThreadID,
							BusReqType _requestType,
							long _addr,
							Cache _sourceCache,
							CacheLine _sourceLine,
							Stack<CacheFillStackEntry> _cacheFillStack,
							LSQEntry _lsqEntry,
							long eventTime)
	{
		super(eventTime, 3);
		
		requestingThreadID =_requestingThreadID;
		requestType = _requestType;
		addr = _addr;
		sourceCache = _sourceCache;
		sourceLine = _sourceLine;
		cacheFillStack = _cacheFillStack;
		lsqEntry = _lsqEntry;
	}
	
	@Override
	public void handleEvent()
	{
		if (Bus.isLocked)
		{
			//Bus is locked. Thus put the request in the request queue of the Bus
			Bus.reqQueue.add(new BusRequestQElmnt(requestingThreadID, requestType, sourceCache, sourceLine, cacheFillStack, lsqEntry));
		}
		else
		{
			//First acquire the lock
			Bus.isLocked = true;
			
			//Add the request to the bus
			Bus.newRequest(requestingThreadID, requestType, addr, sourceCache, sourceLine, cacheFillStack, lsqEntry);
			
			//Process the newly added request
			Bus.processRequest();
		}
	}
}
