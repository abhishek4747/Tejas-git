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

import emulatorinterface.Newmain;
import memorysystem.CacheLine.MESI;
import generic.*;

public class MainMemAccessForTLBEvent extends Event 
{
	long pageID;
	
	public MainMemAccessForTLBEvent(Time_t eventTime,
			SimulationElement requestingElement, long tieBreaker,
			long pageID,
			RequestType requestType) 
	{
		super(eventTime, requestingElement, null, tieBreaker,
				requestType);
		this.pageID = pageID;
	}
	
	@Override
	public void handleEvent(EventQueue eventQueue)
	{
		/*Do nothing for the main memory*/
		//Add the entry into the TLB
		this.getRequestingElement().getPort().put(new TLBAddEntryEvent(this.getRequestingElement().getLatencyDelay(),//FIXME
													null,
													this.getRequestingElement(), 
													0, //tieBreaker,
													RequestType.TLB_ADDRESS_READY, 
													pageID));
	}
}