package generic;



public class Port 
{
	private int noOfPorts;

	//occupancy defines the number of clockCycles it takes for one completion 
	//of a single transfer on the port.
	private Time_t occupancy;
	private Time_t portBusyUntil[];
	
	//FIXME: need a separate arrangement for globalClock.
	private Time_t globalClock;
	
	public Port(int noOfPorts, Time_t occupancy)
	{
		//initialize no. of ports and the occupancy.
		this.noOfPorts = noOfPorts;
		this.occupancy = occupancy;
				
		//If the port is an unlimited port, no need for setting timeBusyUntil field.
		if(!(noOfPorts==-1 && occupancy.equals(-1)))
		{
			for(int i=0; i<noOfPorts; i++)
			{
				this.portBusyUntil[i] = globalClock;
			}
		}
	}
	
	//returns the next available slot.
	Time_t getNextSlot()
	{
		if(this.noOfPorts==-1 && this.occupancy.equals(-1))
		{
			//In case of unlimited ports, return now.
			return globalClock;
		}
		else
		{
			//else return the most recent available slot.
			Time_t availableSlot = portBusyUntil[0];
			for(int i=0; i<noOfPorts; i++)
			{
				if(portBusyUntil[i].lessThan(availableSlot))
				{
					availableSlot = portBusyUntil[i];
				}
			}
			
			if(availableSlot.lessThan(globalClock))
			{
				availableSlot = globalClock;
			}
			
			return availableSlot;
		}
	}
	
	//returns if any port is available for next n slots.
	boolean occupySlots(int noOfSlots)
	{
		if(noOfPorts==-1 && occupancy.equals(-1))
		{
			//In case of unlimited port, anybody can occupy the port for n slots.
			return true;
		}
		else
		{
			for(int i=0; i<noOfPorts; i++)
			{
				if(portBusyUntil[i].lessThan(globalClock))
				{
					portBusyUntil[i].add(noOfSlots);
					return true;
				}
			}
			
			//If we reach here, that means nothing was available.
			//Just return false.
			return false;
		}
	}
	
	/*
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