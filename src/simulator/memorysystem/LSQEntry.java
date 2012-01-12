/*****************************************************************************
				Tejas Simulator
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

import pipeline.outoforder_new_arch.ReorderBufferEntry;


public class LSQEntry
{
	private int indexInQ;
	private LSQEntryType type;

	private ReorderBufferEntry robEntry;
	private long addr;
	private boolean valid;
	private boolean forwarded;//Whether the load has got its value or not

	private boolean removed; //If the entry has been committed and removed from the LSQ
	
	public enum LSQEntryType {LOAD, STORE};
	
	public LSQEntry(LSQEntryType type, ReorderBufferEntry robEntry)
	{
		this.type = type;
		this.robEntry = robEntry;
		valid = false;
		forwarded = false;
		removed = false;
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
		
		if(this.valid == true && valid == true)
		{
			System.out.println("entry already valid");
		}
		this.valid = valid;
		
//		if(valid == true &&
//				(robEntry.isOperand1Available() == false ||
//						robEntry.isOperand2Available() == false ||
//						robEntry.getIssued() == false))
//		{
//			System.out.println("i'm setting valid to true, even before the core has issued the load/store");
//		}
	}

	public boolean isForwarded() {
		return forwarded;
	}

	public void setForwarded(boolean forwarded) {
		
		if(this.forwarded == true && forwarded == true)
		{
			System.out.println("entry already forwarded");
		}
		
		this.forwarded = forwarded;
		
//		if(forwarded == true &&
//				(robEntry.isOperand1Available() == false ||
//						robEntry.isOperand2Available() == false ||
//						robEntry.getIssued() == false))
//		{
//			System.out.println("i'm setting forwarded to true, even before the core has issued the load/store");
//		}
	}
	
	protected boolean isRemoved() {
		return removed;
	}

	public void setRemoved(boolean removed) {
		this.removed = removed;
	}
	
	public ReorderBufferEntry getRobEntry() {
		return robEntry;
	}

	protected void setIndexInQ(int indexInQ) {
		this.indexInQ = indexInQ;
	}

	public int getIndexInQ() {
		return indexInQ;
	}
	
	
}
