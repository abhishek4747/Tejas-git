package generic;

public class Port 
{
	private PortType portType;
	private int noOfPorts;
	private EventQueue eventQueue;

	//occupancy defines the number of clockCycles needed for  
	//a single transfer on the port.
	private Time_t occupancy;
	private Time_t[] portBusyUntil;
	
	//NOTE : Time is in terms of GlobalClock cycles
	
	public Port(PortType portType, int noOfPorts, Time_t occupancy,
			EventQueue eventQueue)
	{
		this.portType = portType;
		
		//initialise no. of ports and the occupancy.
		if(portType==PortType.Unlimited)
		{
			return;
		}
		
		else if(portType!=PortType.Unlimited && 
				noOfPorts>0 && occupancy.greaterThan(new Time_t(0)))
		{
			// For a FCFS or a priority based port, noOfPorts and
			// occupancy must be non-zero.
			this.noOfPorts = noOfPorts;
			this.occupancy = occupancy;
			
			//set busy field of all ports to 0
			portBusyUntil = new Time_t[noOfPorts];
					
			for(int i=0; i < noOfPorts; i++)
			{
				this.portBusyUntil[i] = new Time_t(0);
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
		if(this.portType==PortType.Unlimited)
		{
			// For an unlimited port, add the event with current-time.
			event.setEventTime(new Time_t(GlobalClock.getCurrentTime()));
			eventQueue.addEvent(event);
			return;
		}
		
		else if(this.portType==PortType.FirstComeFirstServe)
		{
			//else return the slot that will be available earliest.
			int availablePortID = 0;
			for(int i=0; i<noOfPorts; i++)
			{
				if(portBusyUntil[i].getTime() < 
						portBusyUntil[availablePortID].getTime())
				{
					availablePortID = i;
				}
			}
			
			// If all the ports are available, return current-time.
			if(portBusyUntil[availablePortID].
					lessThan(new Time_t(GlobalClock.getCurrentTime())))
			{
				portBusyUntil[availablePortID].setTime(GlobalClock.getCurrentTime());
			}
			
			//set the port as busy for occupancy amount of time.
			portBusyUntil[availablePortID].add(occupancy);
			
			// set the time of the event
			event.setEventTime(portBusyUntil[availablePortID]);
			
			// add event in the eventQueue
			eventQueue.addEvent(event);
		}
	}
	
	/*
	//returns the next available slot.
	public long getNextSlot()
	{
		
		if ((this.noOfPorts == -1) && (this.occupancy.getTime() == -1))
		{
			//In case of unlimited ports, return current-time.
			return GlobalClock.getCurrentTime();
		}
		else
		{
			//else return the slot that will be available earliest.
			long availableSlot = portBusyUntil[0].getTime();
			for(int i=0; i<noOfPorts; i++)
			{
				if(portBusyUntil[i].getTime() < availableSlot)
				{
					availableSlot = portBusyUntil[i].getTime();
				}
			}
			
			// If all the ports are available, return current-time.
			if(!(availableSlot > GlobalClock.getCurrentTime()))
			{
				availableSlot = GlobalClock.getCurrentTime();
			}
			
			return availableSlot;
		}
	}
	
	//returns true if any port is available for next n slots.
	public boolean occupySlots(int noOfSlots, int stepSize)
	{
		if(noOfPorts == -1 && (this.occupancy.getTime() == -1))
		{
			//In case of unlimited port, anybody can occupy the port for n slots.
			return true;
		}
		else
		{
			for(int i=0; i < noOfPorts; i++)
			{
				if(!portBusyUntil[i].greaterThan(new Time_t(GlobalClock.getCurrentTime())))
				{
					portBusyUntil[i].add(new Time_t(noOfSlots * occupancy.getTime() * stepSize));
					return true;
				}
			}
			
			//If we reach here, it means nothing was available.
			//Just return false.
			return false;
		}
	}
	
	
	//returns the next slot without booking anything
	Time_t calculateNextSlot()
	{
		//In case of unlimited port, tell everyBody to come now
		if(noOfPorts==-1 && occupancy==-1)
		{
			return globalClock;
		}
		
		
		Time_t availableSlot;
		
		availableSlot = portBusyUntil[0];
		for(i=0; i<noOfPorts; i++)
		{
			if(portBusyUntil[i] < availableSlot)
				availableSlot = portBusyUntil[i];
		}
		
		if(availableSlot < globalClock)
		{
			return globalClock;
		}
		else
		{
			return availableSlot;
		}
	}
	
	//locks the port for n cycles
	void lockForNCycles(Time_t noOfCycles)
	{
		//In case of unlimited port, tell everyBody to come now
		if(noOfPorts==-1 && occupancy==-1)
		{
			return;
		}
		
		
		Time_t nextAvailableSlot = calculateNextSlot();
		Time_t lockTillSlot = nextAvailableSlot + noOfCycles;
		for(int i = 0; i < noOfPorts; i++)
		{
			if( portBusyUntil[i] < lockTillSlot )
			{
				portBusyUntil[i] = lockTillSlot;
			}
		}
	}
	*/
}