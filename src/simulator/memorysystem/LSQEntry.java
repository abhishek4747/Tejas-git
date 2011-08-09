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

import pipeline.outoforder.ReorderBufferEntry;


public class LSQEntry
{
	private LSQEntryType type;
	public ReorderBufferEntry getRobEntry() {
		return robEntry;
	}

	private ReorderBufferEntry robEntry;
	private long addr;
	private boolean valid;
	private boolean forwarded;//Whether the load has got its value or not

	private boolean storeCommitted;
	
	public enum LSQEntryType {LOAD, STORE};
	
	public LSQEntry(LSQEntryType type, ReorderBufferEntry robEntry)
	{
		this.type = type;
		this.robEntry = robEntry;
		valid = false;
		forwarded = false;
		storeCommitted = false;
	}

	public LSQEntryType getType() {
		return type;
	}
	
	public long getAddr() {
		return addr;
	}

	public void setAddr(long addr) {
		this.addr = addr;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public boolean isForwarded() {
		return forwarded;
	}

	public void setForwarded(boolean forwarded) {
		this.forwarded = forwarded;
	}
	
	protected boolean isStoreCommitted() {
		return storeCommitted;
	}

	protected void setStoreCommitted(boolean storeCommitted) {
		this.storeCommitted = storeCommitted;
	}
}
