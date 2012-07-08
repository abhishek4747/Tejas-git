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
	public OpticalRouter(NocConfig nocConfig, NucaCacheBank bankReference) {
		super(nocConfig, bankReference);
		// TODO Auto-generated constructor stub
		this.readyToSend = false;
		this.dataEvent = new Vector<SignalWavelengthEvent>();
		this.localDataEvent = new Vector<SignalWavelengthEvent>();
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event){
		RequestType reqType = event.getRequestType();
		if(reqType == RequestType.TOKEN ){
			while(dataEvent.size() > 0 ){
				dataBus.getPort().put(dataEvent.elementAt(0).
						update(eventQ,
								1, 
								this, 
								this.dataBus, 
								dataEvent.elementAt(0).getRequestType()));
				dataEvent.remove(0);
			}
			dataEvent.clear();
			tokenBus.getPort().put(event.
						update(eventQ,
								1, 
								this, 
								this.tokenBus, 
								RequestType.TOKEN));
			readyToSend = false;
		}
		else if(reqType == RequestType.LOCAL_TOKEN){
			while(localDataEvent.size() > 0 ){
				dataBus.getPort().put(this.localDataEvent.elementAt(0).
						update(eventQ,
								1, 
								this, 
								this.dataBus, 
								this.localDataEvent.elementAt(0).getRequestType()));
				this.localDataEvent.remove(0);
			}
			this.localDataEvent.clear();

			tokenBus.getPort().put(event.
						update(eventQ,
								1, 
								this, 
								this.tokenBus, 
								RequestType.LOCAL_TOKEN));
			readyToSendLocally = false;
		}
		else
		{//TODO  incoming data directly send to bank
		 //		 outgoing data kept in buffer
			if(reqType == RequestType.Mem_Response ||
					reqType == RequestType.Main_Mem_Read ||
					reqType == RequestType.Main_Mem_Write){
				//System.out.println("Optical router to dataEvent "+  reqType + " "+ ((AddressCarryingEvent) event).getSourceBankId()+ " " +((AddressCarryingEvent) event).getDestinationBankId());
				readyToSend = true;
				
				SignalWavelengthEvent WaveEvent = new SignalWavelengthEvent
								(eventQ, 
								0, 
								event.getRequestingElement(),
								event.getProcessingElement(),
								reqType, 
								((AddressCarryingEvent)event).getAddress(), -1);
				this.dataEvent.add(WaveEvent);

			}

			else if(((AddressCarryingEvent)event).getRequestingElement().equals(this.bankReference)){
				if(((AddressCarryingEvent) event).getDestinationBankId().elementAt(1) 
								   == this.bankReference.getBankId().elementAt(1))
				{
					readyToSendLocally =true;
					this.localDataEvent.add((SignalWavelengthEvent) event);
				}
				else {
					readyToSend = true;
					this.dataEvent.add((SignalWavelengthEvent) event);
				}
			}
			else if(this.bankReference.getBankId().equals(((AddressCarryingEvent)event).getDestinationBankId()))
			{
				//System.out.println("Event to Bank " + "from" + ((AddressCarryingEvent)event).getSourceBankId() + "to" + ((AddressCarryingEvent)event).getDestinationBankId() + "with address" + ((AddressCarryingEvent)event).getAddress());
				this.bankReference.getPort().put(event.
						update(eventQ,
								1,
								this,
								this.bankReference,
								reqType));
			}
			/*else if(reqType == RequestType.Main_Mem_Read || reqType == RequestType.Main_Mem_Response ||
					reqType == RequestType.Main_Mem_Write || reqType == RequestType.Mem_Response ||
					reqType == RequestType.Cache_Read || reqType == RequestType.Cache_Read_from_iCache)
			{
				this.bankReference.getPort().put(event.update(
						eventQ,
						1,	//this.getLatency()
						this, 
						this.bankReference,
						reqType));
			}*/
		}
	}
}
