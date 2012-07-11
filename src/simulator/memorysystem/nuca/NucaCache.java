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
import generic.OMREntry;

import java.util.Enumeration;
import java.util.Vector;
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
        //this.cacheBank=new NucaCacheBank[cacheRows][cacheColumns/2];
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
        for(int i=0;i<cacheMapping.size();i++)
        {
        	System.out.println("Core " + i);
        	for(int j=0;j<cacheMapping.get(i).size();j++)
        	{
        		System.out.println(cacheMapping.get(i).get(j));
        	}
        }
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
    
    public void printMshrStatus(NucaCacheBank nucaCacheBank)
    {
    	//for(int i=0;i<cacheColumns;i++)
    	{
    		//for(int j=0;j<cacheRows;j++)
    		{
    			Enumeration<OMREntry> tempIte = nucaCacheBank.missStatusHoldingRegister.elements();
    			Enumeration<Long> tempIte1 = nucaCacheBank.missStatusHoldingRegister.keys();
    			System.out.println("bank id " + nucaCacheBank.getRouter().getBankId());
    			while(tempIte.hasMoreElements())
    			{
    				System.out.println("address "+ tempIte1.nextElement()  + "outstanding request size " + tempIte.nextElement().outStandingEvents.size());
    			}
    			System.out.println("\n \n");
    		}
    	}
    }
    
    public void printidTOBankMapping()
    {
    	for(int i=0;i< cacheRows ; i++)
    	{
    		for(int j=0;j<cacheColumns ; j++)
    		{
    			System.out.println("bank number ["+ i + "]["+j+"]");
    			System.out.println("bank id"+ cacheBank[i][j].getRouter().getBankId());
    		}
    	}
    }
    
    private void makeCacheBanks(CacheConfig cacheParameters,CoreMemorySystem containingMemSys)
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
		noc.ConnectBanks(cacheBank,bankRows,bankColumns,cacheParameters.nocConfig);
	}
    
    public int getBankNumber(long addr)
	{
		if(mapping == Mapping.SET_ASSOCIATIVE)
			return (int)((addr>>>blockSizeBits)%getNumOfBanks());
		else if(mapping == Mapping.ADDRESS)
		{
			long tag = (addr>>>blockSizeBits);
			int bankNumBits = (int)(Math.log10(getNumOfBanks())/Math.log10(2));
			int tagSize = (int)(Math.log10(tag)/Math.log10(2));
			int bankId = (int)(tag >>> (tagSize-bankNumBits +1));
	//		System.out.println(bankId);
			return bankId;
		}else
		{
			long tag = (addr>>>blockSizeBits);
			int bankNumBits = (int)(Math.log10(getNumOfBanks())/Math.log10(2));
			int tagSize = (int)(Math.log10(tag)/Math.log10(2));
			int bankId = (int)(tag >>> (tagSize-bankNumBits +1));
	//		System.out.println(bankId);
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
		return cacheRows*cacheColumns;		
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

	public abstract long getTag(long addr);
	public abstract Vector<Integer> getDestinationBankId(long addr);
	public abstract Vector<Integer> getNearestBankId(long addr,int coreId);
}