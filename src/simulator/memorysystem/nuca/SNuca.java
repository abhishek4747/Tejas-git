/*****************************************************************************
				BhartiSim Simulator
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

				Contributor: Mayur Harne
*****************************************************************************/

package memorysystem.nuca;
import java.util.Vector;

import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;
import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import memorysystem.MissStatusHoldingRegister;
import config.CacheConfig;

public class SNuca extends NucaCache
{
	public SNuca(CacheConfig cacheParameters, CoreMemorySystem containingMemSys) {
        super(cacheParameters,containingMemSys);
    }
		
	public Vector<Integer> getDestinationBankId(long addr)
	{
		Vector<Integer> bankId = new Vector<Integer>();
		int bankNumber = getBankNumber(addr);
		bankId.add(bankNumber/cacheColumns);
		bankId.add(bankNumber%cacheColumns);
		return bankId;
	}
	
	public Vector<Integer> getSourceBankId(long addr,int coreId)
	{
		Vector<Integer> bankId = new Vector<Integer>();
		//int bankNumber = getBankNumber(addr);
		bankId.add(coreId/cacheColumns);
		bankId.add(coreId%cacheColumns);
		return bankId;
	}
	
	double getDistance(int x1,int y1,int x2,int y2)
	{
		return Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
	}
	
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
	    if (event.getRequestType() == RequestType.PerformPulls)
		{
			for(int i=0;i<cacheRows;i++)
			{
				for(int j=0;j<cacheColumns;j++)
				{
					cacheBank[i][j].pullFromUpperMshrs();
				}
			}
		}
		//schedule pulling for the next cycle
			event.addEventTime(1);
			event.getEventQ().addEvent(event);
		}

	public boolean addEvent(AddressCarryingEvent addressEvent)
	{
		SimulationElement requestingElement = addressEvent.getRequestingElement();
		long address = addressEvent.getAddress();
		Vector<Integer> sourceBankId = getSourceBankId(address,addressEvent.coreId);
		Vector<Integer> destinationBankId = getDestinationBankId(address);
		addressEvent.oldRequestingElement = (SimulationElement) requestingElement.clone();
		MissStatusHoldingRegister destinationCacheBankMshr = this.cacheBank[destinationBankId.get(0)][destinationBankId.get(1)].missStatusHoldingRegister;
		addressEvent.setDestinationBankId(sourceBankId);
		addressEvent.setSourceBankId(destinationBankId);
		addressEvent.setProcessingElement(	this.cacheBank[destinationBankId.get(0)][destinationBankId.get(1)] );

		if(destinationCacheBankMshr.isFull())
		{
			return false;
		}
		
				
		boolean entryCreated = destinationCacheBankMshr.addOutstandingRequest(addressEvent);
		if(entryCreated)
		{
			this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter().
			getPort().put(addressEvent.
									updateEvent(addressEvent.getEventQ(), 
												0,//to be  changed to some constant(wire delay) 
												requestingElement, 
												this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter(), 
												addressEvent.getRequestType(), 
												sourceBankId, 
												destinationBankId));
		}
		return true;
	}
}
