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
import generic.OMREntry;
import generic.RequestType;
import generic.SimulationElement;

import java.util.ArrayList;
import java.util.Vector;

import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CacheLine;
import memorysystem.CoreMemorySystem;
import memorysystem.MESI;
import memorysystem.MemorySystem;
import memorysystem.MissStatusHoldingRegister;
import memorysystem.Mode1MSHR;
import memorysystem.Cache.CoherenceType;
import misc.Util;
import config.CacheConfig;
import config.SimulationConfig;
import net.NOC;
import net.NOC.CONNECTIONTYPE;
import net.optical.OpticalNOC;
import net.optical.TopLevelTokenBus;
import config.SystemConfig;

public class NucaCache extends Cache
{
	public enum NucaType{
		S_NUCA,
		D_NUCA,
		NONE, CB_D_NUCA
	}
	
	public enum Mapping {
		SET_ASSOCIATIVE,
		ADDRESS,
		BOTH
	}
    /*cache is assumed to in the form of a 2 dimensional array*/
    public NucaCacheBank cacheBank[][];
    public int cacheRows;
    public int cacheColumns;
    int numOfCores;// number of cores present on system
    int cacheSize;//cache size in bytes
    int associativity;
    int blockSizeBits;
    public NOC noc;
    public Mapping mapping;
    public Vector<Vector<Vector<Integer>>> cacheMapping;
    public NucaCache(CacheConfig cacheParameters, CoreMemorySystem containingMemSys, TopLevelTokenBus tokenbus)
    {
    	super(cacheParameters, containingMemSys);
    	this.nucaType = SimulationConfig.nucaType;
    	this.cacheRows = cacheParameters.getNumberOfBankRows();
        this.cacheColumns = cacheParameters.getNumberOfBankColumns();
        this.numOfCores = SystemConfig.NoOfCores;
        this.cacheSize = cacheParameters.getSize();
        this.associativity = cacheParameters.getAssoc();
        this.blockSizeBits = Util.logbase2(cacheParameters.getBlockSize());
        this.mapping = cacheParameters.mapping;
        cacheMapping = new Vector<Vector<Vector<Integer>>>();
        for(int i=0;i<SystemConfig.NoOfCores;i++)
        {
        	Vector<Vector<Integer>> temp = new Vector<Vector<Integer>>();
        	cacheMapping.add(temp);
        	for(int j=0;j<cacheColumns;j++)
        	{
	        	Vector<Integer> temp1 = new Vector<Integer>();
	        	cacheMapping.get(i).add(temp1);
        	}
        }
        noc = new NOC();
        missStatusHoldingRegister = new Mode1MSHR(40000);
        makeCacheBanks(cacheParameters, containingMemSys, tokenbus);
        for(int i=0;i<2;i++)
		{
			for(int j=0;j<cacheColumns;j++)
			{
				if(i==0)
					cacheBank[cacheRows/2][j].isFirstLevel = true;
				if(i==1)
				{
					cacheBank[cacheRows-1][j].isLastLevel = true; 
				}
			}
		}
    }
    
    private void makeCacheBanks(CacheConfig cacheParameters,CoreMemorySystem containingMemSys, TopLevelTokenBus tokenBus)
	{
		int bankColumns,bankRows,i,j;
		
		bankColumns = cacheParameters.getNumberOfBankColumns();  //number banks should be power of 2 otherwise truncated
		bankRows = cacheParameters.getNumberOfBankRows();  //number banks should be power of 2 otherwise truncated
		this.cacheBank = new NucaCacheBank[bankRows][bankColumns];
		for(i=0;i<bankRows;i++)
		{
			for(j=0;j<bankColumns;j++)
			{
				Vector<Integer> bankId = new Vector<Integer>(2);
				bankId.clear();
				bankId.add(i);
				bankId.add(j);
				this.cacheBank[i][j] = new NucaCacheBank(bankId,cacheParameters,containingMemSys,this);
			}
		}
	    if(cacheParameters.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL)
        	noc = new NOC();
        else
        	noc = new OpticalNOC();
		noc.ConnectBanks(cacheBank,bankRows,bankColumns,cacheParameters.nocConfig,tokenBus);
	}
 
    public boolean addEvent(AddressCarryingEvent addrEvent)
	{
		if(missStatusHoldingRegister.isFull())
		{
			return false;
		}
		boolean entryCreated = missStatusHoldingRegister.addOutstandingRequest(addrEvent);
		if(entryCreated)
		{
			putEventToRouter(addrEvent);
		}
		return true;
	}
    
    void putEventToRouter(AddressCarryingEvent addrEvent)
	{
		long address = addrEvent.getAddress();
		Vector<Integer> sourceBankId = getSourceBankId(address,addrEvent.coreId);
		Vector<Integer> destinationBankId = getDestinationBankId(address,addrEvent.coreId);
		AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(addrEvent.getEventQ(),
																								0,this, this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter(), 
																								addrEvent.getRequestType(), address,addrEvent.coreId,
																								sourceBankId,destinationBankId);
		//eventToBeSent.oldSourceBankId = new Vector<Integer>(sourceBankId);
		if(this.cacheBank[0][0].cacheParameters.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL) 
		{
			this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter().
			getPort().put(eventToBeSent);
		}
		else
		{
			((OpticalNOC)this.noc).entryPoint.getPort().put(eventToBeSent);
		}
	}
    
    public int getBankNumber(long addr)
	{
		if(mapping == Mapping.SET_ASSOCIATIVE)
			return ((int)((addr>>>blockSizeBits)%getNumOfBanks())+getNumOfBanks());
		else if(mapping == Mapping.ADDRESS)
		{
			long tag = (addr>>>blockSizeBits);
			int bankNumBits = (int)(Math.log10(getNumOfBanks())/Math.log10(2));
			int tagSize = (int)(Math.log10(tag)/Math.log10(2));
			int bankId = (int)(tag >>> (tagSize-bankNumBits +1));
			return bankId;
		}else
		{
			long tag = (addr>>>blockSizeBits);
			int bankNumBits = (int)(Math.log10(getNumOfBanks())/Math.log10(2));
			int tagSize = (int)(Math.log10(tag)/Math.log10(2));
			int bankId = (int)(tag >>> (tagSize-bankNumBits +1));
			return bankId;
		}
	}

    int getSetIndex(long address)
    {
    	int bankNum = getBankNumber(address);
    	return (bankNum%cacheRows);
    }
    
    public Vector<Integer> integerToBankId(int bankNumber)
	{
		Vector<Integer> id = new Vector<Integer>(2);
		id.add((bankNumber/cacheColumns));
		id.add((bankNumber%cacheColumns));
		return id;
	}
	
	public int bankIdtoInteger(Vector<Integer> bankId)
	{
		int bankNumber = bankId.get(0)*cacheColumns + bankId.get(1);
		return bankNumber;
	}
	
	public int getNumOfBanks()
	{
		return cacheRows*cacheColumns/2;		
	}
	
	public Vector<Integer> getSourceBankId(long addr,int coreId)
	{
		Vector<Integer> bankId = new Vector<Integer>();
		bankId.add(coreId/cacheColumns);
		bankId.add(coreId%cacheColumns);
		return bankId;
	}
	
	public Vector<Integer> getDestinationBankId(long addr,int coreId)
	{
		Vector<Integer> destinationBankId = new Vector<Integer>();
		int bankNumber= getBankNumber(addr);
		if(SimulationConfig.nucaType == NucaType.D_NUCA)
		{
			destinationBankId.add(cacheRows/2);
		}
		else
		{
			destinationBankId.add(bankNumber /cacheColumns);
		}
		destinationBankId.add(bankNumber%cacheColumns);
		return destinationBankId;
	}
	
	
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
	   if(event.getRequestType() == RequestType.Mem_Response)
	    {
	    	handleMemResponse(eventQ,event);
	    }
	}
	
	
	protected void handleMemResponse(EventQueue eventQ, Event event)
	{
		ArrayList<Event> eventsToBeServed = missStatusHoldingRegister.removeRequestsIfAvailable((AddressCarryingEvent)event);
		sendResponseToWaitingEvent(eventsToBeServed);
	}
	
	
	protected void sendResponseToWaitingEvent(ArrayList<Event> outstandingRequestList)
	{
		while (!outstandingRequestList.isEmpty())
		{	
			AddressCarryingEvent eventPoppedOut = (AddressCarryingEvent) outstandingRequestList.remove(0); 
			if (eventPoppedOut.getRequestType() == RequestType.Cache_Read)
			{
				sendMemResponse(eventPoppedOut);
			}
			else if (eventPoppedOut.getRequestType() == RequestType.Cache_Write)
			{
				if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
				{
					 AddressCarryingEvent addrEvent = new AddressCarryingEvent(eventPoppedOut.getEventQ(),
							 																		MemorySystem.mainMemory.getLatency(),
							 																		this,
							 																		MemorySystem.mainMemory,
							 																		RequestType.Main_Mem_Write,
							 																		eventPoppedOut.coreId);
					 MemorySystem.mainMemory.getPort().put(addrEvent);
				}
			}
		}
	}				
}