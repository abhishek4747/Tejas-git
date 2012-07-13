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
import generic.SimulationElement;
import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import misc.Util;
import config.CacheConfig;
import config.SystemConfig;

public class SNuca extends NucaCache
{
	public SNuca(CacheConfig cacheParameters, CoreMemorySystem containingMemSys) {
        super(cacheParameters,containingMemSys);
    }
		
	public long getTag(long addr) {
		// TODO Auto-generated method stub
		long tag = (addr >>> (blockSizeBits +Util.logbase2(getNumOfBanks())));
		return tag;
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
	
	public Vector<Integer> getNearestBankId(long addr, int coreId)
	{
		Vector<Integer> destinationBankId = getDestinationBankId(addr);
		Vector<Integer> nearestBankId = new Vector<Integer>();
		nearestBankId.add(coreCacheMapping[coreId][0]/cacheColumns);
		nearestBankId.add(coreCacheMapping[coreId][1]%cacheColumns);
		double distance = getDistance(coreCacheMapping[coreId][0]/cacheColumns,
				                      coreCacheMapping[coreId][0]%cacheColumns,
				                      destinationBankId.get(0),
				                      destinationBankId.get(1));
		for(int i=1;i < coreCacheMapping[coreId].length;i++)
		{
			double newDistance = getDistance(coreCacheMapping[coreId][i]/cacheColumns,
						                    coreCacheMapping[coreId][i]%cacheColumns,
						                    destinationBankId.get(0),
						                    destinationBankId.get(1));
			if(newDistance < distance)
			{
				distance = newDistance;
				nearestBankId.clear();
				nearestBankId.add(coreCacheMapping[coreId][i]/cacheColumns);
				nearestBankId.add(coreCacheMapping[coreId][i]%cacheColumns);
			}

		}
		return nearestBankId;
	}
	
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		SimulationElement requestingElement = event.getRequestingElement();
		long address = ((AddressCarryingEvent)(event)).getAddress();
		Vector<Integer> sourceBankId = getSourceBankId(address,((AddressCarryingEvent)(event)).coreId);
		Vector<Integer> destinationBankId = getDestinationBankId(address);
		((AddressCarryingEvent)event).oldRequestingElement = (SimulationElement) event.getRequestingElement().clone();
		sourceBankId.clear();
		sourceBankId.add(0);
		sourceBankId.add(0);
		this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter().
										getPort().put(((AddressCarryingEvent)event).
																updateEvent(eventQ, 
																			0,//to be  changed to some constant(wire delay) 
																			requestingElement, 
																			this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter(), 
																			event.getRequestType(), 
																			sourceBankId, 
																			destinationBankId));

	}

}
