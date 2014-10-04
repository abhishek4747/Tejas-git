package memorysystem.coherence;

import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.RequestType;
import generic.SimulationElement;
import generic.Statistics;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;

import memorysystem.CoreMemorySystem;
import memorysystem.MESI;
import memorysystem.MemorySystem;
import misc.Util;
import config.CacheConfig;
import config.EnergyConfig;
import config.SystemConfig;

// Unlock function should call the state change function. This is called using the current event field inside the directory entry.
// For write hit event, there is some mismatch

public class Directory extends SimulationElement implements Coherence {
	
	public CoreMemorySystem containingMemSys;
	protected int blockSize; // in bytes
	public int blockSizeBits; // in bits
	public  int assoc;
	protected int assocBits; // in bits
	protected int size; // MegaBytes
	protected int numLines;
	protected int numLinesBits;
	public int numSetsBits;
	protected double timestamp;
	protected int numLinesMask;
	private int getNumLines() {
		long totSize = size;
		return (int)(totSize / (long)(blockSize));
	}
	
	public int getSetIdx(long addr)
	{
		int startIdx = getStartIdx(addr);
		return startIdx/assoc;
	}
	
	public int getStartIdx(long addr) {
		long SetMask =( 1 << (numSetsBits) )- 1;
		int startIdx = (int) ((addr >>> blockSizeBits) & (SetMask));
		return startIdx;
	}
	
	public int getNextIdx(int startIdx,int idx) {
		int index = startIdx +( idx << numSetsBits);
		return index;
	}
	
	protected void mark(DirectoryEntry ll, long tag)
	{
		ll.setTag(tag);
		mark(ll);
	}
	
	private void mark(DirectoryEntry ll)
	{
		ll.setTimestamp(timestamp);
		timestamp += 1.0;
	}	
	
	long hits = 0, misses = 0;
	
	long readMissAccesses = 0;
	long writeHitAccesses = 0;
	long writeMissAccesses = 0;
	long evictedFromCoherentCacheAccesses = 0;
	long evictedFromSharedCacheAccesses = 0;	

	public Directory(String cacheName, int id, CacheConfig cacheParameters,
			CoreMemorySystem containingMemSys) {
		
		super(cacheParameters.portType,
				cacheParameters.getAccessPorts(), 
				cacheParameters.getPortOccupancy(),
				cacheParameters.getLatency(),
				cacheParameters.operatingFreq);
		
		MemorySystem.coherenceNameMappings.put(cacheName, this);
		
		this.blockSize = cacheParameters.getBlockSize();
		this.assoc = cacheParameters.getAssoc();
		this.size = cacheParameters.getSize();
		this.blockSizeBits = Util.logbase2(blockSize);
		this.assocBits = Util.logbase2(assoc);
		this.numLines = getNumLines();
		this.numLinesBits = Util.logbase2(numLines);
		this.numSetsBits = numLinesBits - assocBits;
		
		// Create directory entries
		lines = new DirectoryEntry[cacheParameters.numEntries];
        for(int i=0;i<lines.length;i++) {
            lines[i] = new DirectoryEntry();
        }
        
        // Create a pending events field of each set in the directory cache
        int numSets = lines.length/assoc;
        for(int i=0; i<numSets; i++) {
        	pendingEvents.add(new LinkedList<AddressCarryingEvent>());
        }
	}

	private DirectoryEntry[] lines;

	// List of pending event list for each set in the cache
	private ArrayList<LinkedList<AddressCarryingEvent>> pendingEvents = new ArrayList<LinkedList<AddressCarryingEvent>>();

	private void sendInvalidateForDirectoryEntry(DirectoryEntry evictedEntry) {
		long addr = evictedEntry.getAddress();
		
		if(evictedEntry.getListOfAwaitedCacheResponses().size()!=0) {
			misc.Error.showErrorAndExit("Cannot invalidate an entry which is already expecting cache responses");
		}
		
		for(Cache c : evictedEntry.getSharers()) {
			evictedEntry.addCacheToAwaitedCacheList(c);
			sendAnEventFromDirectoryToCache(addr, c, RequestType.DirectoryInvalidate);
		}
	}

	public void writeHit(long addr, Cache c) {
		writeHitAccesses++;
		sendAnEventFromCacheToDirectoryAndAddToPendingList(addr, c, RequestType.DirectoryWriteHit);
	}

	public void readMiss(long addr, Cache c) {
		readMissAccesses++;
		sendAnEventFromCacheToDirectoryAndAddToPendingList(addr, c, RequestType.DirectoryReadMiss);
	}

	public void writeMiss(long addr, Cache c) {
		writeMissAccesses++;
		sendAnEventFromCacheToDirectoryAndAddToPendingList(addr, c, RequestType.DirectoryWriteMiss);
	}

	private void sendAnEventFromCacheToDirectoryAndAddToPendingList
		(long addr, Cache c, RequestType request) {
		
		incrementHitMissInformation(addr);
		
		// Create an event
		Directory directory = this;
		AddressCarryingEvent event = new AddressCarryingEvent(
				c.getEventQueue(), 0, c, directory, request, addr);

		// 2. Send event to directory
		c.sendEvent(event);
		
		addPendingEvent(event);
	}
	
	private void incrementHitMissInformation(long addr) {
		DirectoryEntry dirEntry = getDirectoryEntry(addr);
		
		if(dirEntry==null || dirEntry.getState()==MESI.INVALID) {
			misses++;
		} else {
			hits++;
		}		
	}

	private void sendAnEventFromDirectoryToCache(long addr, Cache c, RequestType request) {
		// Create an event
		Directory directory = this;
		AddressCarryingEvent event = new AddressCarryingEvent(
				c.getEventQueue(), GlobalClock.getCurrentTime()
						+ directory.getLatency(), directory, c, request, addr);
	
		// 2. Send event to cache
		this.sendEvent(event);
	}

	private void addPendingEvent(AddressCarryingEvent event) {
		long addr = event.getAddress();
		long setAddress = getSetAddress(addr);
		pendingEvents.get((int) setAddress).add(event);
		event.setHasArrivedAtDestination(false);
	}

	public void writeHitSendMessage(long addr, Cache c) {
		DirectoryEntry dirEntry = getDirectoryEntry(addr);

		switch (dirEntry.getState()) {
			case MODIFIED: {
				if(dirEntry.getOwner()!=c) {
					// Following is a sequence of events which can cause this state : 
					// core1 writeMiss. core2 writeHit.
					// directory processes writeMiss of core1->invalidates core2's entry.
					if(c.access(addr)!=null) {
						misc.Error.showErrorAndExit("This cacheline was not expected here !!");
					} else {
						// case of writeMiss
						dirEntry.getCurrentEvent().setRequestType(RequestType.DirectoryWriteMiss);
						writeMissSendMessage(addr, c);
					}
				} else {
					unlock(addr, null);
				}
				
				break;
			}
	
			case EXCLUSIVE: {
				if(dirEntry.getOwner()!=c) {
					// Following is a sequence of events which can cause this state : 
					// core1 writeMiss. core2 readMiss. core2 evict. core3 writeHit
					// directory processes writeMiss of core1->invalidates core3's entry.
					// directory processes readMiss of core2->makes the entry shared.
					// directory processes evict of core2->makes the entry exclusive.
					if(c.access(addr)!=null) {
						misc.Error.showErrorAndExit("This cacheline was not expected here !!");
					} else {
						// case of writeMiss
						dirEntry.getCurrentEvent().setRequestType(RequestType.DirectoryWriteMiss);
						writeMissSendMessage(addr, c);
					}
				} else {
					unlock(addr, null);
				}
				
				break;
			}
	
			case SHARED: {
				if (dirEntry.isSharer(c) == false) {
					if(c.access(addr)!=null) {
						misc.Error.showErrorAndExit("This cacheline was not expected here !!");
					} else {
						// Following is a sequence of events which can cause this state : 
						// core1 writeMiss. core2 readMiss. core3 writeHit
						// directory processes writeMiss of core1->invalidates core3's entry.
						// directory processes readMiss of core2->makes the entry shared.
						dirEntry.getCurrentEvent().setRequestType(RequestType.DirectoryWriteMiss);
						writeMissSendMessage(addr, c);
					}
				} else {
					for (Cache sharerCache : dirEntry.getSharers()) {
						if (sharerCache != c) {
							sendInvalidateEventToCache(addr, c);
						}
					}
				}
	
				break;
			}
	
			case INVALID: {
				if(c.access(addr)!=null) {
					misc.Error.showErrorAndExit("This cacheline was not expected here !!");
				} else {
					// Following is a sequence of events which can cause this state : 
					// core1 writeMiss. core1 evict. core2 writeHit
					// directory processes writeMiss of core1->invalidates core2's entry.
					// directory processes evict of core1->makes the entry invalid.
					dirEntry.getCurrentEvent().setRequestType(RequestType.DirectoryWriteMiss);
					writeMissSendMessage(addr, c);
				}
				
				break;
			}
		}
	}

	private void sendInvalidateEventToCache(long addr, Cache c) {
		Directory directory = this;
		EventQueue eventQueue = c.getEventQueue();
		AddressCarryingEvent event = new AddressCarryingEvent(eventQueue,
				GlobalClock.getCurrentTime() + c.getLatency(), directory, c,
				RequestType.DirectoryInvalidate, addr);

		c.sendEvent(event);
	}

	public void writeHitStateChange(long addr, Cache c) {
		DirectoryEntry dirEntry = getDirectoryEntry(addr);

		switch (dirEntry.getState()) {
			case MODIFIED: {
				if (dirEntry.getOwner() != c) {
					misc.Error.showErrorAndExit("Trying to modify a line owned by another cache. Cache : " + c + ". Present Owner : " + dirEntry.getOwner());
				}
	
				break;
			}
	
			case EXCLUSIVE: {
				if (dirEntry.getOwner() != c) {
					misc.Error.showErrorAndExit("Changing cacheline state from exclusive to modified. However this is not its owner cache : " + c);
				}
	
				dirEntry.setState(MESI.MODIFIED);
				c.updateStateOfCacheLine(addr, MESI.MODIFIED);
				break;
			}
	
			case SHARED: {
				for (Cache sharerCache : dirEntry.getSharers()) {
					if (sharerCache != c) {
						sharerCache.updateStateOfCacheLine(addr, MESI.INVALID);
					}
				}
	
				dirEntry.clearAllSharers();
				dirEntry.setState(MESI.MODIFIED);
				dirEntry.addSharer(c);
				c.updateStateOfCacheLine(addr, MESI.MODIFIED);
				break;
			}
	
			case INVALID: {
				misc.Error.showErrorAndExit("Trying to modify an invalid cacheline. Cache : " + c);
				break;
			}
		}
	}

	public long getSetAddress(long addr) {
		return getSetIdx(addr);
	}

	public void unlock(long addr, Cache c) {
		DirectoryEntry dirEntry = getDirectoryEntry(addr);
		if(dirEntry==null) {
			misc.Error.showErrorAndExit("Calling unlock function for an invalid directory entry. Address : " + addr + ". Cache : " + c);
		}

		if (c != null) {
			if (dirEntry.getListOfAwaitedCacheResponses().contains(c)) {
				dirEntry.removeCacheFromAwaitedCacheList(c);
			} else {
				misc.Error.showErrorAndExit("No response expected from this cache : "	+ c);
			}
		}

		if (dirEntry.getListOfAwaitedCacheResponses().isEmpty() == true) {
			callStateChangeFunction(dirEntry.getCurrentEvent());
			dirEntry.setCurrentEvent(null);

			AddressCarryingEvent nextEvent = getPendingEventToProcess(addr);
			if (nextEvent != null) {
				handleEvent(nextEvent.getEventQ(), nextEvent);
			}
		}
	}

	private void callStateChangeFunction(AddressCarryingEvent currentEvent) {
		long addr = currentEvent.getAddress();
		Cache c = (Cache) currentEvent.getRequestingElement();
		switch(currentEvent.getRequestType()) {
			case DirectoryWriteHit: {
				writeHitStateChange(addr, c);
				break;
			}
			
			case DirectoryReadMiss: {
				readMissStateChange(addr, c);
				break;
			}
			
			case DirectoryWriteMiss: {
				writeMissStateChange(addr, c);
				break;
			}
			
			case DirectoryEvictedFromSharedCache: {
				evictedFromSharedCacheStateChange(addr, c);
				break;
			}
			
			case DirectoryEvictedFromCoherentCache: {
				evictedFromCoherentCacheStateChange(addr, c);
				break;
			}
		}
	}

	/*
	 * Returns the first event which satisfies these properties : 
	 * (a) has reached the directory
	 * (b) belongs to the same lineAddr as that of addr or belongs to a line which is not present in the set
	 */
	private AddressCarryingEvent getPendingEventToProcess(long addr) {
		long myLineAddr = getLineAddress(addr);
		int setAddr = (int) getSetAddress(addr);

		for (AddressCarryingEvent event : pendingEvents.get(setAddr)) {
			long nextAddr = event.getAddress();
			long nextLineAddr = getLineAddress(nextAddr);
			DirectoryEntry dirEntry = getDirectoryEntry(nextLineAddr);
			boolean validDirEntry = dirEntry!=null && dirEntry.isValid()==true;
			
			if (myLineAddr!=nextLineAddr && validDirEntry) {
				// Do nothing
			} else if (event.hasArrivedAtDestination()) {
				return event;
			}
		}
		
		return null;
	}

	private long getLineAddress(long addr) {
		return addr >>> blockSizeBits;
	}
	
	public DirectoryEntry accessDir(long addr) {
		/* compute startIdx and the tag */
		int startIdx = getStartIdx(addr);
		long tag = computeTag(addr);
		
		/* search in a set */
		for(int idx = 0; idx < assoc; idx++) 
		{
			// calculate the index
			int index = getNextIdx(startIdx,idx);
			// fetch the cache line
			DirectoryEntry ll = this.lines[index];
			// If the tag is matching, we have a hit
			if(ll.hasTagMatch(tag)){
				return  ll;
			}
		}
		return null;
	}
	
	public long computeTag(long addr) {
		long tag = addr >>> (numSetsBits + blockSizeBits);
		return tag;
	}

	private DirectoryEntry getDirectoryEntry(long addr) {
		DirectoryEntry dirEntry = (DirectoryEntry) accessDir(addr);
		return dirEntry;
	}
	
	public void evictedFromSharedCache(long addr, Cache c) {
		evictedFromSharedCacheAccesses++;
		sendAnEventFromCacheToDirectoryAndAddToPendingList(addr, c, RequestType.DirectoryEvictedFromSharedCache);
	}
	
	public void evictedFromCoherentCache(long addr, Cache c) {
		evictedFromCoherentCacheAccesses++;
		sendAnEventFromCacheToDirectoryAndAddToPendingList(addr, c, RequestType.DirectoryEvictedFromCoherentCache);
	}
	
	private void evictedFromSharedCacheSendMessage(long addr, Cache c) {
		DirectoryEntry dirEntry = getDirectoryEntry(addr);
		
		switch(dirEntry.getState()) {
			case MODIFIED:
			case EXCLUSIVE:
			case SHARED:
			{
				sendInvalidateForDirectoryEntry(dirEntry);
				break;
			}
			
			case INVALID: {
				unlock(addr, null);
				break;
			}
		}
	}
	
	private void evictedFromSharedCacheStateChange(long addr, Cache c) {
		DirectoryEntry dirEntry = getDirectoryEntry(addr);
		
		switch(dirEntry.getState()) {
			case MODIFIED:
			case EXCLUSIVE:
			case SHARED:
			{
				for(Cache sharer : dirEntry.getSharers()) {
					sharer.updateStateOfCacheLine(addr, MESI.INVALID);
				}
				dirEntry.clearAllSharers();
				dirEntry.setState(MESI.INVALID);
				break;
			}
			
			case INVALID: {
				break;
			}
		}
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event e) {
		AddressCarryingEvent event = (AddressCarryingEvent) e;
		event.setHasArrivedAtDestination(true);
		long addr = event.getAddress();

		if (canProcess(addr, event) == false) {
			return;
		}
		
		// Lock the directory entry
		DirectoryEntry dirEntry = getDirectoryEntry(addr);
		dirEntry.setCurrentEvent(event);
		
		// Remove this entry from the pending events queue
		pendingEvents.get(getSetIdx(addr)).remove(event);

		Cache senderCache = (Cache) event.getRequestingElement();

		switch (event.getRequestType()) {
			case DirectoryWriteHit: {
				writeHitSendMessage(addr, senderCache);
				break;
			}
			
			case DirectoryReadMiss: {
				readMissSendMessage(addr, senderCache);
				break;
			}
			
			case DirectoryWriteMiss: {
				writeMissSendMessage(addr, senderCache);
				break;
			}
			
			case DirectoryEvictedFromSharedCache: {
				evictedFromSharedCacheSendMessage(addr, senderCache);
				break;
			}
			
			case DirectoryEvictedFromCoherentCache: {
				evictedFromCoherentCacheSendMessage(addr, senderCache);
				break;
			}
		}
	}

	private void readMissSendMessage(long addr, Cache c) {
		DirectoryEntry dirEntry = getDirectoryEntry(addr);
		
		switch(dirEntry.getState()) {
			case MODIFIED: 
			case EXCLUSIVE: {
				
				if(dirEntry.getOwner()==c) {
					if(c.access(addr)==null) {
						misc.Error.showErrorAndExit("Invalid state");
					} else {
						// Following is a sequence of events which can cause this state : 
						// core1 read, read in the same cycle. This will lead to two readMisses
						// For the second readMiss, we will find the directory entry to contain the same cache as the owner cache
						unlock(addr, null);
					}
				} else {
					dirEntry.addCacheToAwaitedCacheList(c);
					sendCachelineForwardRequest(dirEntry.getOwner(), c, addr);
				}
				
				break;
			}
			
			case SHARED: {
				if(dirEntry.getSharers().contains(c)) {
					if(c.access(addr)==null) {
						misc.Error.showErrorAndExit("Invalid state");
					} else {
						// Following is a sequence of events which can cause this state : 
						// core1 read, read in the same cycle. This will lead to two readMisses
						// For the second readMiss, we will find the directory entry to contain the same cache as one of the sharer cache
						unlock(addr, null);
					}
				} else {
					dirEntry.addCacheToAwaitedCacheList(c);
					sendCachelineForwardRequest(dirEntry.getFirstSharer(), c, addr);
				}

				break;
			}
			
			case INVALID: {
				dirEntry.addCacheToAwaitedCacheList(c);
				// If the line is supposed to be fetched from the next level cache, 
				// we will just send a cacheRead request to this cache
				c.sendRequestToNextLevel(addr, RequestType.Cache_Read);
				break;
			}
		}
	}
	
	private void writeMissSendMessage(long addr, Cache c) {
		DirectoryEntry dirEntry = getDirectoryEntry(addr);
		
		switch(dirEntry.getState()) {
			case MODIFIED: 
			case EXCLUSIVE: {
				
				if(dirEntry.getOwner()==c) {
					if(c.access(addr)==null) {
						misc.Error.showErrorAndExit("Invalid state");
					} else {
						// Following is a sequence of events which can cause this state : 
						// core1 write, write in the same cycle. This will lead to two writeMisses
						// For the second writeMiss, we will find the directory entry to contain the same cache as the owner cache
						unlock(addr, null);
					}
				} else {
					dirEntry.addCacheToAwaitedCacheList(c);
					sendCachelineForwardRequest(dirEntry.getOwner(), c, addr);					
				}
				
				break;
			}
			
			case SHARED: {
				if(dirEntry.getSharers().contains(c)) {
					if(c.access(addr)==null) {
						misc.Error.showErrorAndExit("Invalid state");
					} else {
						// Following is a sequence of events which can cause this state : 
						// core1 read, write in the same cycle. This will lead to two writeMisses
						// For the writeMiss, we will find the directory entry to contain the same cache as one of the sharer cache
						unlock(addr, null);
					}
				} else {
					sendInvalidateForDirectoryEntry(dirEntry);
					dirEntry.addCacheToAwaitedCacheList(c);
					sendCachelineForwardRequest(dirEntry.getFirstSharer(), c, addr);
				}

				break;
			}
			
			case INVALID: {
				dirEntry.addCacheToAwaitedCacheList(c);
				// If the line is supposed to be fetched from the next level cache, 
				// we will just send a cacheRead request to this cache
				c.sendRequestToNextLevel(addr, RequestType.Cache_Read);

				break;
			}
		}
	}

	
	private void sendCachelineForwardRequest(Cache ownerCache, Cache destinationCache, long addr) {
		EventQueue eventQueue = ownerCache.getEventQueue();
		
		AddressCarryingEvent event = new AddressCarryingEvent(eventQueue, 0, 
			this, ownerCache, 
			RequestType.DirectoryCachelineForwardRequest, addr);
		
		event.payloadElement = destinationCache;
		
		this.sendEvent(event);
	}

	private void readMissStateChange(long addr, Cache c) {
		DirectoryEntry dirEntry = getDirectoryEntry(addr);
		
		switch(dirEntry.getState()) {
			case MODIFIED: 
			case EXCLUSIVE: {
				dirEntry.getOwner().updateStateOfCacheLine(addr, MESI.SHARED);
				c.updateStateOfCacheLine(addr, MESI.SHARED);
				dirEntry.setState(MESI.SHARED);
				dirEntry.addSharer(c);
				break;
			}
			
			case SHARED: {
				c.updateStateOfCacheLine(addr, MESI.SHARED);
				dirEntry.addSharer(c);
				break;
			}
			
			case INVALID: {
				c.updateStateOfCacheLine(addr, MESI.EXCLUSIVE);
				dirEntry.setState(MESI.EXCLUSIVE);
				dirEntry.addSharer(c);
				break;
			}
		}		
	}
	
	private void writeMissStateChange(long addr, Cache c) {
		DirectoryEntry dirEntry = getDirectoryEntry(addr);
		
		switch(dirEntry.getState()) {
			case MODIFIED: 
			case EXCLUSIVE: {
				if(dirEntry.getOwner()==c) {
					if(c.access(addr)==null) {
						misc.Error.showErrorAndExit("");
					}
				} else {
					dirEntry.getOwner().updateStateOfCacheLine(addr, MESI.INVALID);
					c.updateStateOfCacheLine(addr, MESI.MODIFIED);
					dirEntry.removeSharer(dirEntry.getOwner());
					dirEntry.setState(MESI.MODIFIED);
					dirEntry.addSharer(c);					
				}
				break;
			}
			
			case SHARED: {
				c.updateStateOfCacheLine(addr, MESI.MODIFIED);
				for(Cache sharer : dirEntry.getSharers()) {
					sharer.updateStateOfCacheLine(addr, MESI.INVALID);
				}
				dirEntry.clearAllSharers();
				dirEntry.setState(MESI.MODIFIED);
				dirEntry.addSharer(c);
				break;
			}
			
			case INVALID: {
				c.updateStateOfCacheLine(addr, MESI.MODIFIED);
				dirEntry.setState(MESI.MODIFIED);
				dirEntry.addSharer(c);
				break;
			}
		}		
	}
	
	private void evictedFromCoherentCacheSendMessage(long addr, Cache c) {
		DirectoryEntry dirEntry = getDirectoryEntry(addr);
		
		switch(dirEntry.getState()) {
			case MODIFIED:
			case EXCLUSIVE: {
				if(dirEntry.getOwner()!=c) {
					if(c.access(addr)!=null) {
						misc.Error.showErrorAndExit("Invalid state");
					} else {
						// Following is a sequence of events which can cause this state : 
						// core1 writeMiss. core2 evict.
						// directory processes writeMiss of core1->invalidates core2's entry.
						unlock(addr, null);
					}
				} else {
					unlock(addr, null);
				}
				
				break;
			}
			
			case SHARED: {
				if(dirEntry.getSharers().contains(c)==false) {
					if(c.access(addr)!=null) {
						misc.Error.showErrorAndExit("Invalid state");
					} else {
						// Following is a sequence of events which can cause this state : 
						// core1 readMiss. core2 evict.
						// directory processes readMiss of core1->makes cache line shared.
						unlock(addr, null);
					}
				} else {
					if(dirEntry.getSharers().size()==2) {
						for(Cache sharer : dirEntry.getSharers()) {
							if(sharer!=c) {
								dirEntry.addCacheToAwaitedCacheList(sharer);
								sendAnEventFromDirectoryToCache(addr, sharer, RequestType.DirectorySharedToExclusive);
								break;
							}
						}
					} else {
						unlock(addr, null);
					}
				}
				
				break;
			}
			
			case INVALID: {
				if(c.access(addr)!=null) {
					misc.Error.showErrorAndExit("Invalid state");
				} else {
					// Following is a sequence of events which can cause this state : 
					// sharedCache evict from shared cache. core2 evict.
					// directory processes evictFromSharedCache->invalidates core2's entry.
					unlock(addr, null);
				}
				break;
			}
		}	
	}
	
	private void evictedFromCoherentCacheStateChange(long addr, Cache c) {
		DirectoryEntry dirEntry = getDirectoryEntry(addr);
		
		switch(dirEntry.getState()) {
			case MODIFIED:
			case EXCLUSIVE: {
				dirEntry.getOwner().updateStateOfCacheLine(addr, MESI.INVALID);
				dirEntry.clearAllSharers();
				dirEntry.setState(MESI.INVALID);
				
				break;
			}
			
			case SHARED: {
				c.updateStateOfCacheLine(addr, MESI.INVALID);
				dirEntry.removeSharer(c);
				if(dirEntry.getSharers().size()==1) {
					dirEntry.setState(MESI.EXCLUSIVE);
					dirEntry.getOwner().updateStateOfCacheLine(addr, MESI.EXCLUSIVE);
				} else if (dirEntry.getSharers().size()==0) {
					dirEntry.setState(MESI.INVALID);					
				}
				
				break;
			}
			
			case INVALID: {
				misc.Error.showErrorAndExit("Invalid state");
				break;
			}
		}
		
	}
	
	private boolean canProcess(long addr, AddressCarryingEvent event) {
		
		if(isThereAPendingEventForTheSameLineBeforeMe(event)) {
			return false;
		}

		DirectoryEntry dirEntry = getDirectoryEntry(addr);
		if (dirEntry != null) {
			if (dirEntry.isLocked() == false) {
				// The directory entry is present and its unlocked right now
				return true;
			} else {
				return false;
			}
		} else {
			// If there is an invalid entry in the cache, then use this directory entry
			if (isThereAnInvalidEntryInCacheSet(addr)) {
				DirectoryEntry evictedEntry = (DirectoryEntry) this.fillDir(addr);
				return true;
			}
			
			if (isThereAnUnlockedEntryInCacheSet(addr)) {
				DirectoryEntry evictedEntry = getLRUUnlockedEntry(addr);
				evictedEntry.setCurrentEvent(getInvalidateEventForAddr(evictedEntry.getAddress()));
				sendInvalidateForDirectoryEntry(evictedEntry);
				return false;
			} else {
				return false;
			}
		}
	}

	private AddressCarryingEvent getInvalidateEventForAddr(long addr) {
		AddressCarryingEvent event =  new AddressCarryingEvent();
		event.setAddress(addr);
		event.setRequestType(RequestType.DirectoryInvalidate);
		return event;
	}

	private DirectoryEntry getLRUUnlockedEntry(long addr) {
		/* compute startIdx and the tag */
		int startIdx = getStartIdx(addr);
		long tag = computeTag(addr); 
		/* find any invalid lines -- no eviction */
		DirectoryEntry fillLine = null;
				
		for (int idx = 0; idx < assoc; idx++) 
		{
			int nextIdx = getNextIdx(startIdx, idx);
			DirectoryEntry ll = this.lines[nextIdx];
			if (ll.getTag() == tag && ll.getState() != MESI.INVALID) 
			{	
				misc.Error.showErrorAndExit("attempting fillDir for a line already present. address : " + addr);
			}
			if (!(ll.isLocked())) 
			{
				if(fillLine == null)
				{
					fillLine = ll;
				}
				else if(fillLine.getTimestamp() > ll.getTimestamp())
				{
					fillLine = ll;
				}
			}
		}
		
		return fillLine;
	}

	private boolean isThereAnInvalidEntryInCacheSet(long addr) {
		/* compute startIdx and the tag */
		int startIdx = getStartIdx(addr);
		
		/* search in a set */
		for(int idx = 0; idx < assoc; idx++) 
		{
			// calculate the index
			int index = getNextIdx(startIdx,idx);
			// fetch the cache line
			DirectoryEntry ll = lines[index];
			// If the tag is matching, we have a hit
			if(ll.getState() == MESI.INVALID) {
				return  true;
			}
		}
		return false;
	}
	
	private boolean isThereAnUnlockedEntryInCacheSet(long addr) {
		/* compute startIdx and the tag */
		int startIdx = getStartIdx(addr);
		
		/* search in a set */
		for(int idx = 0; idx < assoc; idx++) 
		{
			// calculate the index
			int index = getNextIdx(startIdx,idx);
			// fetch the cache line
			DirectoryEntry ll = lines[index];
			// If the tag is matching, we have a hit
			if(ll.isLocked() == true) {
				return  true;
			}
		}
		return false;
	}

	private boolean isThereAPendingEventForTheSameLineBeforeMe(
			AddressCarryingEvent event) {
		long addr = event.getAddress();
		int setAddr = (int) getSetAddress(addr);
		long lineAddr = getLineAddress(addr);
		
		for(AddressCarryingEvent e : pendingEvents.get(setAddr)) {
			if(e==event) {
				// reached at my point. So I am the first event for this line
				return false;
			}
			
			long newAddr = e.getAddress();
			long newLineAddr = getLineAddress(newAddr);
			if(lineAddr==newLineAddr) {
				return true;
			}
		}
		
		misc.Error.showErrorAndExit("Unholy mess !!");
		return true;
	}

	public DirectoryEntry fillDir(long addr) //Returns a copy of the evicted line
	{
		/* compute startIdx and the tag */
		int startIdx = getStartIdx(addr);
		long tag = computeTag(addr); 
		/* find any invalid lines -- no eviction */
		DirectoryEntry fillLine = null;
		
		for (int idx = 0; idx < assoc; idx++) 
		{
			int nextIdx = getNextIdx(startIdx, idx);
			DirectoryEntry ll = lines[nextIdx];
			if (ll.getTag() == tag && ll.getState() != MESI.INVALID) 
			{	
				misc.Error.showErrorAndExit("attempting fillDir for a line already present. address : " + addr);
			}
			if (!(ll.isValid())) 
			{
				fillLine = ll;
				fillLine.setState(MESI.INVALID);
				fillLine.setAddress(addr);
				mark(fillLine, tag);
				return fillLine;
			}
		}
		
		misc.Error.showErrorAndExit("no invalid entry found in fillDir()");
		return null;
	}

	public void printStatistics(FileWriter outputFileWriter) throws IOException {
		outputFileWriter.write("\n");
		outputFileWriter.write("Directory Access due to ReadMiss\t=\t" + readMissAccesses + "\n");
		outputFileWriter.write("Directory Access due to WriteMiss\t=\t" + writeMissAccesses + "\n");
		outputFileWriter.write("Directory Access due to WriteHit\t=\t" + writeHitAccesses + "\n");
		outputFileWriter.write("Directory Access due to EvictionFromCoherentCache\t=\t" + evictedFromCoherentCacheAccesses + "\n");
		outputFileWriter.write("Directory Access due to EvictionFromSharedCache\t=\t" + evictedFromSharedCacheAccesses + "\n");
		
		outputFileWriter.write("Directory Hits\t=\t" + hits + "\n");
		outputFileWriter.write("Directory Misses\t=\t" + misses + "\n");
		if ((hits+misses) != 0) {
			outputFileWriter.write("Directory Hit-Rate\t=\t" + Statistics.formatDouble((double)(hits)/(hits+misses)) + "\n");
			outputFileWriter.write("Directory Miss-Rate\t=\t" + Statistics.formatDouble((double)(misses)/(hits+misses)) + "\n");
		}
	}
	
	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException
	{
		long numAccesses = readMissAccesses + writeHitAccesses + writeMissAccesses 
				+ evictedFromCoherentCacheAccesses + evictedFromSharedCacheAccesses;
		EnergyConfig newPower = new EnergyConfig(SystemConfig.directoryConfig.power.leakageEnergy,
				SystemConfig.directoryConfig.power.readDynamicEnergy);
		EnergyConfig power = new EnergyConfig(newPower, numAccesses);
		power.printEnergyStats(outputFileWriter, componentName);
		return power;
	}
	
	public void sendEvent(AddressCarryingEvent event) {
		if(event.getEventTime()!=0) {
			misc.Error.showErrorAndExit("Send event with zero latency !!");
		}
		
		if(event.getProcessingElement().getComInterface()!=this.getComInterface()) {
			getComInterface().sendMessage(event);
		} else {
			event.getProcessingElement().getPort().put(event);
		}
	}
}
