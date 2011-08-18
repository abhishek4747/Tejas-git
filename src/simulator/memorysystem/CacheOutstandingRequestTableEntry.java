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

import generic.RequestType;
import generic.SimulationElement;

public class CacheOutstandingRequestTableEntry
{	
	RequestType requestType;
	SimulationElement requestingElement;
	long address;
	
	/**
	 * Just stores the LSQ entry index if the ready event is for an LSQ.
	 * Stores the INVALID_INDEX otherwise.
	 */
	//int lsqIndex = LSQ.INVALID_INDEX;
	LSQEntry lsqEntry = null;
	
	public CacheOutstandingRequestTableEntry(RequestType requestType,
			SimulationElement requestingElement, long address, LSQEntry lsqEntry) 
	{
		super();
		this.requestType = requestType;
		this.requestingElement = requestingElement;
		this.address =address;
		this.lsqEntry = lsqEntry;
	}	
}
