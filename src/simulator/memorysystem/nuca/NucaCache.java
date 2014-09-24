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
import net.NocInterface;
import net.optical.TopLevelTokenBus;
import config.SystemConfig;

public class NucaCache extends Cache
{
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
    
    public Vector<NucaCacheBank> cacheBank;
    public HashMap<Vector<Integer>,NucaCacheBank> bankIdtoNucaCacheBank; 
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
    static public HashMap<Vector<Integer>,Integer> accessedBankIds = new HashMap<Vector<Integer>, Integer>();
    public NucaCache(CacheConfig cacheParameters, CoreMemorySystem containingMemSys, TopLevelTokenBus tokenbus,NucaType nucaType)
    {
    	//TODO : cache id can be more intuitive
    	super("NucaCache", 0, cacheParameters, containingMemSys);
    	this.nucaType = SimulationConfig.nucaType;
    	this.cacheRows = SystemConfig.nocConfig.getNumberOfBankRows();
        this.cacheColumns = SystemConfig.nocConfig.getNumberOfBankColumns();
        this.cacheBank =new Vector<NucaCacheBank>();
        this.bankIdtoNucaCacheBank = new HashMap<Vector<Integer>, NucaCacheBank>();
        this.blockSizeBits = Util.logbase2(cacheParameters.getBlockSize());
        this.mapping = SystemConfig.nocConfig.mapping;
        maxHopLength = Integer.MIN_VALUE;
        minHopLength = Integer.MAX_VALUE;
        noc = new NOC();
        this.nucaType = nucaType;
        missStatusHoldingRegister = new Mode3MSHR(blockSizeBits, 40000, null);
    }
    protected void makeCacheBanks(CacheConfig cacheParameters,CoreMemorySystem containingMemSys, TopLevelTokenBus tokenBus, NucaType nucaType, SNuca nucaCache)
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
	
	
	/*protected void handleMemResponse(EventQueue eventQ, Event event)
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
	}*/
    
    
	public int getBankNumber(long addr)
	{
		if(mapping == Mapping.SET_ASSOCIATIVE) 
		{
			long tag = (addr>>>(numSetsBits+blockSizeBits));
			return (int)(tag & (getNumOfBanks()-1));
		}
		else if(mapping == Mapping.ADDRESS)
		{
			long tag = (addr>>>(numLinesBits+blockSizeBits));
			return (int)(tag & (getNumOfBanks()-1));
		}
		else
		{
			misc.Error.showErrorAndExit("Invalid Type of Mapping!!!");
			return 0;
		}
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
		Vector<Integer> bankId = ((NocInterface) (ArchitecturalComponent.getCores()[coreId].comInterface)).getId();
		return bankId;
	}
	
	public Vector<Integer> getBankId(long addr)
	{
		Vector<Integer> destinationBankId = new Vector<Integer>();
		int bankNumber= getBankNumber(addr);
		destinationBankId = ((NocInterface) cacheBank.get(bankNumber).comInterface).getId();
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