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

import generic.*;

public class InvalidateEvent extends Event
{
	Cache targetCache; 
	long address; 
	Cache sourceCache;
	CacheLine sourceLine;
	
	public InvalidateEvent(Cache _targetCache, 
							long _address, 
							Cache _sourceCache,
							CacheLine _sourceLine,
							long eventTime)
	{
		super(eventTime, 4);
		
		targetCache = _targetCache;
		address = _address;
		sourceCache = _sourceCache;
		sourceLine = _sourceLine;
	}
	
	public void handleEvent()
	{
		CacheLine cl = targetCache.access(address);
		if (cl != null)
			cl.setState(MESI.INVALID);
		Bus.snoopingCoresProcessed++;
		
		//If all snooping cores have invalidated their copies, 
		//finally set the source line as MODIFIED and end the request
		if (Bus.snoopingCoresProcessed >= (Bus.upperLevels.size() - 1))
		{
			sourceLine.setState(MESI.MODIFIED);
			Bus.endRequest();
		}
	}
}
