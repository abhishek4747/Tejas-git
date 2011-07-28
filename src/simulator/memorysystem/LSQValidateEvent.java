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

import memorysystem.LSQEntry.LSQEntryType;
import generic.*;

public class LSQValidateEvent extends Event
{
	CoreMemorySystem containingMemSys;
	int lsqIndex;
	long addr;
	
	public LSQValidateEvent(CoreMemorySystem _containingMemSys, int _lsqIndex, long _addr, long eventTime)
	{
		super(eventTime, 2, 0);
		
		lsqIndex = _lsqIndex;
		addr = _addr;
		containingMemSys = _containingMemSys;
	}
	
	public void handleEvent()
	{
		if (containingMemSys.lsqueue.lsqueue[lsqIndex].getType() == LSQEntryType.LOAD)
			containingMemSys.lsqueue.loadValidate(lsqIndex, addr);
		else
			containingMemSys.lsqueue.storeValidate(lsqIndex, addr);
	}
}
