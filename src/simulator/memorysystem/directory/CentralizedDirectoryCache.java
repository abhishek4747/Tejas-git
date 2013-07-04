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
package memorysystem.directory;

import generic.Core;
import generic.Event;
import generic.EventComparator;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;

import config.CacheConfig;
import main.ArchitecturalComponent;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CacheLine;
import memorysystem.CoreMemorySystem;
import memorysystem.MESI;
import memorysystem.MemorySystem;
import memorysystem.Cache.CacheType;

public class CentralizedDirectoryCache extends Cache{

	private int numPresenceBits;
	private long invalidations;
	private long directoryMisses;
	private long numReadMiss;
	private long numWriteMiss;
	private long numReadHit;
	private long numWriteHit;
	private long directoryHits;
	private long writebacks;
	private long dataForwards;
	Core[] cores;
	private DirectoryEntry[] lines;
	public boolean debug =false;
	private long timestamp=0;
	public static final long DIRECTORYSIZE = 32000;
	
	private HashMap<Long, DirectoryEntry> directoryHashmap;
	private PriorityQueue<DirectoryEntry> dirEntryHeap;
	
	public CentralizedDirectoryCache(CacheConfig cacheParameters,
			CoreMemorySystem containingMemSys, int numCores, Core[] coresArray) {
		super(cacheParameters, containingMemSys);
		directoryHashmap = new HashMap<Long, DirectoryEntry>();
		lines = new DirectoryEntry[cacheParameters.getSize()*1024];
		for(int i=0;i<lines.length;i++) {
			lines[i] = new DirectoryEntry(numCores, i);
		}
		invalidations =0;
		writebacks =0;
		dataForwards =0;
		directoryHits = 0;
		directoryMisses = 0;
		cores = coresArray;
		this.levelFromTop = CacheType.Directory;
		dirEntryHeap = new PriorityQueue<DirectoryEntry>(1, new DirectoryEntryComparator());
	}
	
	public long computeTag(long addr) {
		long tag = addr >>> (numSetsBits);
		return tag;
	}
	
	public int getSetIdx(long addr)
	{
		int startIdx = getStartIdx(addr);
		return startIdx/assoc;
	}
	
	public int getStartIdx(long addr) {
		long SetMask = ( 1 << (numSetsBits) )- 1;
		int startIdx = (int) ((addr ) & (SetMask));
		return startIdx;
	}
	
	public CacheLine getCacheLine(int idx) {
		return this.lines[idx];
	}
	
	
	public DirectoryEntry lookup(AddressCarryingEvent event ,long address, boolean calledFromEviction) {
		//	Search for the directory entry 
		//if not found, create one with invalid state 
		DirectoryEntry dirEntry = (DirectoryEntry) processRequest(RequestType.Cache_Read, address);
		if(dirEntry ==null) 
		{
			DirectoryEntry evictedDirEntry =  (DirectoryEntry) fill(address, MESI.UNCACHED);
			if(evictedDirEntry != null) 
			{
				sendeventToSharers(evictedDirEntry,(AddressCarryingEvent) event, RequestType.MESI_Invalidate,true);
			}
			dirEntry = (DirectoryEntry) access(address);
		}
		/*DirectoryEntry dirEntry = directoryHashmap.get(address);
		if(dirEntry==null){
			if(calledFromEviction)
			{
				return null;
				System.out.println(" address not found  " + address);
				System.err.println("cache line evicted but no entry in directory found " + address);
				System.exit(1);
			}
			dirEntry=new DirectoryEntry(numPresenceBits, address);
			dirEntry.setState( DirectoryState.uncached  );		//Needless as when dirEntry is initialized, state is set to invalid
			directoryHashmap.put( address, dirEntry );
			dirEntryHeap.add(dirEntry);
		}*/
		return dirEntry;
	}
	
	
	
	public void handleEvent( EventQueue eventQ, Event event )
	{
		if( event.getRequestType() == RequestType.WriteHitDirectoryUpdate )
		{
			writeHitDirectoryUpdate(eventQ,event);
		} 
		else if( event.getRequestType() == RequestType.WriteMissDirectoryUpdate )
		{
			writeMissDirectoryUpdate(eventQ,event);
		}
		else if( event.getRequestType() == RequestType.ReadMissDirectoryUpdate )
		{
			readMissDirectoryUpdate(eventQ, event);
		}
		else if( event.getRequestType() == RequestType.EvictionDirectoryUpdate )
		{
			EvictionDirectoryUpdate(eventQ, event);
		}
		else if (event.getRequestType() == RequestType.MemResponseDirectoryUpdate)
		{
			memResponseDIrectoryUpdate(eventQ,event);
		}
	}
	
	private void memResponseDIrectoryUpdate(EventQueue eventQ, Event event) 
	{
		long dirAddress = getDirectoryAddress((AddressCarryingEvent) event);
		DirectoryEntry dirEntry = lookup((AddressCarryingEvent)event,dirAddress,false);
		if(dirEntry == null)
		{
			return;
		}
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 
		dirEntry.setPresenceBit(requestingCore, true);
	}
	
	private boolean checkAndScheduleEventForNextCycle(long dirAddress, Event event)
	{
		DirectoryEntry dirEntry  = lookup((AddressCarryingEvent)event,dirAddress,false);
		if(dirEntry.getOwner() == -1)
		{
			if(dirEntry.getState() == MESI.UNCACHED)
			{
				return false;
			}
			this.getPort().put(event.update(event.getEventQ(),
								 1,
								 event.getRequestingElement(), 
								 this, 
								 event.getRequestType()));
			return true;
		} 
		if(debug) System.out.println("returned false");
		return false;
	}
	
	public void EvictionDirectoryUpdate(EventQueue eventQ,Event event) 
	{
		long dirAddress = getDirectoryAddress((AddressCarryingEvent)event);
		DirectoryEntry dirEntry = lookup((AddressCarryingEvent)event,dirAddress,true);
		if(dirEntry == null)
		{
			return;
		}
		MESI state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 
		
		if(checkAndScheduleEventForNextCycle(dirAddress, event))
			return;
		//System.out.println(event);
		//System.out.println(dirEntry);
		dirEntry.setTimestamp(++timestamp);
		/*dirEntryHeap.remove(dirEntry);
		dirEntryHeap.add(dirEntry);
*/
		if(state==MESI.MODIFIED) {
			//Writeback the result
			int prevOwner = dirEntry.getOwner();
			if( prevOwner==requestingCore ){
				this.writebacks++;
				incrementDirectoryMisses(1);
				((Cache)requestingElement).propogateWrite((AddressCarryingEvent)event);
				dirEntry.setPresenceBit(prevOwner, false);
				//Set the state to invalid - i.e. uncached
				dirEntry.setState(MESI.INVALID );
				dirEntry.resetAllPresentBits();
			}
			else{
				//System.err.println(" cacheline in modified state but evicted from some other core ");
				//System.exit(1);
				dirEntry.setPresenceBit(requestingCore, false);	//Redundant
				//If some other node is the owner, that means that the line in this node was already invalid.
				//No need to do anything.
				if(dirEntry.getOwner() == -1)
					dirEntry.setState(MESI.INVALID);
			}
		}
		else if(state==MESI.SHARED || 
				        state == MESI.EXCLUSIVE )
		{
			//Unset the presence bit of this cache - i.e. uncached
			dirEntry.setPresenceBit(requestingCore, false);
			//If no owner left, set the dirState to invalid
			int owner = dirEntry.getOwner();
			if(owner==-1)
				dirEntry.setState(MESI.INVALID );
		}
	}


	public void readMissDirectoryUpdate(EventQueue eventQ,Event event) {

		long dirAddress =getDirectoryAddress((AddressCarryingEvent)event);
		DirectoryEntry dirEntry = lookup((AddressCarryingEvent)event,dirAddress,false);
		MESI state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();
		

		MESI stateToSet = MESI.UNCACHED;
		
		if(checkAndScheduleEventForNextCycle(dirAddress, event))
		{
			return;
		}

		//System.out.println(event);
		//System.out.println(dirEntry);
		dirEntry.setTimestamp(++timestamp);
		/*dirEntryHeap.remove(dirEntry);
		dirEntryHeap.add(dirEntry);
*/
		incrementNumReadMiss(1);
		if( state==MESI.UNCACHED  )
		{
			incrementDirectoryMisses(1);
			stateToSet = MESI.EXCLUSIVE;
			if (((Cache)requestingElement).isLastLevel)
			{
				sendRequestToMainMemory( (AddressCarryingEvent)event );
			}
			else
			{
				Cache requestingCache = (Cache)requestingElement;
				requestingCache.sendReadRequestToLowerCache((AddressCarryingEvent)event);
			}
		}
		else if(state==MESI.MODIFIED )
		{
			incrementWritebacks(1);
			incrementDirectoryHits(1);
			incrementDirectoryMisses(1);
			sendMemResponse(dirEntry, (AddressCarryingEvent)event, RequestType.Cache_Read_Writeback);
			stateToSet = MESI.SHARED; //TODO check at owner whether the line is evicted or not Presently It is not checked
		}
		else if(state==MESI.SHARED 
				       ||  state == MESI.EXCLUSIVE )
		{
			incrementDirectoryHits(1);
			sendMemResponse(dirEntry, (AddressCarryingEvent)event, RequestType.Send_Mem_Response);
			stateToSet = MESI.SHARED;
		}
		dirEntry.setState(stateToSet);
		//updateDirectoryLRUQueue(dirAddress, (AddressCarryingEvent)event);
	}

	
	public void writeMissDirectoryUpdate(EventQueue eventQ,Event event) {

		long address = ((AddressCarryingEvent)event).getAddress();
		long dirAddress =getDirectoryAddress((AddressCarryingEvent) event);
		DirectoryEntry dirEntry = lookup((AddressCarryingEvent)event,dirAddress,false);
		MESI state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 
		
		if(checkAndScheduleEventForNextCycle(dirAddress, event))
		{
			return;
		}
		//System.out.println(event);
		//System.out.println(dirEntry);
		incrementNumWriteMiss(1);
		dirEntry.setTimestamp(++timestamp);
		/*dirEntryHeap.remove(dirEntry);
		dirEntryHeap.add(dirEntry);
		*/
		if(state == MESI.UNCACHED )
		{
			incrementDirectoryMisses(1);
			dirEntry.setState( MESI.EXCLUSIVE );
			//Request lower levels
			if (((Cache)requestingElement).isLastLevel)
			{
				sendRequestToMainMemory((AddressCarryingEvent)event);
			}
			else
			{
				Cache requestingCache = (Cache)requestingElement;
				requestingCache.sendReadRequestToLowerCache((AddressCarryingEvent)event);
			}
		}
		else if( state==MESI.MODIFIED  )
		{
			//request for the blocks from the previous owner
			incrementDirectoryHits(1);
			int prevOwner = dirEntry.getOwner();
			if(prevOwner == event.coreId)
			{
				cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
						new AddressCarryingEvent(
										eventQ,
										((Cache)requestingElement).getLatencyDelay(),
										requestingElement,
										cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache(),
										RequestType.Send_Mem_Response,
										address,
										(event).coreId));
			}
			else
			{
				incrementInvalidations(1);
				sendMemResponse(dirEntry,(AddressCarryingEvent) event, RequestType.Send_Mem_Response_Invalidate);
				dirEntry.resetAllPresentBits();
			}
		}
		else if( state == MESI.SHARED )
		{
			//request for the blocks from any of the owners
			incrementDirectoryHits(1);
			sendMemResponse(dirEntry,(AddressCarryingEvent) event, RequestType.Send_Mem_Response_Invalidate);

			//invalidate all except for the one from which the block has been requested
			sendeventToSharers(dirEntry,(AddressCarryingEvent) event, RequestType.MESI_Invalidate,false);
			dirEntry.resetAllPresentBits();
			dirEntry.setState( MESI.MODIFIED );
		}
		else if( state == MESI.EXCLUSIVE )
		{
			incrementDirectoryHits(1);
			if(requestingCore == dirEntry.getOwner())
			{
				dirEntry.setState(MESI.MODIFIED);
				cores[requestingCore].getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
						new AddressCarryingEvent(
										eventQ,
										((Cache)requestingElement).getLatencyDelay(),
										requestingElement,
										cores[requestingCore].getExecEngine().getCoreMemorySystem().getL1Cache(),
										RequestType.Send_Mem_Response,
										address,
										(event).coreId));
			}
			else
			{
				sendMemResponse(dirEntry, (AddressCarryingEvent)event,RequestType.Send_Mem_Response_Invalidate );
				dirEntry.resetAllPresentBits();
				dirEntry.setState( MESI.MODIFIED );
			}
		}
		//updateDirectoryLRUQueue(dirAddress, (AddressCarryingEvent)event);
	}

	public void writeHitDirectoryUpdate(EventQueue eventQ,Event event) {
		long dirAddress =getDirectoryAddress((AddressCarryingEvent) event);
		DirectoryEntry dirEntry = lookup((AddressCarryingEvent)event,dirAddress,false);
		int requestingCore = event.coreId; 
		
		if(checkAndScheduleEventForNextCycle(dirAddress, event))
		{
			return;
		}
		//System.out.println(event);
		//System.out.println(dirEntry);
		//incrementDirectoryHits(1);
		incrementNumWriteHit(1);
		dirEntry.setTimestamp(++timestamp);
	/*	dirEntryHeap.remove(dirEntry);
		dirEntryHeap.add(dirEntry);
	*/	if(dirEntry.getState() == MESI.UNCACHED ){
			//System.err.println(" not possible because of cache hit ");
		}
		else if(dirEntry.getState() == MESI.MODIFIED )
		{
			if( requestingCore != dirEntry.getOwner() )
			{
				sendMemResponse(dirEntry, (AddressCarryingEvent)event,RequestType.Send_Mem_Response_On_WriteHit );			
				dirEntry.resetAllPresentBits();
			}
		}
		else if( dirEntry.getState() == MESI.SHARED )
		{
			sendeventToSharers(dirEntry,(AddressCarryingEvent) event, RequestType.MESI_Invalidate,false);	
			dirEntry.setState(MESI.MODIFIED);
			dirEntry.resetAllPresentBits();
			dirEntry.setPresenceBit(requestingCore, true);
		}
		else if(dirEntry.getState()==MESI.EXCLUSIVE )
		{
			if(requestingCore == dirEntry.getOwner( ))
			{
				dirEntry.setState(MESI.MODIFIED);
			}
			else
			{
				sendMemResponse(dirEntry, (AddressCarryingEvent)event,RequestType.Send_Mem_Response_On_WriteHit);
				dirEntry.resetAllPresentBits();
				dirEntry.setState( MESI.MODIFIED );
			}
		}
		
		//updateDirectoryLRUQueue(dirAddress, (AddressCarryingEvent)event);
	}
	
	
	
	private long counter = 0;
	
	void updateDirectoryLRUQueue(long dirAddress,AddressCarryingEvent event) 
	{
		counter++;
		if(counter%1000 != 0)
		{
			return;
		}
		//Vector<DirectoryEntry> temp = new Vector<DirectoryEntry>( this.directoryHashmap.values());
		while(dirEntryHeap.size() >= DIRECTORYSIZE)
		{
			DirectoryEntry dirEntry = dirEntryHeap.peek();
			sendeventToSharers(dirEntry,(AddressCarryingEvent) event, RequestType.MESI_Invalidate,true);
		}
		
		//System.out.println("directory LRUQueue size: " + directoryLRUQueue.size() + "directorySize: "+ temp.size());
		
		/*if(directoryLRUQueue.contains(dirAddress)) 
		{
			
			directoryLRUQueue.remove(dirAddress);
			//elemIndex.remove(dirAddress);
		}
		//int size = directoryLRUQueue.size();
		directoryLRUQueue.add(dirAddress);
		//elemIndex.put(dirAddress, size);
		if(directoryLRUQueue.size() != temp.size()) {
			Vector v = null;
			v.size();
		}*/
	}
	
	private void sendMemResponse(DirectoryEntry dirEntry,AddressCarryingEvent event,RequestType requestType)
	{
		incrementDataForwards(1);
		int owner = dirEntry.getOwner();
		if(cores[owner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop != CacheType.L1)
		{
			System.err.println(" getL1 is not returning L1 cache " + cores[owner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop);
			System.exit(1);
		}
		cores[owner].getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
				new AddressCarryingEvent(
						event.getEventQ(),
						cores[owner].getExecEngine().getCoreMemorySystem().getL1Cache().getLatency() +getNetworkDelay(),
						event.getRequestingElement(), 
						cores[owner].getExecEngine().getCoreMemorySystem().getL1Cache(),
						requestType, 
						event.getAddress(),
						(event).coreId));
	}
	
	private void sendeventToSharers(DirectoryEntry dirEntry,AddressCarryingEvent event, RequestType requestType,boolean flag)
	{
		int requestingCore = event.coreId;
		for(int i=0;i<numPresenceBits;i++){
			if(dirEntry.getPresenceBit(i))
			{
				//Invalidate others
				incrementInvalidations(1);
				if(i!=requestingCore || flag)
				{ 
					dirEntry.setPresenceBit(i,false);
					if(cores[i].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop != CacheType.L1)
					{
						System.err.println(" getL1 is not returning L1 cache " + cores[i].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop);
						System.exit(1);
					}
					cores[i].getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
							new AddressCarryingEvent(
									event.getEventQ(),
									cores[i].getExecEngine().getCoreMemorySystem().getL1Cache().getLatency() + getNetworkDelay(),
									this, 
									cores[i].getExecEngine().getCoreMemorySystem().getL1Cache(),
									requestType, 
									event.getAddress(),
									(event).coreId));
				}
			}
		}
	}
	
	private void sendRequestToMainMemory(AddressCarryingEvent event)
	{
		MemorySystem.mainMemory.getPort().put(
				new AddressCarryingEvent(
						event.getEventQ(),
						MemorySystem.mainMemory.getLatencyDelay(),
						event.getRequestingElement(), 
						MemorySystem.mainMemory,
						RequestType.Main_Mem_Read,
						event.getAddress(),
						(event).coreId));
	}
	
	public long getDirectoryAddress(AddressCarryingEvent event)
	{
		long address = event.getAddress();
		long addressToStore =  address >>>  ((Cache)event.getRequestingElement()).blockSizeBits;
		
		if(debug) System.out.println("address returned " + addressToStore);
		
		return addressToStore;
	}
	public MESI updateDirectoryWarmUp(long address,int coreid,RequestType requestType)
	{
		DirectoryEntry dirEntry= search(address);
//		DirectoryEntry dirEntry = look
		MESI state=MESI.INVALID;
		MESI dirState = MESI.UNCACHED;
		
		if(dirEntry==null)
		{
			dirEntry=new DirectoryEntry(numPresenceBits, address);
			state=MESI.EXCLUSIVE;
			dirState = MESI.EXCLUSIVE;
			directoryHashmap.put( address, dirEntry );
		}
		else
		{
			if(dirEntry.getState() == MESI.UNCACHED )
			{
				state=MESI.EXCLUSIVE;
				dirState=MESI.EXCLUSIVE;
			}
			else //Instead of Invalidating Others 
			{
				state=MESI.SHARED;
				dirState=MESI.SHARED;
			}
		}
		
		dirEntry.setState(dirState);
		dirEntry.setPresenceBit(coreid,true);
		return state;
	}

	public DirectoryEntry search(long address)
	{
		return directoryHashmap.get(address);
	}
	public long getInvalidations() {
		return invalidations;
	}	
	public void incrementInvalidations(int invalidations) {
		this.invalidations += invalidations;
	}	
	public long getWritebacks() {
		return writebacks;
	}	
	public void incrementWritebacks(int writebacks) {
		this.writebacks += writebacks;
	}	
	public long getDataForwards() {
		return dataForwards;
	}	
	public void incrementDataForwards(int dataForwards) {
		this.dataForwards += dataForwards;
	}
	public long getDirectoryMisses() {
		return directoryMisses;
	}
	public void incrementDirectoryMisses(long directoryMisses) {
		this.directoryMisses += directoryMisses;
	}
	public long getDirectoryHits() {
		return directoryHits;
	}
	public void incrementDirectoryHits(long directoryHits) {
		this.directoryHits += directoryHits;
	}
	public long getNumReadMiss() {
		return numReadMiss;
	}
	public void incrementNumReadMiss(long numReadMiss) {
		this.numReadMiss += numReadMiss;
	}
	public long getNumWriteMiss() {
		return numWriteMiss;
	}
	public void incrementNumWriteMiss(long numWriteMiss) {
		this.numWriteMiss += numWriteMiss;
	}
	public long getNumReadHit() {
		return numReadHit;
	}
	public void incrementNumReadHit(long numReadHit) {
		this.numReadHit += numReadHit;
	}
	public long getNumWriteHit() {
		return numWriteHit;
	}
	public void incrementNumWriteHit(long numWriteHit) {
		this.numWriteHit += numWriteHit;
	}
	
	public long getNumberOfDirectoryEntries() {
		Vector<DirectoryEntry> temp = new Vector<DirectoryEntry>( this.directoryHashmap.values());
		return temp.size();
	}
	
	public int getNetworkDelay() {
		return 6;
	}
	
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		s.append(this.levelFromTop + " : ");
		
		return s.toString();
	}
}
