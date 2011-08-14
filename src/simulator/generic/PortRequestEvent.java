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
				this.setEventTime(requestedDevice.port.getNextSlot());
				this.setPriority(this.getPriority() + 1); //Increase the priority to prevent starvation (and to maintain preference)
				newEventQueue.addEvent(this);
				return;
			}
		}
		//If the requested device is main memory and the port cannot be occupied
		else if (!MemorySystem.mainMemPort.occupySlots(noOfSlots, MemorySystem.mainMemStepSize))
		{
			this.setEventTime(MemorySystem.mainMemPort.getNextSlot());
			this.setPriority(this.getPriority() + 1); //Increase the priority to prevent starvation (and to maintain preference)
			newEventQueue.addEvent(this);
			return;
		}
		
		//If port occupied
		targetEvent.getEventTime().add(new Time_t(GlobalClock.getCurrentTime()));
		newEventQueue.addEvent(targetEvent);
	}
}
