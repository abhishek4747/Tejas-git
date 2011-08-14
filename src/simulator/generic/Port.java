package generic;

public class Port 
{
	private int noOfPorts;

	//occupancy defines the number of clockCycles it takes for one completion 
	//of a single transfer on the port.
	private Time_t occupancy;
	private Time_t[] portBusyUntil;
	
	//NOTE : all notions of time is in terms of GlobalClock cycles
	
	public Port(int noOfPorts, Time_t occupancy)
	{
		//initialize no. of ports and the occupancy.
		this.noOfPorts = noOfPorts;
		this.occupancy = occupancy;
		
		if (noOfPorts > 0)
			portBusyUntil = new Time_t[noOfPorts];
				
		//If the port is an unlimited port, no need for setting timeBusyUntil field.
		if(!(noOfPorts==-1 && occupancy.equals(-1)))
		{
			for(int i=0; i < noOfPorts; i++)
			{
				this.portBusyUntil[i] = new Time_t(GlobalClock.getCurrentTime());
			}
		}
	}
	
	//returns the next available slot.
	public Time_t getNextSlot()
	{
		Time_t temp = new Time_t(GlobalClock.getCurrentTime());
		
		if ((this.noOfPorts == -1) && (this.occupancy.getTime() == -1))
		{
			//In case of unlimited ports, return now.
			return temp;
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
			
			if(availableSlot.lessThan(temp))
			{
				availableSlot = temp;
			}
			
			return availableSlot;
		}
	}
	
	//returns if any port is available for next n slots.
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
				if(portBusyUntil[i].lessThan(new Time_t(GlobalClock.getCurrentTime())))
				{
					portBusyUntil[i].add(new Time_t(noOfSlots * occupancy.getTime() * stepSize));
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