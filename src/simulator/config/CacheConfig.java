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
package config;

import generic.MultiPortingType;

public class CacheConfig 
{
	public WritePolicy writePolicy;
	public boolean isLastLevel;
	public String nextLevel;
	public int blockSize;
	public int assoc;
	public int size;
	public int latency;
	
	public int accessPorts;
	public MultiPortingType multiportType;
	
	public static enum WritePolicy{
		WRITE_BACK, WRITE_THROUGH
	}

	//Getters and setters
	public WritePolicy getWritePolicy() {
		return writePolicy;
	}

	public void setWritePolicy(WritePolicy writePolicy) {
		this.writePolicy = writePolicy;
	}

	public boolean isLastLevel() {
		return isLastLevel;
	}

	public void setLastLevel(boolean isLastLevel) {
		this.isLastLevel = isLastLevel;
	}

	public String getNextLevel() {
		return nextLevel;
	}

	public void setNextLevel(String nextLevel) {
		this.nextLevel = nextLevel;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
	}

	public int getAssoc() {
		return assoc;
	}

	public void setAssoc(int assoc) {
		this.assoc = assoc;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getLatency() {
		return latency;
	}

	public int getAccessPorts() {
		return accessPorts;
	}

	public void setAccessPorts(int accessPorts) {
		this.accessPorts = accessPorts;
	}

	public MultiPortingType getMultiportType() {
		return multiportType;
	}

	public void setMultiportType(MultiPortingType multiportType) {
		this.multiportType = multiportType;
	}

	public void setLatency(int latency) {
		this.latency = latency;
	}
}
