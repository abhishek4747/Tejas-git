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

public class BusRequestQElmnt 
{
	protected int requestingThreadID;
	protected BusReqType requestType;
	protected long address;
	protected Cache sourceCache;
	protected CacheLine sourceLine;
	protected Stack<CacheFillStackEntry> cacheFillStack;
	protected LSQEntry lsqEntry;
	
	public BusRequestQElmnt(int _requestingThreadID,
							BusReqType _requestType, 
							Cache _sourceCache, 
							CacheLine _sourceLine, 
							Stack<CacheFillStackEntry> _cacheFillStack,
							LSQEntry _lsqEntry)
	{
		requestingThreadID = _requestingThreadID;
		requestType = _requestType;
		sourceCache = _sourceCache;
		sourceLine = _sourceLine;
		cacheFillStack = _cacheFillStack;
		lsqEntry = _lsqEntry;
	}
}
