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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import main.ArchitecturalComponent;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import memorysystem.MainMemoryController;
import memorysystem.MemorySystem;
import memorysystem.Mode3MSHR;
import misc.Util;
import config.CacheConfig;
import config.SimulationConfig;
import net.NOC;
import net.optical.TopLevelTokenBus;
import config.SystemConfig;

public class NucaCache extends Cache
{
	static int count=0;
	static int sum=0;
	public enum NucaType{
		S_NUCA,
		D_NUCA,
		NONE
	}
	
	public enum Mapping {
		SET_ASSOCIATIVE,
		ADDRESS,
		BOTH
	}
    /*cache is assumed to in the form of a 2 dimensional array*/
    public Vector<NucaCacheBank> cacheBank;
    public int cacheRows;
    public int cacheColumns;
    public NOC noc;
    public NucaType nucaType;
    public Mapping mapping;
    private long averageHopLength;
    private int maxHopLength;
    private int minHopLength;
    private long numOfRequests;
    private int totalNucaBankAcesses;
    public HashMap<NucaCacheBank,Vector<Long>> bankToAdresses;
    Vector<Long> blockAddresses = new Vector<Long>();
    public NucaCache(CacheConfig cacheParameters, CoreMemorySystem containingMemSys, TopLevelTokenBus tokenbus,NucaType nucaType)
    {
    	super(cacheParameters, containingMemSys);
    	this.nucaType = SimulationConfig.nucaType;
    	this.cacheRows = SystemConfig.nocConfig.getNumberOfBankRows();
        this.cacheColumns = SystemConfig.nocConfig.getNumberOfBankColumns();
        this.cacheBank =new Vector<NucaCacheBank>();
        this.blockSizeBits = Util.logbase2(cacheParameters.getBlockSize());
        this.mapping = SystemConfig.nocConfig.mapping;
        maxHopLength = Integer.MIN_VALUE;
        minHopLength = Integer.MAX_VALUE;
        noc = new NOC();
        this.nucaType = nucaType;
        missStatusHoldingRegister = new Mode3MSHR(blockSizeBits, 40000, null);
        bankToAdresses = new HashMap<NucaCacheBank, Vector<Long>>();
    }
    protected void makeCacheBanks(CacheConfig cacheParameters,CoreMemorySystem containingMemSys, TopLevelTokenBus tokenBus, NucaType nucaType, SNuca nucaCache)
   	{
       	int rows = SystemConfig.nocConfig.getNumberOfBankRows();
       	int cols = SystemConfig.nocConfig.getNumberOfBankColumns();
   		for(int i=0;i<rows;i++)
   		{
   			for(int j=0;j<cols;j++)
   			{
   				if(SystemConfig.nocConfig.nocElements.coresCacheLocations.get(i).get(j)==0)
   				{
   					Vector<Integer> bankId = new Vector<Integer>();
   					bankId.add(i);
   					bankId.add(j);
   					cacheBank.add(new NucaCacheBank(bankId, cacheParameters, containingMemSys, this, nucaType));
   				}
   			}
   		}
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
	}
    
    @Override
	public void handleEvent(EventQueue eventQ, Event event)
    {
	   if(event.getRequestType() == RequestType.Mem_Response)
	    {
	    	handleMemResponse(eventQ,event);
	    }
	}
	
	
	protected void handleMemResponse(EventQueue eventQ, Event event)
	{
		AddressCarryingEvent addrEvent = ((AddressCarryingEvent)event);
		updateMaxHopLength(addrEvent.hopLength,(AddressCarryingEvent)event);
		updateMinHopLength(addrEvent.hopLength);
		updateAverageHopLength(addrEvent.hopLength);
		ArrayList<AddressCarryingEvent> eventsToBeServed = missStatusHoldingRegister.removeRequestsByAddressIfAvailable(addrEvent);
		sendResponseToWaitingEvent(eventsToBeServed);
	}
	

	protected void sendResponseToWaitingEvent(ArrayList<AddressCarryingEvent> outstandingRequestList)
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
					MemorySystem.mainMemoryController.getPort().put(eventPoppedOut.updateEvent(eventPoppedOut.getEventQ(), 
							MemorySystem.mainMemoryController.getLatencyDelay(), this, 
							MemorySystem.mainMemoryController, RequestType.Main_Mem_Write,eventPoppedOut.getAddress(),eventPoppedOut.coreId));
				}
			}
		}
	}
    
    
	public int getBankNumber(long addr)
	{
		if(mapping == Mapping.SET_ASSOCIATIVE) 
		{
			long tag = (addr>>>(numSetsBits+blockSizeBits));
			//System.out.println("bankNumber long " + bankNumber + "bankNumberInt =" + (int)(bankNumber & (getNumOfBanks()-1)));
			return (int)(tag & (getNumOfBanks()-1));
		}
		else if(mapping == Mapping.ADDRESS)
		{
			long tag = (addr>>>(numSetsBits+blockSizeBits));
			int bankNumBits = (int)(Math.log10(getNumOfBanks())/Math.log10(2));
			int tagSize = (int)(Math.log10(tag)/Math.log10(2));
			int bankId = (int)(tag >>> (tagSize-bankNumBits)) + getNumOfBanks();
			return bankId;
		}
		else
		{
			long tag = (addr>>>(numSetsBits+blockSizeBits));
			int bankNumBits = (int)(Math.log10(getNumOfBanks())/Math.log10(2));
			int tagSize = (int)(Math.log10(tag)/Math.log10(2));
			int bankId = (int)(tag >>> (tagSize-bankNumBits)) + getNumOfBanks();
			return bankId;
		}
	}

	Vector<Integer> getMemoryControllerId(Vector<Integer> currBankId)//nearest Memory Controller
    {
    	double distance = Double.MAX_VALUE;
    	int memControllerId = 0;
    	int x1 = currBankId.get(0);//bankid/cacheColumns;
    	int y1 = currBankId.get(1);//bankid%cacheColumns;
    	MainMemoryController memController = MemorySystem.mainMemoryController;
    	for(int i=0;i<memController.numberOfMemoryControllers;i++)
    	{
    		int x2 = memController.mainmemoryControllersLocations[i]/cacheRows;
    		int y2 = memController.mainmemoryControllersLocations[i]%cacheColumns;
    		double localdistance = Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    		if(localdistance < distance) 
    		{
    			distance = localdistance;
    			memControllerId = memController.mainmemoryControllersLocations[i];
    		}
    	}
    	return integerToBankId(memControllerId);
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
		return cacheBank.size();		
	}
	
	public Vector<Integer> getCoreId(int coreId)
	{
		Vector<Integer> bankId = ArchitecturalComponent.getCores()[coreId].getId();
		return bankId;
	}
	
	public Vector<Integer> getBankId(long addr)
	{
		Vector<Integer> destinationBankId = new Vector<Integer>();
		int bankNumber= getBankNumber(addr);
		destinationBankId = cacheBank.get(bankNumber).getBankId();
		return destinationBankId;
	}
	
	public void updateMaxHopLength(int newHopLength,AddressCarryingEvent event) 
	{
		numOfRequests++;
		
		if(this.maxHopLength < newHopLength) 
		{
			this.maxHopLength = newHopLength;
			System.out.println("source " + event.getSourceId() + 
								"destination "+ event.getDestinationId() + 
								"Hop Length " + this.maxHopLength);
		}
	}
	
	public void updateMinHopLength(int newHopLength) 
	{
		if(this.minHopLength > newHopLength) 
		{
			this.minHopLength = newHopLength;
		}
	}
	
	public void updateAverageHopLength(int newHopLength)
	{
		averageHopLength += newHopLength;
	}
	
	public int getMaxHopLength() {
		return this.maxHopLength;
	}
	
	public int getMinHopLength() {
		return this.minHopLength;
	}
	
	public float getAverageHoplength() {
		return ((float)this.averageHopLength/(this.numOfRequests+1));
	}
	public int getTotalNucaBankAcesses() {
		return totalNucaBankAcesses;
	}

	public void setTotalNucaBankAcesses(int totalNucaBankAcesses) {
		this.totalNucaBankAcesses = totalNucaBankAcesses;
	}
	public int incrementTotalNucaBankAcesses(int i) {
		return totalNucaBankAcesses+=i;
	}
}	