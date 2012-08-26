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
import generic.RequestType;
import generic.SimulationElement;

import java.util.Vector;

import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import misc.Util;
import config.CacheConfig;
import net.NOC;
import config.SystemConfig;

public abstract class NucaCache extends Cache
{
	public enum NucaType{
		S_NUCA,
		D_NUCA,
		CB_S_NUCA,
		CB_D_NUCA,
		NONE
	}
	
	public enum Mapping {
		SET_ASSOCIATIVE,
		ADDRESS,
		BOTH
	}
    /*cache is assumed to in the form of a 2 dimensional array*/
    public NucaCacheBank cacheBank[][];
    int cacheRows;
    int cacheColumns;
    int numOfCores;// number of cores present on system
    int cacheSize;//cache size in bytes
    int associativity;
    int blockSizeBits;
    public NOC noc;
    public Mapping mapping;
    public int coreCacheMapping[][];
    public Vector<Vector<Vector<Integer>>> cacheMapping;
    NucaCache(CacheConfig cacheParameters, CoreMemorySystem containingMemSys)
    {
    	super(cacheParameters, containingMemSys);
    	this.cacheRows = cacheParameters.getNumberOfBankRows();
        this.cacheColumns = cacheParameters.getNumberOfBankColumns();
        this.numOfCores = SystemConfig.NoOfCores;
        this.cacheSize = cacheParameters.getSize();
        this.associativity = cacheParameters.getAssoc();
        this.blockSizeBits = Util.logbase2(cacheParameters.getBlockSize());
        coreCacheMapping = SystemConfig.coreCacheMapping.clone();
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
        initCacheMapping();
        makeCacheBanks(cacheParameters, containingMemSys);
    }

    Vector<Integer> sort(int coreId,Vector<Integer> list)
    {
    	int x1 = (coreId*2)/cacheRows;
    	int y1 = (coreId*2)%cacheRows;
    	Vector<Double> distance = new Vector<Double>();
    	for(int i=0;i<list.size();i++)
    	{
        	int x2 = list.get(i)/cacheRows;
        	int y2 = list.get(i)%cacheRows;
        	int x = (x1-x2)*(x1-x2);
        	int y = (y1-y2)*(y1-y2);
        	distance.add(Math.sqrt(x+y));
    	}
    	for(int i=0;i<distance.size();i++)
    	{
    		int index = i;
    		double val = distance.get(i);
    		for(int j=i+1;j<distance.size();j++)
    		{
    			if( val > distance.get(j))
    			{
    				index = j;
    				val = distance.get(j);
    			}
    		}
    		if(index != i){
	    		double temp = distance.get(i);
				double temp1 = distance.get(index);
				int t = list.get(i);
				int t1 = list.get(index);
				distance.remove(i);
				distance.add(i, temp1);
				distance.remove(index);
				distance.add(index, temp);
				list.remove(i);
				list.add(i, t1);
				list.remove(index);
				list.add(index, t);
    		}
    	}
    	
    	return list;
    }
    
    
    void fillCacheMapping()
    {
    	int numOfCores = cacheRows*cacheColumns/2;
    	for(int i=0;i<numOfCores;i++)
    	{
    		for(int j=0;j<cacheColumns;j++)
    		{
    			for(int k=0;k<cacheRows;k++)
    			{
    				if(j%2==0)
    				{
    					if(k%2 == 1)
    					{
    						cacheMapping.get(i).get(j).add(k*cacheColumns + j);
    					}
    					else
    						continue;
    				}
    				else
    				{
    					if(k%2 == 0)
    					{
    						cacheMapping.get(i).get(j).add(k*cacheColumns + j);
    					}
    					else
    						continue;
    				}
    			}
    		}
    	}
    }
    
    void initCacheMapping()
    {
    	fillCacheMapping();
    	int numOfCores = cacheRows*cacheColumns/2;
    	for(int i=0;i<numOfCores;i++)
    	{
    		for(int j=0;j<cacheColumns;j++)
    		{
    			Vector<Integer> set= cacheMapping.get(i).remove(j);
    			cacheMapping.get(i).add(j,sort(i,set));
    		}
    	}
    	Vector<Vector<Integer>> temp = new Vector<Vector<Integer>>(cacheMapping.get(0));
    	cacheMapping.add(0,temp);
    }
    
    private void makeCacheBanks(CacheConfig cacheParameters,CoreMemorySystem containingMemSys) 
    {
		int bankColumns,bankRows,i,j;
		
		bankColumns = cacheParameters.getNumberOfBankColumns();  
		bankRows = cacheParameters.getNumberOfBankRows(); 
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
		noc.ConnectBanks(cacheBank,bankRows,bankColumns,cacheParameters.nocConfig);
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
	
	public void setStatistics()
	{
		for(int i=0;i<cacheRows;i++)
		{
			for(int j=0;j<cacheColumns;j++)
			{
				noOfRequests = noOfRequests + cacheBank[i][j].noOfRequests;
				hits = hits + cacheBank[i][j].hits;
				misses = misses + cacheBank[i][j].misses;
			}			
		}
	}
	
	public Vector<Integer> getSourceBankId(long addr,int coreId)
	{
		Vector<Integer> bankId = new Vector<Integer>();
		bankId.add(coreId/cacheColumns);
		bankId.add(coreId%cacheColumns);
		return bankId;
	}
	
	abstract  Vector<Integer> getDestinationBankId(long address, int coreId);

	public boolean addEvent(AddressCarryingEvent addressEvent)
	{
		SimulationElement requestingElement = addressEvent.getRequestingElement();
		long address = addressEvent.getAddress();
		Vector<Integer> sourceBankId = getSourceBankId(address,addressEvent.coreId);
		Vector<Integer> destinationBankId = getDestinationBankId(address,addressEvent.coreId);
		addressEvent.oldRequestingElement = (SimulationElement) requestingElement.clone();
		addressEvent.setDestinationBankId(sourceBankId);
		addressEvent.setSourceBankId(destinationBankId);
		addressEvent.setProcessingElement(	this.cacheBank[destinationBankId.get(0)][destinationBankId.get(1)] );
		addressEvent.oldSourceBankId = (Vector<Integer>) sourceBankId.clone();
		if(missStatusHoldingRegister.isFull())
		{
			return false;
		}
				
		boolean entryCreated = missStatusHoldingRegister.addOutstandingRequest(addressEvent);
		if(entryCreated)
		{
			this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter().
			getPort().put(addressEvent.
									updateEvent(addressEvent.getEventQ(), 
												0,
												requestingElement, 
												this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter(), 
												addressEvent.getRequestType(), 
												sourceBankId, 
												destinationBankId));
		}
		return true;
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
		event.addEventTime(1);
		event.getEventQ().addEvent(event);
	}
}