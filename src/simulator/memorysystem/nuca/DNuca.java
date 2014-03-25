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

				Contributor: Anuj Arora
*****************************************************************************/
package memorysystem.nuca;

import java.util.HashMap;
import java.util.Vector;

import main.ArchitecturalComponent;
import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import misc.Util;
import net.NOC.CONNECTIONTYPE;
import net.optical.TopLevelTokenBus;
import config.CacheConfig;
import config.SystemConfig;

public class DNuca extends NucaCache{

	int numOfBankSets;
	Vector<Integer> bankSetnum;
	int bankSetBits;
	int numBanksinBankSet;
	static long eventId;
	HashMap<Integer,Vector<Vector<Integer>>> bankSetNumToBankIds;
	public DNuca(CacheConfig cacheParameters,
			CoreMemorySystem containingMemSys, TopLevelTokenBus tokenbus,
			NucaType nucaType) 
	{
		super(cacheParameters, containingMemSys, tokenbus, nucaType);
		bankSetNumToBankIds = new HashMap<Integer,Vector<Vector<Integer>>>();
		bankSetnum = new Vector<Integer>();
		makeCacheBanks(cacheParameters, containingMemSys, tokenbus,nucaType,this);
		makeBankSets();
		bankSetBits = Util.logbase2(numOfBankSets);
	}
	protected void makeCacheBanks(CacheConfig cacheParameters,CoreMemorySystem containingMemSys, TopLevelTokenBus tokenBus, NucaType nucaType, DNuca nucaCache)
   	{
       	int rows = SystemConfig.nocConfig.getNumberOfBankRows();
       	int cols = SystemConfig.nocConfig.getNumberOfBankColumns();
   		for(int i=0;i<rows;i++)
   		{
   			for(int j=0;j<cols;j++)
   			{
   				if(SystemConfig.nocConfig.nocElements.nocElementsLocations.get(i).get(j).equals("0"))
   				{
   					Vector<Integer> bankId = new Vector<Integer>();
   					bankId.add(i);
   					bankId.add(j);
   					cacheBank.add(new DNucaBank(bankId, cacheParameters, containingMemSys, this, nucaType));
   				}
   			}
   		}
   	}
	void makeBankSets()
	{
		int rows = SystemConfig.nocConfig.getNumberOfBankRows();
		
       	int cols = SystemConfig.nocConfig.getNumberOfBankColumns();
   		for(int i=0;i<rows;i++)
   		{
   			for(int j=0;j<cols;j++)
   			{
   				if(SystemConfig.nocConfig.nocElements.nocElementsLocations.get(i).get(j).equals("0"))
   				{
   					Vector<Integer> bankId = new Vector<Integer>();
   					bankId.add(i);
   					bankId.add(j);
   					if(bankSetNumToBankIds.get(i)==null)
   					{
   						numOfBankSets++;
   						bankSetnum.add(i);
   						Vector<Vector<Integer>> temp = new Vector<Vector<Integer>>();
   						temp.add(bankId);
   						bankSetNumToBankIds.put(i,temp);
   						
   					}
   					else
   					{
   						Vector<Vector<Integer>> temp=bankSetNumToBankIds.get(i);
   						temp.add(bankId);
   						bankSetNumToBankIds.put(i,temp);
   					}
   					for(NucaCacheBank nuca:cacheBank)
					{
						if(nuca.getBankId().equals(bankId))
						{
							bankIdtoNucaCacheBank.put(bankId, nuca);
						}
					}
   				}
   			}
   		}
   		numBanksinBankSet= bankSetNumToBankIds.get(bankSetnum.get(0)).size();
	}
	void putEventToRouter(AddressCarryingEvent addrEvent)
	{
		long address = addrEvent.getAddress();
		Vector<Integer> sourceId = getCoreId(addrEvent.coreId);
		int bankSet = getBankSetId(address);
		Vector<Integer> destinationId = getNearestBank(bankSet, sourceId);//getBankInBankSet(bankSet, address);
		if(accessedBankIds.get(destinationId)==null)
			accessedBankIds.put(destinationId,1);
		else
			accessedBankIds.put(destinationId,accessedBankIds.get(destinationId)+1);
		AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(addrEvent.getEventQ(),
											 0,ArchitecturalComponent.getCores()[addrEvent.coreId], 
											 ArchitecturalComponent.getCores()[addrEvent.coreId].getRouter(),
											 addrEvent.getRequestType(),
											 address,addrEvent.coreId,
											 sourceId,destinationId);
		if(SystemConfig.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL) 
		{
			ArchitecturalComponent.getCores()[addrEvent.coreId].getRouter().
			getPort().put(eventToBeSent);
		}
	}
	Vector<Integer> getBankInBankSet(int bankSet,long addr) 
	{
		int banknuminset =-1;
		if(mapping == Mapping.SET_ASSOCIATIVE) 
		{
			long tag = (addr>>>(numSetsBits+blockSizeBits));
			banknuminset = (int) (tag & (numBanksinBankSet-1));
			
		}
		else if(mapping == Mapping.ADDRESS)
		{
			long tag = (addr>>>(numLinesBits+blockSizeBits));
			banknuminset = (int) (tag & (numBanksinBankSet-1));
		}
		else
		{
			misc.Error.showErrorAndExit("Invalid Type of Mapping!!!");
		}
		return bankSetNumToBankIds.get(bankSetnum.get(bankSet)).get(banknuminset);
	}
	int getBankSetId(long addr)
	{
		if(mapping == Mapping.SET_ASSOCIATIVE) 
		{
			long tag = (addr>>>(numSetsBits+blockSizeBits));
			long bankNum = tag & (getNumOfBanks()-1);
			int bankSet = (int) (bankNum/numBanksinBankSet);
			return bankSet;
		}
		else if(mapping == Mapping.ADDRESS)
		{
			long tag = (addr>>>(numLinesBits+blockSizeBits));
			long bankNum = tag & (getNumOfBanks()-1);
			int bankSet = (int) (bankNum/numBanksinBankSet);
			return bankSet;
		}
		else
		{
			misc.Error.showErrorAndExit("Invalid Type of Mapping!!!");
			return 0;
		}
	}
	Vector<Integer> getNearestBank(int bankSet,Vector<Integer> coreId)
	{
		Vector<Vector<Integer>> bankIds = bankSetNumToBankIds.get(bankSetnum.get(bankSet));
		Vector<Integer> nearestBankId=null;
		int min=Integer.MAX_VALUE;
		for(Vector<Integer> bankId:bankIds)
		{
			int dist = (coreId.get(0) - bankId.get(0))*(coreId.get(0) - bankId.get(0)) + 
					   (coreId.get(1) - bankId.get(1))*(coreId.get(1) - bankId.get(1)) ;
			if(dist<min)
			{
				min=dist;
				nearestBankId = bankId;
			}
		}
		return nearestBankId;
	}
	int getNumOfBankSets() {
		return numOfBankSets;
	}
}
