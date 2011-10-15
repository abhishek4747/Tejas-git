package memorysystem;

import config.SystemConfig;
import generic.EventQueue;
import generic.SimulationElement;
import generic.Core;
import generic.Event;
import generic.RequestType;

public class MainMemory extends SimulationElement
{
	public MainMemory(EventQueue eventQueue) {
		super(SystemConfig.mainMemPortType,
				SystemConfig.mainMemoryAccessPorts,
				SystemConfig.mainMemoryPortOccupancy,
				eventQueue,
				SystemConfig.mainMemoryLatency,
				SystemConfig.mainMemoryFrequency
				);
		// TODO Auto-generated constructor stub
	}
	
	public void handleEvent(Event event)
	{
		if (event.getRequestType() == RequestType.Main_Mem_Read)
		{
			event.getRequestingElement().getPort().put(
					event.update(
							event.getRequestingElement().getLatencyDelay(),
							null,
							event.getRequestingElement(),
							RequestType.Mem_Response));
		}
		else if (event.getRequestType() == RequestType.Main_Mem_Write)
		{
			//TODO : If we have to simulate the write timings also, then the code will come here
			//Just to tell the requesting things that the write is completed
			Core.outstandingMemRequests--;
		}
	}
}
