package memorysystem.coherence;

import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.RequestType;
import generic.SimulationElement;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import com.sun.org.apache.xpath.internal.functions.FuncUnparsedEntityURI;

import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CacheLine;
import memorysystem.MESI;
import memorysystem.MemorySystem;

// Unlock function should call the state change function. This is called using the current event field inside the directory entry.
// For write hit event, there is some mismatch

public class Directory extends Cache implements Coherence {

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
		sendAnEventFromCacheToDirectoryAndAddToPendingList(addr, c, RequestType.DirectoryWriteHit);
	}

	public void readMiss(long addr, Cache c) {
		sendAnEventFromCacheToDirectoryAndAddToPendingList(addr, c, RequestType.DirectoryReadMiss);
	}

	public void writeMiss(long addr, Cache c) {
		sendAnEventFromCacheToDirectoryAndAddToPendingList(addr, c, RequestType.DirectoryWriteMiss);
	}

	private void sendAnEventFromCacheToDirectoryAndAddToPendingList
		(long addr, Cache c, RequestType request) {
		// Create an event
		Directory directory = this;
		AddressCarryingEvent event = new AddressCarryingEvent(
				c.getEventQueue(), GlobalClock.getCurrentTime()
						+ directory.getLatency(), c, directory, request, addr);

		// 2. Send event to directory
		c.sendEvent(event);
		
		addPendingEvent(event);
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
		return addr >>> blockSizeBits;
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

	private DirectoryEntry getDirectoryEntry(long addr) {
		DirectoryEntry dirEntry = (DirectoryEntry) access(addr);
		return dirEntry;
	}
	
	public void evictedFromSharedCache(long addr, Cache c) {
		sendAnEventFromCacheToDirectoryAndAddToPendingList(addr, c, RequestType.DirectoryEvictedFromSharedCache);
	}
	
	public void evictedFromCoherentCache(long addr, Cache c) {
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
					misc.Error.showErrorAndExit("ReadMiss cannot be called from an owner cache");
				} else {
					dirEntry.addCacheToAwaitedCacheList(c);
					sendCachelineForwardRequest(dirEntry.getOwner(), c, addr);
				}
				
				break;
			}
			
			case SHARED: {
				if(dirEntry.getSharers().contains(c)) {
					misc.Error.showErrorAndExit("ReadMiss cannot be called from a sharer cache");
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
					misc.Error.showErrorAndExit("WriteMiss cannot be called from an owner cache");
				} else {
					dirEntry.addCacheToAwaitedCacheList(c);
					sendCachelineForwardRequest(dirEntry.getOwner(), c, addr);					
				}
				
				break;
			}
			
			case SHARED: {
				if(dirEntry.getSharers().contains(c)) {
					misc.Error.showErrorAndExit("WriteMiss cannot be called from a sharer cache");
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
					misc.Error.showErrorAndExit("");
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
					misc.Error.showErrorAndExit("Invalid state");					
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
				DirectoryEntry evictedEntry = this.fillDir(addr, MESI.INVALID);
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
			DirectoryEntry ll = (DirectoryEntry)getCacheLine(nextIdx);
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
			CacheLine ll = getCacheLine(index);
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
			DirectoryEntry ll = (DirectoryEntry) getCacheLine(index);
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

	private DirectoryEntry fillDir(long addr, MESI stateToSet) {
		// TODO Auto-generated method stub
		return null;
	}

	public CacheLine fill(long addr) //Returns a copy of the evicted line
	{
		/* compute startIdx and the tag */
		int startIdx = getStartIdx(addr);
		long tag = computeTag(addr); 
		/* find any invalid lines -- no eviction */
		DirectoryEntry fillLine = null;
		
		for (int idx = 0; idx < assoc; idx++) 
		{
			int nextIdx = getNextIdx(startIdx, idx);
			DirectoryEntry ll = (DirectoryEntry)getCacheLine(nextIdx);
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
}
