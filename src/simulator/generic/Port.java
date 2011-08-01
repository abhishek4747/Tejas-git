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
		return getNextSlot(occupancy);
	}
	
	Time_t getNextSlot(Time_t occupancyRequired)
	{
		//In case of unlimited port, tell everyBody to come now
		if(noOfPorts==-1 && occupancy.equals(-1))
		{
			return globalClock;
		}
			
		Time_t availableSlot;
		int availablePort;
				
		availableSlot = portBusyUntil[0];
		availablePort = 0;
		
		for(int i=0; i<noOfPorts; i++)
		{
			if(portBusyUntil[i].lessThan(globalClock))
			{
				//return saying that u can have the port now itself.
				return (globalClock);
			}
			
			else if(portBusyUntil[i].lessThan(availableSlot))
			{
				availablePort = i;
				availableSlot = portBusyUntil[i];
			}
		}
		
		//return the available slot.
		return availableSlot;
	}
	
	
	
	/*
	//returns the next available slot for booking the port for n slots
	Time_t occupySlots(int noOfSlots)
	{
		//In case of unlimited or a priorityBased port, tell everyBody to come now
		//In case of unlimited port, tell everyBody to come now
		if(noOfPorts==-1 && occupancy.equals(-1))
		{
			return globalClock;
		}
		
		
		//This nextSlot  function is called for n times so that we will have a 
		//optimal allocation of ports.
		Time_t firstFlot = getNextSlot();
		
		for(int i=0; i<(noOfPorts-1); i++)
		{
			getNextSlot();
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