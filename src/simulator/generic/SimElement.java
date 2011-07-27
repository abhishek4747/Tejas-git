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
package generic;

import config.*;

public abstract class SimElement 
{	
	protected String name;
	protected int ports;
	protected int latency;
	protected MultiPortingType multiPortType;
	
	/**
	 * This method was deleted as it was not found apt
	 * @param Takes as input a request packet for the Element to process
	 * @return boolean for telling whether the operation was successful or not, hit or miss etc
	 */
	//abstract public boolean processRequest(SimRequest request);
	
	
	//Getters and setters
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPorts() {
		return ports;
	}

	public void setPorts(int ports) {
		this.ports = ports;
	}

	public int getLatency() {
		return latency;
	}

	public void setLatency(int latency) {
		this.latency = latency;
	}

	public MultiPortingType getMultiPortType() {
		return multiPortType;
	}

	public void setMultiPortType(MultiPortingType multiPortType) {
		this.multiPortType = multiPortType;
	}
}