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

package memorysystem;

import java.util.Stack;
import java.util.Vector;

import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class SignalWavelengthEvent  extends AddressCarryingEvent{

	private int wavelength;
	public SignalWavelengthEvent(EventQueue eventQ, 
			long eventTime, SimulationElement requestingElement, SimulationElement processingElement,
			RequestType requestType, long address, int wavelength, Vector<Integer> sourceBankId, 
			Vector<Integer> destinationBankId, int coreId, Vector<Integer> oldSourceBankId,
			SimulationElement oldReqElement, Stack<SimulationElement> simStack, Stack<RequestType> reqStack) {
		
		super(eventQ, eventTime, requestingElement, processingElement, requestType, address);
		
		this.wavelength = wavelength; 
		this.setSourceBankId(sourceBankId);
		this.setDestinationBankId(destinationBankId);
		this.oldSourceBankId = oldSourceBankId;
		this.coreId = coreId;
		this.oldRequestingElement = oldReqElement;
		this.requestingElementStack = simStack;
		this.requestTypeStack = reqStack;
		
	}
	
	public void setWavelength(int wavelength){
		this.wavelength = wavelength;
	}
	public int getWavelength(){
		return this.wavelength;
	}
}
