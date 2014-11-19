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

				Contributor: Anuj Arora, Mayur Harne
*****************************************************************************/


package memorysystem.nuca;

import generic.CommunicationInterface;
import generic.SimulationElement;

import java.util.HashMap;
import java.util.Vector;

import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import net.ID;
import net.NocInterface;
import config.CacheConfig;

public class NucaCache extends Cache
{
	public static enum NucaType{
		S_NUCA,
		D_NUCA,
		NONE
	}
	
	public static enum Mapping {
		SET_ASSOCIATIVE,
		ADDRESS,
		BOTH
	}
    
    public Vector<Cache> cacheBank;
    public HashMap<ID,NucaCacheBank> bankIdtoNucaCacheBank; 
    public Vector<Vector<Integer>> bankSets; //set of bank sets, each value denote the position of cache bank in "cacheBank"
    public NucaType nucaType;
    public Mapping mapping;

    static public HashMap<ID,Integer> accessedBankIds = new HashMap<ID, Integer>();
    public NucaCache(String cacheName, int id, CacheConfig cacheParameters,
			CoreMemorySystem containingMemSys)
	{
		super(cacheName, id, cacheParameters, containingMemSys);
        this.cacheBank =new Vector<Cache>(); //cache banks are added later
        if(cacheParameters.nucaType == NucaType.D_NUCA)
        	this.bankSets = new Vector<Vector<Integer>>();
        this.mapping = cacheParameters.mapping;
        this.nucaType = cacheParameters.nucaType;
    }
    
    public Cache createBanks(String token, CacheConfig config, CommunicationInterface cominterface) {
		int size = cacheBank.size();
		Cache c =null;
		if(config.nucaType == NucaType.S_NUCA){
			c = new SNucaBank(token+"["+size+"]", 0, config, null, this);
		}
		else if(config.nucaType == NucaType.D_NUCA)
		{
			c = new DNucaBank(token+"["+size+"]", 0, config, null, this);
			addToBankSet(cominterface);
		}
		cacheBank.add(c);
		return c;
	}
    public Cache getBank(ID id, long addr) {
		if(this.nucaType == NucaType.S_NUCA)
			return getSNucaBank(addr);
		else if(this.nucaType == NucaType.D_NUCA)
			return getDNucaBank(getBankSetId(addr), id);
		else
		{
			misc.Error.showErrorAndExit("Invalid Nuca Type");
			return null;
		}
					
	}
    
    //For SNUCA
	public Cache getSNucaBank(long addr)
	{
		if(mapping == Mapping.SET_ASSOCIATIVE) 
		{
			long tag = (addr>>>(numSetsBits+blockSizeBits));
			return integerToBank((int)(tag & (getNumOfBanks()-1)));
		}
		else if(mapping == Mapping.ADDRESS)
		{
			long tag = (addr>>>(numLinesBits+blockSizeBits));
			return integerToBank((int)(tag & (getNumOfBanks()-1)));
		}
		else
		{
			misc.Error.showErrorAndExit("Invalid Type of Mapping!!!");
			return null;
		}
	}
	

	//For D_NUCA
	Cache getDNucaBank(int bankS,ID coreId)
	{
		Vector<Integer> bankSet = bankSets.get(bankS); 
		int bankNum=-1;
		int min=Integer.MAX_VALUE;
		for(int bank : bankSet)
		{
			ID bankId =((NocInterface)(cacheBank.get(bank)).getComInterface()).getId();
			int dist = (coreId.getx() - bankId.getx())*(coreId.getx() - bankId.getx()) + 
					   (coreId.gety() - bankId.gety())*(coreId.gety() - bankId.gety()) ;
			if(dist<min)
			{
				min=dist;
				bankNum = bank;
			}
		}
		return (Cache) this.cacheBank.get(bankNum);
	}
	public void addToBankSet(CommunicationInterface cominterface)
	{
		//All the cache banks in the same row is added to same set. 
		ID id = ((NocInterface) cominterface).getId();
		int row = id.getx();
		while(bankSets.size() < row+1)
		{
			bankSets.add(new Vector<Integer>());
		}
		bankSets.get(row).add(this.cacheBank.size()); //Next element to be added to "cacheBank" is the new cache bank.
												 //See -- In function createBanks, cacheBank.add(c)
                                         		 //So, cacheBank.size() gives its position in "cacheBank"
	}
	int getBankNum(long addr)
	{
		int bankNum=-1;
		if(mapping == Mapping.SET_ASSOCIATIVE) 
		{
			long tag = (addr>>>(numSetsBits+blockSizeBits));
			bankNum = (int) (tag & (getNumOfBanks()-1)); //FIXME: getNumOfBanks() assumes 2^n.. remove that
		}
		else if(mapping == Mapping.ADDRESS)
		{
			long tag = (addr>>>(numLinesBits+blockSizeBits));
			bankNum = (int) (tag & (getNumOfBanks()-1));
		}
		else
		{
			misc.Error.showErrorAndExit("Invalid Type of Mapping!!!");
			return 0;
		}
		return bankNum;
	}
	int findBankSetNum(int bankNum)
	{
		int bankSetNum = -1;
		for(Vector<Integer> bankSet : bankSets)
		{
			for(int bankNumber : bankSet){
				if(bankNum == bankNumber)
					bankSetNum = bankSets.indexOf(bankSet);
			}
		}
		if(bankSetNum == -1)
			misc.Error.showErrorAndExit("Error in finding the bank set!!!");
		return bankSetNum;
	}

	int getBankSetId(long addr)
	{
		int bankNum = getBankNum(addr);
		int bankSet = findBankSetNum(bankNum);
		return bankSet;
	}

	
    public Cache integerToBank(int bankNumber)
	{
		return (Cache) this.cacheBank.get(bankNumber);
	}
	
	public int getNumOfBanks()
	{
		return cacheBank.size();		
	}
	
//	public void updateMaxHopLength(int newHopLength,AddressCarryingEvent event) 
//	{
//		numOfRequests++;
//		
//		if(this.maxHopLength < newHopLength) 
//		{
//			this.maxHopLength = newHopLength;
//			System.out.println("source " + event.getSourceId() + 
//								"destination "+ event.getDestinationId() + 
//								"Hop Length " + this.maxHopLength);
//		}
//	}
//	
//	public void updateMinHopLength(int newHopLength) 
//	{
//		if(this.minHopLength > newHopLength) 
//		{
//			this.minHopLength = newHopLength;
//		}
//	}
//	
//	public void updateAverageHopLength(int newHopLength)
//	{
//		averageHopLength += newHopLength;
//	}
//	
//	public int getMaxHopLength() {
//		return this.maxHopLength;
//	}
//	
//	public int getMinHopLength() {
//		return this.minHopLength;
//	}
//	
//	public float getAverageHoplength() {
//		return ((float)this.averageHopLength/(this.numOfRequests+1));
//	}
//	public int getTotalNucaBankAcesses() {
//		return totalNucaBankAcesses;
//	}
//
//	public void setTotalNucaBankAcesses(int totalNucaBankAcesses) {
//		this.totalNucaBankAcesses = totalNucaBankAcesses;
//	}
//	public int incrementTotalNucaBankAcesses(int i) {
//		return totalNucaBankAcesses+=i;
//	}
}	