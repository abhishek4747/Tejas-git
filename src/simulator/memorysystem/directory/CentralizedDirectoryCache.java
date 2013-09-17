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
import generic.GlobalClock;
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

public class CentralizedDirectoryCache extends Cache 
{

	private long invalidations;
	private long directoryMisses;
	private long numReadMiss;
	private long numWriteMiss;
	private long numReadHit;
	private long numWriteHit;
	private long directoryHits;
	private long writebacks;
	private long dataForwards;
	
	private DirectoryEntry[] lines;
	public boolean debug =false;
	private long timestamp=0;
	
	
	public CentralizedDirectoryCache(CacheConfig cacheParameters, CoreMemorySystem containingMemSys, int numCores) 
	{
		super(cacheParameters, containingMemSys);
		
		lines = new DirectoryEntry[cacheParameters.getSize()*1024];
		for(int i=0;i<lines.length;i++) {
			lines[i] = new DirectoryEntry(numCores);
		}
		invalidations =0;
		writebacks =0;
		dataForwards =0;
		directoryHits = 0;
		directoryMisses = 0;

		this.levelFromTop = CacheType.Directory;
	}
	
	// This function ensures that cache functions like access and fill return a directory entry and not a cache line.
	public DirectoryEntry getCacheLine(int idx) {
		return this.lines[idx];
	}
	
	
	public DirectoryEntry lookup(AddressCarryingEvent event ,long address) 
	{
		//	Search for the directory entry 
		//if not found, create one with invalid state 
		DirectoryEntry dirEntry = (DirectoryEntry) processRequest(RequestType.Cache_Read, address, event);
		if(dirEntry ==null)
		{
			// Right now, we tell the cache to mark this new line as exclusive.
			// Later on, we will mark it as invalid.
			// This follows our semantics - if address was not tracked before, its directory entry must be marked invalid.
			DirectoryEntry evictedDirEntry =  (DirectoryEntry) fill(address, MESI.EXCLUSIVE);
			
			if(evictedDirEntry != null) 
			{
				// Since the directory entry is being removed, all the caches holding this line must invalidate this line.
				sendeventToSharers(evictedDirEntry, RequestType.MESI_Invalidate, null);
				evictedDirEntry.clearAllSharers();
			}
			
			dirEntry = (DirectoryEntry) access(address);
			
			// The fill function in directory has not removed the previous sharers of the fillLine in the cache.
			// So, we must remove them here.
			dirEntry.clearAllSharers();
			
			// Explanation for Invalid state given above.
			dirEntry.setState(MESI.INVALID);
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
	
	
	boolean printDirectoryDebugMessages = false;
	public void handleEvent( EventQueue eventQ, Event event )
	{
		if(printDirectoryDebugMessages==true) {
//			if(event.getClass()==AddressCarryingEvent.class &&
//				((AddressCarryingEvent)event).getAddress()>>blockSizeBits==48037994l)
			{
				System.out.println("DIRECTORY : globalTime = " + GlobalClock.getCurrentTime() + 
						"\teventTime = " + event.getEventTime() + "\t" + event.getRequestType() + 
						"\t" + event.getRequestingElement() + 
						"\tdirEntry = " + access(((AddressCarryingEvent)event).getAddress()));
			}
		}
		
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
			memResponseDirectoryUpdate(eventQ,event);
		}
	}
	
	private void memResponseDirectoryUpdate(EventQueue eventQ, Event event) 
	{
		long dirAddress = getDirectoryAddress((AddressCarryingEvent) event);
		DirectoryEntry dirEntry = (DirectoryEntry) processRequest(RequestType.Cache_Read, dirAddress, (AddressCarryingEvent)event);
	
		// There are two scenarios where we would like to invalidate the cache entry for this address.
		
		// Case 1 : 
		// The directory entry associated with this cache line was evicted before the memResponse comes.
		// So, instruct the cache to invalidate this line from it.
		// This ensures that a valid cache line at cache is always present in the directory.
		
		// Case 2 :
		// The directory entry is modified and one sharer for this entry is already present.
		// This case can happen because of a series of events : 
		// Core 1 : WriteMiss for address x
		// Core 2 : ReadMiss for address y --- evicts the directory entry for address x
		// Core 3 : WriteMiss for address x
		// L2 sends reply to Core 1 : We add Core 1 as the sharer for address x
		// L2 sends reply to Core 3 : We add Core 3 as the sharer for address x 
		// Since, we this violates the MESI protocol, we invalidate the entry for address x
		Cache requestingCache = (Cache)event.getRequestingElement();
		
		boolean needToInvalidateCacheEntry = (dirEntry==null) || (dirEntry.getState()==MESI.MODIFIED && dirEntry.getNoOfSharers()>0 && dirEntry.getOwner()!=requestingCache);
		if(needToInvalidateCacheEntry)
		{
						
			requestingCache.getPort().put(
					new AddressCarryingEvent(
						requestingCache.containingMemSys.getCore().getEventQueue(),
						0, //requestingCache.getLatency() + getNetworkDelay(), FIXME: 
						this, 
						requestingCache,
						RequestType.MESI_Invalidate, 
						((AddressCarryingEvent)event).getAddress(),
						requestingCache.containingMemSys.getCore().getCore_number()));
			
			return;
		}
		
		//Cache requestingCache = (Cache)event.getRequestingElement();
		
		// If the state of directory entry is exclusive, set it to shared before adding a new sharer
		if(dirEntry.getState()==MESI.EXCLUSIVE && dirEntry.getNoOfSharers()>0 && dirEntry.getOwner()!=requestingCache) {
			dirEntry.setState(MESI.SHARED);
		}
		
		dirEntry.addSharer(requestingCache);
	}
	
	private boolean checkAndScheduleEventForNextCycle(long dirAddress, Event event)
	{
		DirectoryEntry dirEntry  = lookup((AddressCarryingEvent)event,dirAddress);
		if(dirEntry.getNoOfSharers()==0)
		{
			if(dirEntry.getState() == MESI.INVALID)
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
		
		if(debug) {
			System.out.println("returned false");
		}
		
		return false;
	}
	
	public void EvictionDirectoryUpdate(EventQueue eventQ,Event event) 
	{
		long dirAddress = getDirectoryAddress((AddressCarryingEvent)event);
		DirectoryEntry dirEntry = lookup((AddressCarryingEvent)event,dirAddress);
		if(dirEntry == null) {
			return;
		}
		
		MESI state = dirEntry.getState();
		Cache requestingCache = (Cache)event.getRequestingElement();
				
		if(checkAndScheduleEventForNextCycle(dirAddress, event)) {
			return;
		}
		
		//System.out.println(event);
		//System.out.println(dirEntry);
		dirEntry.setTimestamp(++timestamp);
		/*dirEntryHeap.remove(dirEntry);
		dirEntryHeap.add(dirEntry);
*/
		incrementDirectoryHits(1);

		
		if(state==MESI.MODIFIED) {
			//Writeback the result
			Cache prevOwner = dirEntry.getOwner();
			if( prevOwner==requestingCache ){
				this.writebacks++;
				requestingCache.propogateWrite((AddressCarryingEvent)event);
				dirEntry.setState(MESI.INVALID );
				dirEntry.clearAllSharers();
			}
			else{
				// An Invalidate request was sent some time back. It will reach the 
				// cache in some time - We Hope !!
				return;
			}
		}
		
		else if(state==MESI.SHARED || state == MESI.EXCLUSIVE )
		{
			if(dirEntry.isSharer(requestingCache)==false) {
				// This cache line may be shared in the past.
				// An invalidation request for the requestingCache will reach there in some time
				// time t  --> SHARED between core 1 and core 2
				// time t+1 --> WRITE_HIT for core 2; INVALIDATION sent to core 1
				// time t+2 --> EVICTION by core 1 -- WE ARE HERE NOW.
				// time t+3 --> INVALIDATION message reaches core 1 (FUTURE)
				return;
			}
			
			dirEntry.removeSharer(requestingCache);
			
			if(dirEntry.getNoOfSharers()==0) {
				dirEntry.setState(MESI.INVALID );
			}
		}
		
		else
		{
			// Hack :  writeMiss for address x came.
			// Then, before the memResponse for address x comes, its directory entry was evicted.
			// Now, when the cache wants to evict address x, it finds the entry as invalid.
			return;
		}
	}


	public void readMissDirectoryUpdate(EventQueue eventQ,Event event) {

		long dirAddress =getDirectoryAddress((AddressCarryingEvent)event);
		DirectoryEntry dirEntry = lookup((AddressCarryingEvent)event,dirAddress);
		MESI state = dirEntry.getState();
		Cache requestingCache = (Cache)event.getRequestingElement();
		

		MESI stateToSet;
		
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
		
		if( state==MESI.INVALID )
		{
			incrementDirectoryMisses(1);
			stateToSet = MESI.EXCLUSIVE;
			
			if (requestingCache.isLastLevel) {
				sendRequestToMainMemory( (AddressCarryingEvent)event );
			} else {
				requestingCache.sendReadRequestToLowerCache((AddressCarryingEvent)event);
			}
		}
		
		else if(state==MESI.MODIFIED )
		{
			incrementWritebacks(1);
			incrementDirectoryHits(1);
			if(requestingCache==dirEntry.getOwner()) {
				this.sendResponseToAPendingEventOfSameCacheLine(requestingCache, event);
				return;
			}
			sendMemResponse(dirEntry, (AddressCarryingEvent)event, RequestType.Cache_Read_Writeback);
			stateToSet = MESI.SHARED; //TODO check at owner whether the line is evicted or not Presently It is not checked
		}
		
		else if(state==MESI.SHARED ||  state == MESI.EXCLUSIVE )
		{
			// A cache which says read miss for address x must not be shown as a sharer for it.
			if(dirEntry.isSharer(requestingCache)) {
				this.sendResponseToAPendingEventOfSameCacheLine(requestingCache, event);
				return;
			}
			
			incrementDirectoryHits(1);
			sendMemResponse(dirEntry, (AddressCarryingEvent)event, RequestType.Send_Mem_Response);
			stateToSet = MESI.SHARED;
		}
		
		else
		{
			misc.Error.showErrorAndExit("directory error !!");
			stateToSet = MESI.INVALID;
		}
		
		dirEntry.setState(stateToSet);
		//updateDirectoryLRUQueue(dirAddress, (AddressCarryingEvent)event);
	}

	
	public void writeMissDirectoryUpdate(EventQueue eventQ,Event event) {

		long dirAddress =getDirectoryAddress((AddressCarryingEvent) event);
		DirectoryEntry dirEntry = lookup((AddressCarryingEvent)event,dirAddress);
		MESI state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();
		Cache requestingCache = (Cache)requestingElement; 
		
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
		
		if(state == MESI.INVALID )
		{
			incrementDirectoryMisses(1);
			dirEntry.setState( MESI.MODIFIED );
			
			//Request lower levels
			if (((Cache)requestingElement).isLastLevel) {
				sendRequestToMainMemory((AddressCarryingEvent)event);
			} else {
				//FIXME : Actually the directory must send a request to owner to read from the lower level.
				requestingCache.sendReadRequestToLowerCache((AddressCarryingEvent)event);
			}
		}
		else if( state==MESI.MODIFIED  )
		{
			//request for the blocks from the previous owner
			incrementDirectoryHits(1);
			Cache prevOwner = dirEntry.getOwner();
			if(prevOwner == requestingCache) {
				this.sendResponseToAPendingEventOfSameCacheLine(requestingCache, event);
				return;
			} else {
				incrementInvalidations(1);
				sendMemResponse(dirEntry,(AddressCarryingEvent) event, RequestType.Send_Mem_Response_Invalidate);
				dirEntry.clearAllSharers();
			}
		}
		else if( state == MESI.SHARED )
		{
			//request for the blocks from any of the owners
			incrementDirectoryHits(1);
			sendMemResponse(dirEntry,(AddressCarryingEvent) event, RequestType.Send_Mem_Response_Invalidate);

			//invalidate all except for the one from which the block has been requested
			sendeventToSharers(dirEntry, RequestType.MESI_Invalidate, null);
			dirEntry.clearAllSharers();
			dirEntry.setState( MESI.MODIFIED );
		}
		else if( state == MESI.EXCLUSIVE )
		{
			incrementDirectoryHits(1);
			if(requestingCache == dirEntry.getOwner()) {
				this.sendResponseToAPendingEventOfSameCacheLine(requestingCache, event);
				return;
			} else {
				sendMemResponse(dirEntry, (AddressCarryingEvent)event,RequestType.Send_Mem_Response_Invalidate );
				dirEntry.clearAllSharers();
				dirEntry.setState( MESI.MODIFIED );
			}
		}
		//updateDirectoryLRUQueue(dirAddress, (AddressCarryingEvent)event);
	}

	public void writeHitDirectoryUpdate(EventQueue eventQ,Event event) 
	{
		long dirAddress =getDirectoryAddress((AddressCarryingEvent) event);
		DirectoryEntry dirEntry = lookup((AddressCarryingEvent)event,dirAddress);
		Cache requestingCache = (Cache)event.getRequestingElement(); 
		
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
	*/	
		
		if(dirEntry.getState()==MESI.EXCLUSIVE) {
			incrementDirectoryHits(1);
			// Mark it as modified
			if(requestingCache == dirEntry.getOwner( )) {
				dirEntry.setState(MESI.MODIFIED);
			} else {
				// The entry for this directory line was invalidated a short while ago.
				// Before the invalidate request reaches the cache, it sent a writeHit
				// So just return from here. The cache will invalidate the entry later.
				return;
			}
			
		} else if (dirEntry.getState()==MESI.SHARED) {
			incrementDirectoryHits(1);
			// Invalidate the entry of all other caches
			sendeventToSharers(dirEntry, RequestType.MESI_Invalidate, requestingCache);	
			dirEntry.clearAllSharers();
			
			// Mark the entry as modified
			dirEntry.setState(MESI.MODIFIED);
			
			// Since the cache does not bother about the cache line after modifying it,
			// unlike write miss event, we add the sharer at the same moment.
			dirEntry.addSharer(requestingCache);
			
		} else {
			// The entry for this directory line was invalidated a short while ago.
			// Before the invalidate request reaches the cache, it sent a writeHit
			// So just return from here. The cache will invalidate the entry later.
			return;
		}
		
	}
	
	private void sendMemResponse(DirectoryEntry dirEntry,AddressCarryingEvent event,RequestType requestType)
	{
		incrementDataForwards(1);
		
		if(dirEntry.getNoOfSharers()==0) {
			misc.Error.showErrorAndExit("This address has no owner cache !!");
		}
		
		Cache ownerCache = dirEntry.getSharerAtIndex(0);
		
		ownerCache.getPort().put(
				new AddressCarryingEvent(
						event.getEventQ(),
						ownerCache.getLatency() +getNetworkDelay(),
						event.getRequestingElement(), 
						ownerCache,
						requestType, 
						event.getAddress(),
						(event).coreId));
	}
	
	private void sendeventToSharers(DirectoryEntry dirEntry, RequestType requestType, Cache excludeThisCache)
	{
		for(int i=0; i<dirEntry.getNoOfSharers(); i++) {
			
			incrementInvalidations(1);
			
			Cache c= dirEntry.getSharerAtIndex(i);
			if(c==excludeThisCache) {
				continue;
			}
			
			c.getPort().put(
				new AddressCarryingEvent(
					c.containingMemSys.getCore().getEventQueue(),
					c.getLatency() + getNetworkDelay(),
					this, 
					c,
					requestType, 
					getCacheAddress(c, dirEntry.getAddress()),
					c.containingMemSys.getCore().getCore_number()));
		}
	}
	
	private void sendRequestToMainMemory(AddressCarryingEvent event)
	{
		MemorySystem.mainMemory.getPort().put(
				new AddressCarryingEvent(
						event.getEventQ(),
						MemorySystem.mainMemory.getLatencyDelay() + getNetworkDelay(),
						event.getRequestingElement(), 
						MemorySystem.mainMemory,
						RequestType.Main_Mem_Read,
						event.getAddress(),
						(event).coreId));
	}
	
	public long getDirectoryAddress(AddressCarryingEvent event)
	{
		long address = event.getAddress();
//		long addressToStore =  address >>>  ((Cache)event.getRequestingElement()).blockSizeBits;
//		
//		if(debug) System.out.println("address returned " + addressToStore);
//		
//		return addressToStore;
		Cache requestingCache = (Cache)event.getRequestingElement(); 
		if(this.blockSizeBits!=requestingCache.blockSizeBits) {
			misc.Error.showErrorAndExit("requesting cache and directory must have same block size !!");
		}
		
		return address;
	}
	
	public long getCacheAddress(Cache c, long dirAddress)
	{
//		long cacheAddress = dirAddress << c.blockSizeBits;
//		return cacheAddress;
		if(this.blockSizeBits!=c.blockSizeBits) {
			misc.Error.showErrorAndExit("requesting cache and directory must have same block size !!");
		}
		
		return dirAddress;
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
		int numDirectoryEntries = 0;
		
		for(int i=0; i<numLines; i++) {
			if(lines[i].getNoOfSharers()>0) {
				numDirectoryEntries++;
			}
		}
		
		return numDirectoryEntries;
	}
	
	public static int getNetworkDelay() {
		return 6;
	}
	
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		s.append(this.levelFromTop + " : ");
		
		return s.toString();
	}
	
	public void sendResponseToAPendingEventOfSameCacheLine(Cache requestingCache, Event event)
	{
		// Following sequence of events may have happened : 
		// writeMiss for address x
		// writeMiss for address (x+1) [x and x+1 map to same directory address]
		// memResponse came for address x
		// now, writeMiss for (x+1) sees that the cache line is occupied by itself
		
		long latency = -1;
		if(requestingCache==event.getRequestingElement()) {
			latency = 0;
		} else {
			misc.Error.showErrorAndExit("requestingCache and requestingElement are supposed to be same !!");
		}
		
		requestingCache.getPort().put(
				new AddressCarryingEvent(
					requestingCache.containingMemSys.getCore().getEventQueue(),
					latency,
					event.getRequestingElement(),
					requestingCache,
					RequestType.Send_Mem_Response,
					((AddressCarryingEvent)event).getAddress(),
					(event).coreId));
	}
}
