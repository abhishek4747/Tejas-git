package generic;

public class Port 
{
	private PortType portType;
	private int noOfPorts;
	private EventQueue eventQueue;

	//occupancy defines the number of clockCycles needed for  
	//a single transfer on the port.
	private long occupancy;
	private long[] portBusyUntil;
	
	//NOTE : Time is in terms of GlobalClock cycles
	
	public Port(PortType portType, int noOfPorts, long occupancy,
			EventQueue eventQueue)
	{
		this.portType = portType;
		this.eventQueue = eventQueue;
		
		//initialise no. of ports and the occupancy.
		if(portType==PortType.Unlimited)
		{
			return;
		}
		
		else if(portType!=PortType.Unlimited && 
				noOfPorts>0 && occupancy>0)
		{
			// For a FCFS or a priority based port, noOfPorts and
			// occupancy must be non-zero.
			this.noOfPorts = noOfPorts;
			this.occupancy = occupancy;
			
			//set busy field of all ports to 0
			portBusyUntil = new long[noOfPorts];
					
			for(int i=0; i < noOfPorts; i++)
			{
				this.portBusyUntil[i] = 0;
			}
		}
		
		else
		{
			// Display an error for invalid initialization.
			misc.Error.showErrorAndExit("Invalid initialization of port !!\n" +
					"port-type=" + portType + " no-of-ports=" + noOfPorts + 
					" occupancy=" + occupancy);
		}
	}
	
	public void put(Event event)
	{
		//overloaded method.
		this.put(event, 1);
	}
	
	public void put(Event event, int noOfSlots)
	{
		if(this.portType == PortType.Unlimited)
		{
			// For an unlimited port, add the event with current-time.
			event.addEventTime(GlobalClock.getCurrentTime());
			eventQueue.addEvent(event);
			return;
		}
		
		else if(this.portType==PortType.FirstComeFirstServe)
		{
			//else return the slot that will be available earliest.
			int availablePortID = 0;
			for(int i=0; i<noOfPorts; i++)
			{
				if(portBusyUntil[i]< 
						portBusyUntil[availablePortID])
				{
					availablePortID = i;
				}
			}
			
			// If all the ports are available, return current-time.
			if(portBusyUntil[availablePortID]<
					GlobalClock.getCurrentTime())
			{
				portBusyUntil[availablePortID] = GlobalClock.getCurrentTime();
			}
			
			//set the port as busy for occupancy amount of time.
			portBusyUntil[availablePortID] += occupancy;
			
			// set the time of the event
			event.addEventTime(portBusyUntil[availablePortID]);
			
			// add event in the eventQueue
			eventQueue.addEvent(event);
		}
	}
}