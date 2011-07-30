package generic;

import javax.xml.bind.annotation.XmlElementDecl.GLOBAL;


public abstract class Port 
{
	private int noOfUnits;
	
	//occupancy defines the number of clockCycles it takes for one completion 
	//of a single transfer on the port.
	private Time_t occupancy;
	private Time_t portBusyUntil[];
	
	
	public Port(int noOfPorts, int occupancy)
	{
		//initialize no. of ports and the occupancy.
		this.noOfPorts = noOfPorts;
		this.occupancy = occupancy;
		
		//initialize each port as being just used.
		for(int i=0; i<noOfPorts; i++)
		{
			this.portBusyUntil[i] = globalClock;
		}
	}
	
	//returns the next available slot.
	Time_t nextSlot()
	{
		//In case of unlimited ports, always return NOW
		if(noOfPorts==-1 && occupancy==-1)
		{
			return globalClock;
		}
		
		return nextSlot(occupancy);
	}
	
	Time_t nextSlot(Time_t occupancyRequired)
	{
		//In case of unlimited ports, always return NOW
		if(noOfPorts==-1 && occupancy==-1)
		{
			return globalClock;
		}
		
		
		Time_t availableSlot;
		int availablePort;
				
		availableSlot = portBusyUntil[0];
		availablePort = 0;
		
		for(int i=0; i<noOfPorts; i++)
		{
			if(portBusyUntil[i] < globalClock)
			{
				//this means that port[i] is free.
				//Grab this port and make it busy until occupancyRequired cycles
				portBusyUntil[i] = globalClock + occupancyRequired;

				//return saying that u can have the port now itself.
				return (globalClock);
			}
			
			else if(portBusyUntil[i] < availableSlot)
			{
				availablePort = i;
				availableSlot = portBusyUntil[i];
			}
		}
		
		
		//we reached here since there was no port available.
		//so, now the availablePort must be booked in advance.
		portBusyUntil[availablePort] += occupancyRequired;
	
		//return the available slot.
		return availableSlot;
	}
	
	//returns the next available slot for booking the port for n slots
	Time_t occupySlots(int noOfSlots)
	{
		//In case of unlimited ports, always return NOW
		if(noOfPorts==-1 && occupancy==-1)
		{
			return globalClock;
		}
		
		
		//This nextSlot  function is called for n times so that we will have a 
		//optimal allocation of ports.
		Time_t firstFlot = nextSlot();
		
		for(int i=0; i<(nOfSlots-1); i++)
		{
			nextSlot();
		}
	}
	
	//returns the next slot without booking anything
	Time_t calculateNextSlot()
	{
		//In case of unlimited ports, always return NOW
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
		//In case of unlimited ports, always return NOW
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
}