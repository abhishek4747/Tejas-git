package generic;

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
	SimulationElement requestedDevice;
	int noOfSlots;
	
	public PortRequestEvent(long tieBreaker, 
							RequestType requestType, 
							int noOfSlots, 
							NewEvent targetEvent) 
	{
		super(new Time_t(GlobalClock.getCurrentTime()), null, null, tieBreaker,
				requestType);
		this.targetEvent = targetEvent;
		this.requestedDevice = targetEvent.getProcessingElement();
		this.noOfSlots = noOfSlots;
	}

	public void handleEvent(NewEventQueue newEventQueue)
	{
		//If the port has not been occupied
		if ((requestedDevice != null) && (!requestedDevice.port.occupySlots(noOfSlots)))
		{
			this.setEventTime(requestedDevice.port.getNextSlot());
			newEventQueue.addEvent(this);
			return;
		}
		//If port occupied
		targetEvent.getEventTime().add(new Time_t(GlobalClock.getCurrentTime()));
		newEventQueue.addEvent(targetEvent);
	}
}
