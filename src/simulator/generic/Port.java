package generic;


public abstract class Port 
{
	private int noOfPorts;
	private long latency;
	private PortType portType;
	private int portBusyUntil[];
	
	//FIXME: multi-porting type can be easily done away by having a combination of 
	//controller and different banks
	//private MultiPortingType multiPortingType;
	
		
	public Port(int noOfPorts, long latency, PortType portType) 
	{
		this.noOfPorts = noOfPorts;
		this.latency = latency;
		this.portType = portType;
		
		//Generate new array
		this.portBusyUntil = new int[noOfPorts];
	}

	//return the next free slot
	long getNextSlot(long currentTime)
	{
		if(portType == PortType.PriorityBased)
		{
			//tell everybody to come in the next time slot.
			//once they come, we will start sorting them.
			return (currentTime + 1);
		}
		
		else if(portType == PortType.FirstComeFirstServe)
		{
			//check for the port that is going to be free first.
			//return this value as the expectedSlot of service.
			int nextFreeSlot;
			
			nextFreeSlot = portBusyUntil[0];
			for(int i=0; i<noOfPorts; i++)
			{
				if(portBusyUntil[i] < nextFreeSlot)
				{
					nextFreeSlot = portBusyUntil[i];
				}
			}
			
			return 	nextFreeSlot;
		}
		
		else
		{
			misc.Error.showErrorAndExit("Invalid port type !!");
			return -1;
		}
	}
}