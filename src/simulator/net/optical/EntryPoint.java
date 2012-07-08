package net.optical;

import java.util.Vector;

import memorysystem.AddressCarryingEvent;
import memorysystem.MemorySystem;

import config.NocConfig;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class EntryPoint extends SimulationElement{

	Vector<AddressCarryingEvent> dataEvent;
	Vector<AddressCarryingEvent> bCastEvent;
	TopDataBus dataBus;
	TopLevelTokenBus tokenBus;
	Vector<BroadcastBus> broadcastBus;
	public EntryPoint(NocConfig nocConfig, TopDataBus dBus, TopLevelTokenBus tBus, Vector<BroadcastBus> bBus) {
		super(nocConfig.portType, nocConfig.getAccessPorts(), nocConfig.getPortOccupancy(), 
				nocConfig.getLatency(), nocConfig.operatingFreq);
		
		this.dataBus = dBus;
		this.tokenBus = tBus;
		this.broadcastBus = bBus;
		dataEvent = new Vector<AddressCarryingEvent>();

	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {

		RequestType requestType = event.getRequestType();
		
		if(requestType == RequestType.TOKEN)
		{
			while(dataEvent.size() > 0 ){
				dataBus.getPort().put(dataEvent.elementAt(0).
						update(eventQ,
								1, 
								this, 
								this.dataBus, 
								(dataEvent.elementAt(0)).getRequestType()));
				dataEvent.remove(0);
			}
			tokenBus.getPort().put(event.
					update(eventQ,
							1, 
							this, 
							this.tokenBus, 
							requestType));
		}
		else
		{
			if(event.getRequestType() == RequestType.Mem_Response){
				System.out.println("entry Point to L1 Mem_Response  " + ((AddressCarryingEvent) event).getSourceBankId()+ " " +((AddressCarryingEvent) event).getDestinationBankId());
				((AddressCarryingEvent)event).oldRequestingElement.getPort().put
						(event.update
							(eventQ, 
							((AddressCarryingEvent)event).oldRequestingElement.getLatencyDelay() , 
							 this,
							 ((AddressCarryingEvent)event).oldRequestingElement,
							 RequestType.Mem_Response));

			}

			else if(requestType == RequestType.Cache_Read ||
					requestType == RequestType.Cache_Read_from_iCache ||
					requestType == RequestType.Cache_Write ){
				Vector<Integer> bankId = ((AddressCarryingEvent)event).getDestinationBankId();
				broadcastBus.elementAt(bankId.elementAt(1)).getPort().put(event.
						update(eventQ,
								1, 
								this, 
								this.broadcastBus.elementAt(bankId.elementAt(1)), 
								requestType));
			}
			else if( requestType == RequestType.Main_Mem_Read ||
					 requestType == RequestType.Main_Mem_Write )
			{
				System.out.println("Event to main memory from entry point   " + event.getRequestType());
				MemorySystem.mainMemory.getPort().put(event.update(eventQ, 
																   MemorySystem.mainMemory.getLatencyDelay(), 
																   this, 
																   MemorySystem.mainMemory, 
																   requestType));	
			}
			else{
				System.out.println("put the event to dataEvent from entrypoint " + event.getRequestType());
				dataEvent.add((AddressCarryingEvent) event);
//				dataBus.getPort().put(event.
//						update(eventQ,
//								1, 
//								this, 
//								this.dataBus, 
//								requestType));
			}
		}
	}
}
