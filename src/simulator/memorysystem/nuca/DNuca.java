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
		//System.out.println("Dnuca ");
		for(int i=0;i<2;i++)
		{
			for(int j=0;j<cacheColumns;j++)
			{
				if(i==0)
					cacheBank[i][j].isFirstLevel = true;
				if(i==1)
				{
					cacheBank[cacheRows-1][j].isLastLevel = true; 
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
		//System.out.println(sourceBankId + " " + destinationBankId + " " + getBankNumber(address));
		//System.exit(0);
		((AddressCarryingEvent)event).oldRequestingElement = (SimulationElement) event.getRequestingElement().clone();
		cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter().
								getPort().put(((AddressCarryingEvent)event).
														updateEvent(eventQ, 
																	0,//to be  changed to some constant(wire delay) 
																	requestingElement, 
																	cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter(), 
																	event.getRequestType(), 
																	sourceBankId, 
																	destinationBankId));
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

	double getDistance(int x1,int y1,int x2,int y2)
	{
		return Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
	}
	
	@Override
	public Vector<Integer> getNearestBankId(long addr, int coreId)
	{
		Vector<Integer> destinationBankId = getDestinationBankId(addr);
		Vector<Integer> nearestBankId = new Vector<Integer>();
		nearestBankId.add(coreCacheMapping[coreId][0]/cacheColumns);
		nearestBankId.add(coreCacheMapping[coreId][0]%cacheColumns);
		//System.out.println(coreCacheMapping[coreId][0] + "core");
		double distance = getDistance(coreCacheMapping[coreId][0]/cacheColumns,
				                      coreCacheMapping[coreId][0]%cacheColumns,
				                      destinationBankId.get(0),
				                      destinationBankId.get(1));
		for(int i=1;i < coreCacheMapping[coreId][1];i++)
		{
			double newDistance = getDistance((coreCacheMapping[coreId][0]+i)/cacheColumns,
						                    (coreCacheMapping[coreId][0]+i)%cacheColumns,
						                    destinationBankId.get(0),
						                    destinationBankId.get(1));
			//System.out.println(distance + "distance" + newDistance + "new distance");
			if(newDistance < distance)
			{
				distance = newDistance;
				nearestBankId.clear();
				nearestBankId.add((coreCacheMapping[coreId][0]+i)/cacheColumns);
				nearestBankId.add((coreCacheMapping[coreId][0]+i)%cacheColumns);
			}
		}
		return nearestBankId;
	}
}