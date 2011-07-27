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

import generic.*;

public class CacheRequestPacket
{
		private int threadID;
		private double time;
		private MemoryAccessType memRequestType;
		private long memoryReqAddr;
		
		private boolean writeThrough = false; //For testing purposes (for cache)
		
		public CacheRequestPacket() ////For testing purposes only
		{
			this.setWriteThrough(false);
		}
		
		public CacheRequestPacket copy()
		{
			CacheRequestPacket newEntry = new CacheRequestPacket();
			newEntry.setThreadID(this.getThreadID());
			newEntry.setTime(this.getTime());
			newEntry.setType(this.getType());
			newEntry.setAddr(this.getAddr());
			return newEntry;
		}
		
		//Getters and setters
		public int getThreadID() {
			return threadID;
		}

		public void setThreadID(int threadID) {
			this.threadID = threadID;
		}
		
		public double getTime() {
			return time;
		}

		public void setTime(double time) {
			this.time = time;
		}

		public MemoryAccessType getType() {
			return memRequestType;
		}

		public void setType(MemoryAccessType type) {
			this.memRequestType = type;
		}

		public long getAddr() {
			return memoryReqAddr;
		}

		public void setAddr(long addr) {
			this.memoryReqAddr = addr;
		}

		public boolean isWriteThrough() {
			return writeThrough;
		}

		public void setWriteThrough(boolean writeThrough) {
			this.writeThrough = writeThrough;
		}
}
