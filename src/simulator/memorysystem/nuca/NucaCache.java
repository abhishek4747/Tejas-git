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
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import main.ArchitecturalComponent;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import memorysystem.MainMemoryController;
import memorysystem.MemorySystem;
import misc.Util;
import config.CacheConfig;
import config.SimulationConfig;
import net.ID;
import net.NOC;
import net.NocInterface;
import config.SystemConfig;

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
    
    public Vector<NucaInterface> cacheBank;
    public HashMap<ID,NucaCacheBank> bankIdtoNucaCacheBank; 
    public Vector<Vector<Integer>> bankSets; //set of bank sets, each value denote the position of cache bank in "cacheBank"
    public NucaType nucaType;
    public Mapping mapping;

    static public HashMap<ID,Integer> accessedBankIds = new HashMap<ID, Integer>();
    public NucaCache(String cacheName, int id, CacheConfig cacheParameters,
			CoreMemorySystem containingMemSys)
	{
		super(cacheName, id, cacheParameters, containingMemSys);
        this.cacheBank =new Vector<NucaInterface>(); //cache banks are added later
        if(cacheParameters.nucaType == NucaType.D_NUCA)
        	this.bankSets = new Vector<Vector<Integer>>();
        this.mapping = cacheParameters.mapping;
        this.nucaType = cacheParameters.nucaType;
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
	
    public Cache integerToBank(int bankNumber)
	{
		return (Cache) this.cacheBank.get(bankNumber);
	}
	
	public int getNumOfBanks()
	{
		return cacheBank.size();		
	}
	public Cache getBank(long addr) {
		if(this.nucaType == NucaType.S_NUCA)
			return getSNucaBank(addr);
		else if(this.nucaType == NucaType.D_NUCA)
			return null;
		else
		{
			misc.Error.showErrorAndExit("Invalid Nuca Type");
			return null;
		}
					
	}
	public void addToBankSet(CommunicationInterface cominterface)
	{
		//All the cache banks in the same row is added to same set. 
		ID id = ((NocInterface) cominterface).getId();
		int row = id.getx();
		
		if(bankSets.get(row) == null)
		{
			bankSets.set(row, new Vector<Integer>());
		}
		bankSets.get(row).add(cacheBank.size()); //Next element to be added to "cacheBank" is the new cache bank.
												 //See -- In function createBanks, cacheBank.add(c)
                                         		 //So, cacheBank.size() gives its position in "cacheBank"
	}
//	public ID getCoreId(int coreId)
//	{
//		ID bankId = ((NocInterface) (ArchitecturalComponent.getCores()[coreId].comInterface)).getId();
//		return bankId;
//	}
//	
//	public ID getBankId(long addr)
//	{
//		ID destinationBankId;
//		int bankNumber= getBankNumber(addr);
//		destinationBankId = ((NocInterface) cacheBank.get(bankNumber).comInterface).getId();
//		return destinationBankId;
//	}
	public Cache createBanks(String token, CacheConfig config, CommunicationInterface cominterface) {
		int size = cacheBank.size();
		Cache c =null;
		if(config.nucaType == NucaType.S_NUCA){
			c = new SNucaBank(token+"["+size+"]", 0, config, null, this);
		}
		else if(config.nucaType == NucaType.D_NUCA)
		{
			c = new DNucaBank(token+"["+size+"]", 0, config, null, this);
			((NucaCache) c).addToBankSet(cominterface);
		}
		cacheBank.add((NucaInterface) c);
		return c;
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