/*****************************************************************************
				Tejas Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
------------------------------------------------------------------------------------------------------------

	Contributors:  Eldhose Peter
*****************************************************************************/

package net.optical;

import java.util.Vector;

import memorysystem.AddressCarryingEvent;
import memorysystem.MemorySystem;
import memorysystem.SignalWavelengthEvent;

import config.NocConfig;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class EntryPoint extends SimulationElement{

	Vector<SignalWavelengthEvent> dataEvent;
	TopDataBus dataBus;
	TopLevelTokenBus tokenBus;
	Vector<BroadcastBus> broadcastBus;
	public EntryPoint(NocConfig nocConfig, TopDataBus dBus, TopLevelTokenBus tBus, Vector<BroadcastBus> bBus) {
		super(nocConfig.portType, nocConfig.getAccessPorts(), nocConfig.getPortOccupancy(), 
				nocConfig.getLatency(), nocConfig.operatingFreq);
		
		this.dataBus = dBus;
		this.tokenBus = tBus;
		this.broadcastBus = bBus;
		dataEvent = new Vector<SignalWavelengthEvent>();

	}
	
	public int findWavelength(Vector<Integer> destBank)
	{
		return(this.dataBus.lowLevelData.elementAt(0).totalBanks * destBank.elementAt(1) + destBank.elementAt(0));
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
				//System.out.println("entry Point to L1 Mem_Response  " + ((AddressCarryingEvent) event).getSourceBankId()+ " " +((AddressCarryingEvent) event).getDestinationBankId());
//				((AddressCarryingEvent)event).oldRequestingElement.getPort().put(event.
//						update(eventQ,
//							  ((AddressCarryingEvent)event).oldRequestingElement.getLatencyDelay() , 
//							  this,
//							  ((AddressCarryingEvent)event).oldRequestingElement,
//							  RequestType.Mem_Response));

			}

			else if(requestType == RequestType.Cache_Read ||
					requestType == RequestType.Cache_Read_from_iCache ||
					requestType == RequestType.Cache_Write ){
				Vector<Integer> bankId = ((AddressCarryingEvent)event).getDestinationId();
//				SignalWavelengthEvent WaveEvent = new SignalWavelengthEvent
//						(eventQ, 
//						0, 
//						event.getRequestingElement(),
//						event.getProcessingElement(),
//						requestType, 
//						((AddressCarryingEvent)event).getAddress(),
//						findWavelength(bankId),
//						((AddressCarryingEvent)event).getSourceBankId(),
//						bankId,
//						((AddressCarryingEvent)event).coreId,
//						((AddressCarryingEvent)event).oldSourceBankId,
//						((AddressCarryingEvent)event).oldRequestingElement,
//						((AddressCarryingEvent)event).requestingElementStack,
//						((AddressCarryingEvent)event).requestTypeStack);
//				broadcastBus.elementAt(bankId.elementAt(1)).getPort().put(WaveEvent.
//						update(eventQ,
//								1, 
//								this, 
//								this.broadcastBus.elementAt(bankId.elementAt(1)), 
//								requestType));
			}
			else if( requestType == RequestType.Main_Mem_Read ||
					 requestType == RequestType.Main_Mem_Write )
			{
				//System.out.println("Event to main memory from entry point   " + event.getRequestType());
				MemorySystem.mainMemoryController.getPort().put(event.update(eventQ, 
																   MemorySystem.mainMemoryController.getLatencyDelay(), 
																   this, 
																   MemorySystem.mainMemoryController, 
																   requestType));	
			}
			else{
				//System.out.println("put the event to dataEvent from entrypoint " + event.getRequestType());
//				if(requestType == RequestType.Main_Mem_Response)
//					((SignalWavelengthEvent)event).setWavelength(findWavelength(((AddressCarryingEvent)event).getDestinationBankId()));

				//commented
//				SignalWavelengthEvent WaveEvent = new SignalWavelengthEvent
//						(eventQ, 
//						0, 
//						event.getRequestingElement(),
//						event.getProcessingElement(),
//						requestType, 
//						((AddressCarryingEvent)event).getAddress(),
//						findWavelength(((AddressCarryingEvent)event).getDestinationBankId()),
//						((AddressCarryingEvent)event).getSourceBankId(),
//						((AddressCarryingEvent)event).getDestinationBankId(),
//						((AddressCarryingEvent)event).coreId,
//						((AddressCarryingEvent)event).oldSourceBankId,
//						((AddressCarryingEvent)event).oldRequestingElement,
//						((AddressCarryingEvent)event).requestingElementStack,
//						((AddressCarryingEvent)event).requestTypeStack);
//				dataEvent.add(WaveEvent);
				//till here
				
				
//				dataEvent.add((AddressCarryingEvent) event);
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
