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
import generic.Event;
import generic.EventQueue;
import generic.SimulationElement;
import java.util.Vector;
import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import config.CacheConfig;

public class DNuca extends NucaCache {

	public DNuca(CacheConfig cacheParameters, CoreMemorySystem containingMemSys) 
	{
		super(cacheParameters,containingMemSys);
		System.out.println("Dnuca ");
		for(int i=0;i<2;i++)
		{
			for(int j=0;j<cacheColumns;j++)
			{
				if(i==0)
					cacheBank[i][j].isFirstLevel = true;
				if(i==1)
				{
					cacheBank[cacheRows-1][j].isLastLevel = false; 
				}
			}
		}
	}

	@Override
	public long getTag(long addr) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		SimulationElement requestingElement = event.getRequestingElement();
		long address = ((AddressCarryingEvent)(event)).getAddress();
		Vector<Integer> sourceBankId = getSourceBankId(address);
		Vector<Integer> destinationBankId = getDestinationBankId(address);
		((AddressCarryingEvent)event).oldRequestingElement = (SimulationElement) event.getRequestingElement().clone();
		cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].
								getPort().put(((AddressCarryingEvent)event).
														updateEvent(eventQ, 
																	0,//to be  changed to some constant(wire delay) 
																	requestingElement, 
																	cacheBank[sourceBankId.get(0)][sourceBankId.get(1)], 
																	event.getRequestType(), 
																	sourceBankId, 
																	destinationBankId));
	}

	public int getBankNumber(long addr)
	{
		return (int)(addr>>>blockSizeBits)%getNumOfBanks();
	}
	
	public Vector<Integer> getDestinationBankId(long addr)
	{
		return getSourceBankId(addr);
	}
	
	public Vector<Integer> getSourceBankId(long addr)
	{
		Vector<Integer> bankId = new Vector<Integer>();
		int bankNumber = getBankNumber(addr);
		bankId.add(0);
		bankId.add(bankNumber%cacheColumns);
		return bankId;
	}
}