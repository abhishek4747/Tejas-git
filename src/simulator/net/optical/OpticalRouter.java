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

import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import config.NocConfig;

import memorysystem.AddressCarryingEvent;
import memorysystem.SignalWavelengthEvent;
import memorysystem.nuca.NucaCacheBank;
import net.NocInterface;
import net.Router;

public class OpticalRouter extends Router{
	
	public boolean readyToSend;
	public boolean readyToSendLocally;
	Vector<SignalWavelengthEvent> dataEvent;
	Vector<SignalWavelengthEvent> localDataEvent;
	AddressCarryingEvent tokenEvent;
	DataBus dataBus;
	TokenBus tokenBus;
	
	//TODO initialize these variables
	public OpticalRouter(NocConfig nocConfig, NocInterface bankReference) {
		super(nocConfig, bankReference);
		// TODO Auto-generated constructor stub
		this.readyToSend = false;
		this.dataEvent = new Vector<SignalWavelengthEvent>();
		this.localDataEvent = new Vector<SignalWavelengthEvent>();
	}

	public int calculateWavelength(Vector<Integer> destBank)
	{
		return(this.numberOfRows * destBank.elementAt(1) + destBank.elementAt(0));
	}
	
	synchronized public boolean CheckBuffer()
	{
		if(this.availBuff == 0)
			return false;
		else{
			this.availBuff --;
			return true;
		}
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event){
//		RequestType reqType = event.getRequestType();
//		if(reqType == RequestType.TOKEN ){
//			while(dataEvent.size() > 0 ){
//				dataBus.getPort().put(dataEvent.elementAt(0).
//						update(eventQ,
//								1, 
//								this, 
//								this.dataBus, 
//								dataEvent.elementAt(0).getRequestType()));
//				dataEvent.remove(0);
//			}
//			dataEvent.clear();
//			tokenBus.getPort().put(event.
//						update(eventQ,
//								1, 
//								this, 
//								this.tokenBus, 
//								RequestType.TOKEN));
//			readyToSend = false;
//		}
//		else if(reqType == RequestType.LOCAL_TOKEN){
//			while(localDataEvent.size() > 0 ){
//				dataBus.getPort().put(this.localDataEvent.elementAt(0).
//						update(eventQ,
//								1, 
//								this, 
//								this.dataBus, 
//								this.localDataEvent.elementAt(0).getRequestType()));
//				this.localDataEvent.remove(0);
//			}
//			this.localDataEvent.clear();
//
//			tokenBus.getPort().put(event.
//						update(eventQ,
//								1, 
//								this, 
//								this.tokenBus, 
//								RequestType.LOCAL_TOKEN));
//			readyToSendLocally = false;
//		}
//		else
//		{//TODO  incoming data directly send to bank
//		 //		 outgoing data kept in buffer
//			if(reqType == RequestType.Mem_Response ||
//					reqType == RequestType.Main_Mem_Read ||
//					reqType == RequestType.Main_Mem_Write){
//				//System.out.println("Optical router to dataEvent "+  reqType + " "+ ((AddressCarryingEvent) event).getSourceBankId()+ " " +((AddressCarryingEvent) event).getDestinationBankId());
//				readyToSend = true;
//				
//				/*SignalWavelengthEvent WaveEvent = new SignalWavelengthEvent
//								(eventQ, 
//								0, 
//								event.getRequestingElement(),
//								event.getProcessingElement(),
//								reqType, 
//								((AddressCarryingEvent)event).getAddress(),
//								-1,
//								((AddressCarryingEvent)event).getSourceBankId(),
//								((AddressCarryingEvent)event).getDestinationBankId(),
//								((AddressCarryingEvent)event).coreId,
//								((AddressCarryingEvent)event).oldSourceBankId,
//								((AddressCarryingEvent)event).oldRequestingElement,
//								((AddressCarryingEvent)event).requestingElementStack,
//								((AddressCarryingEvent)event).requestTypeStack);
//				this.dataEvent.add(WaveEvent);*/
//
//			}
//
//			else if(((AddressCarryingEvent)event).getRequestingElement().equals(this.reference)){
//				if(((AddressCarryingEvent) event).getDestinationId().elementAt(1) 
//								   == this.reference.getId().elementAt(1))
//				{
//					readyToSendLocally =true;
//					/*SignalWavelengthEvent WaveEvent = new SignalWavelengthEvent
//							(eventQ, 
//							0, 
//							event.getRequestingElement(),
//							event.getProcessingElement(),
//							reqType, 
//							((AddressCarryingEvent)event).getAddress(),
//							calculateWavelength(((AddressCarryingEvent)event).getDestinationBankId()),
//							((AddressCarryingEvent)event).getSourceBankId(),
//							((AddressCarryingEvent)event).getDestinationBankId(),
//							((AddressCarryingEvent)event).coreId,
//							((AddressCarryingEvent)event).oldSourceBankId,
//							((AddressCarryingEvent)event).oldRequestingElement,
//							((AddressCarryingEvent)event).requestingElementStack,
//							((AddressCarryingEvent)event).requestTypeStack);
//					this.localDataEvent.add(WaveEvent);*/
//				}
//				else {
//					readyToSend = true;
//					/*SignalWavelengthEvent WaveEvent = new SignalWavelengthEvent
//							(eventQ, 
//							0, 
//							event.getRequestingElement(),
//							event.getProcessingElement(),
//							reqType, 
//							((AddressCarryingEvent)event).getAddress(),
//							calculateWavelength(((AddressCarryingEvent)event).getDestinationBankId()),
//							((AddressCarryingEvent)event).getSourceBankId(),
//							((AddressCarryingEvent)event).getDestinationBankId(),
//							((AddressCarryingEvent)event).coreId,
//							((AddressCarryingEvent)event).oldSourceBankId,
//							((AddressCarryingEvent)event).oldRequestingElement,
//							((AddressCarryingEvent)event).requestingElementStack,
//							((AddressCarryingEvent)event).requestTypeStack);
//					this.dataEvent.add(WaveEvent);*/
//				}
//			}
//			else if(this.reference.getId().equals(((AddressCarryingEvent)event).getDestinationId()))
//			{
//				//System.out.println("Event to Bank " + "from" + ((AddressCarryingEvent)event).getSourceBankId() + "to" + ((AddressCarryingEvent)event).getDestinationBankId() + "with address" + ((AddressCarryingEvent)event).getAddress());
//				this.reference.getPort().put(event.
//						update(eventQ,
//								1,
//								this,
//								this.reference.getSimulationElement(),
//								reqType));
//			}
//		}
	}
}
