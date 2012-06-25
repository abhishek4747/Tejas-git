package memorysystem;

import memorysystem.nuca.NucaCacheBank;
import memorysystem.nuca.NucaCache.NucaType;
import config.SystemConfig;
import generic.EventQueue;
import generic.SimulationElement;
import generic.Core;
import generic.Event;
import generic.RequestType;

public class MainMemory extends SimulationElement
{
	NucaType nucaType;
	public MainMemory(NucaType nucaType) {
		super(SystemConfig.mainMemPortType,
				SystemConfig.mainMemoryAccessPorts,
				SystemConfig.mainMemoryPortOccupancy,
				SystemConfig.mainMemoryLatency,
				SystemConfig.mainMemoryFrequency
				);
		this.nucaType = nucaType;
		// TODO Auto-generated constructor stub
	}
	
	public void handleEvent(EventQueue eventQ, Event event)
	{
		if (event.getRequestType() == RequestType.Main_Mem_Read)
		{
			if(nucaType == NucaType.NONE)
			{	
				event.getRequestingElement().getPort().put(
						event.update(
								eventQ,
								event.getRequestingElement().getLatency(),//2,//wire delay from main memory to cache
								null,
								event.getRequestingElement(),
								RequestType.Mem_Response));
			}
			else
			{

				NucaCacheBank requestingBank =  (NucaCacheBank) event.getRequestingElement();
				//System.out.println(((AddressCarryingEvent)event).getDestinationBankId() +" " + ((AddressCarryingEvent)event).getDestinationBankId());
				requestingBank.getRouter().getPort().put(
						event.update(
								eventQ,
								event.getRequestingElement().getLatencyDelay(),
								null,
								requestingBank.getRouter(),
								RequestType.Main_Mem_Response));
			}
		}
		else if (event.getRequestType() == RequestType.Main_Mem_Write)
		{
			//TODO : If we have to simulate the write timings also, then the code will come here
			//Just to tell the requesting things that the write is completed
			
//			Core.outstandingMemRequests--;
//			Core.outstandingMemRequests--;
		}
	}
}
