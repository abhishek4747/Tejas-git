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

import memorysystem.MemorySystem;

/**
 * @author Moksh Upadhyay
 *	Used to request the acquiring of the ports of the device requested
 *	Takes a final event (to be executed on the requested device) as input
 *	and passes it when the port is acquired
 *	Otherwise, is port is not available, schedules itself for future
 */
public class PortRequestEvent extends NewEvent
{
	NewEvent targetEvent;
	SimulationElement requestedDevice; //null means Main memory
	int noOfSlots;
	
	public PortRequestEvent(long tieBreaker,
							int noOfSlots, 
							NewEvent targetEvent) 
	{
		super(new Time_t(GlobalClock.getCurrentTime()), null, null, tieBreaker,
				targetEvent.requestType);
		this.targetEvent = targetEvent;
		this.requestedDevice = targetEvent.getProcessingElement();
		this.noOfSlots = noOfSlots;
	}

	public void handleEvent(NewEventQueue newEventQueue)
	{
		//If the requested device is not main memory
		if (requestedDevice != null)
		{
			//If the port cannot be occupied
			if (!requestedDevice.port.occupySlots(noOfSlots, requestedDevice.getStepSize()))
			{
				this.getEventTime().setTime(requestedDevice.port.getNextSlot());
				this.setPriority(this.getPriority() + 1); //Increase the priority to prevent starvation (and to maintain preference)
				newEventQueue.addEvent(this);
				return;
			}
		}
		//If the requested device is main memory and the port cannot be occupied
		else if (!MemorySystem.mainMemPort.occupySlots(noOfSlots, MemorySystem.mainMemStepSize))
		{
			this.getEventTime().setTime(MemorySystem.mainMemPort.getNextSlot());
			this.setPriority(this.getPriority() + 1); //Increase the priority to prevent starvation (and to maintain preference)
			newEventQueue.addEvent(this);
			return;
		}
		
		//If port occupied
		targetEvent.getEventTime().add(new Time_t(GlobalClock.getCurrentTime()));
		newEventQueue.addEvent(targetEvent);
	}
}
