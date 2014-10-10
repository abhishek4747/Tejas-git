package memorysystem.coherence;

import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.RequestType;
import generic.Statistics;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import main.ArchitecturalComponent;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CacheLine;
import memorysystem.CoreMemorySystem;
import memorysystem.MESI;
import memorysystem.MemorySystem;
import misc.Util;
import config.CacheConfig;
import config.EnergyConfig;
import config.SystemConfig;

// Unlock function should call the state change function. This is called using the current event field inside the directory entry.
// For write hit event, there is some mismatch

public class Directory extends Cache implements Coherence {
	
	long readMissAccesses = 0;
	long writeHitAccesses = 0;
	long writeMissAccesses = 0;
	long evictedFromCoherentCacheAccesses = 0;
	long evictedFromSharedCacheAccesses = 0;

	public Directory(String cacheName, int id, CacheConfig cacheParameters,
			CoreMemorySystem containingMemSys) {		
		super(cacheName, id, cacheParameters, containingMemSys);		
		MemorySystem.coherenceNameMappings.put(cacheName, this);
	}

	public void writeHit(long addr, Cache c) {
//		XXX if(ArchitecturalComponent.getCore(0).getNoOfInstructionsExecuted()>3000000l) {
//			System.out.println("Directory WriteHit t : " + GlobalClock.getCurrentTime() + " addr : " + addr + " cache : " + c);
//		}
		writeHitAccesses++;
		sendAnEventFromCacheToDirectoryAndAddToPendingList(addr, c, RequestType.DirectoryWriteHit);
	}

	public void readMiss(long addr, Cache c) {
//		XXX if(ArchitecturalComponent.getCore(0).getNoOfInstructionsExecuted()>3000000l) {
//			System.out.println("Directory ReadMiss t : " + GlobalClock.getCurrentTime() + " addr : " + addr + " cache : " + c);
//		}
		readMissAccesses++;
		sendAnEventFromCacheToDirectoryAndAddToPendingList(addr, c, RequestType.DirectoryReadMiss);
	}

	public void writeMiss(long addr, Cache c) {
//		XXX if(ArchitecturalComponent.getCore(0).getNoOfInstructionsExecuted()>3000000l) {
//			System.out.println("Directory WriteMiss t : " + GlobalClock.getCurrentTime() + " addr : " + addr + " cache : " + c);
//		}
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
		CacheLine dirEntry = access(addr);
		
		if(dirEntry==null || dirEntry.getState()==MESI.INVALID) {
			misses++;
		} else {
			hits++;
		}		
	}

	public void writeHitSendMessage(long addr, Cache c) {
		CacheLine dirEntry = access(addr);

		switch (dirEntry.getState()) {
			case MODIFIED: {
				unlock(addr, null);
				break;
			}
	
			case EXCLUSIVE: {
				unlock(addr, null);
				break;
			}
	
			case SHARED: {
				if (dirEntry.isSharer(c) == false) {
					c.fillAndSatisfyRequests(addr); // XXX change
					unlock(addr, null); // Invalid directory state
				} else {
					for (Cache sharerCache : dirEntry.getSharers()) {
						if (sharerCache != c) {
							sendAnEventFromMeToCache(addr, sharerCache, RequestType.EvictCacheLine);
							dirEntry.addCacheToAwaitedCacheList(sharerCache);
						}
					}
				}
	
				break;
			}
	
			case INVALID: {
				unlock(addr, null); // Invalid directory state
				break;
			}
		}
	}

	public void writeStateChange(long addr, Cache c) {
		CacheLine dirEntry = access(addr);
		
		if(c.access(addr)==null) {
			forceInvalidate(dirEntry);
		} else {
			for (Cache sharerCache : dirEntry.getSharers()) {
				if (sharerCache != c) {
					sharerCache.updateStateOfCacheLine(addr, MESI.INVALID);
				}
			}
			
			dirEntry.clearAllSharers();
			dirEntry.setState(MESI.MODIFIED);
			dirEntry.addSharer(c);
			c.updateStateOfCacheLine(addr, MESI.MODIFIED);
		}
	}

	private void forceInvalidate(CacheLine dirEntry) {
		misc.Error.showErrorAndExit("Force Invalidate !!");
		// The directory is in an inconsistent state. 
		// Force a consistent change by evicting the dirEntry.
		for (Cache sharerCache : dirEntry.getSharers()) {
			sharerCache.updateStateOfCacheLine(dirEntry.getAddress(), MESI.INVALID);
		}
		
		dirEntry.clearAllSharers();
		dirEntry.setState(MESI.INVALID);		
	}

	public void callStateChangeFunction(AddressCarryingEvent currentEvent) {
		long addr = currentEvent.getAddress();
		Cache c = (Cache) currentEvent.getRequestingElement();
				
		switch(currentEvent.getRequestType()) {
			case DirectoryWriteHit: {
				writeStateChange(addr, c);
				break;
			}
			
			case DirectoryReadMiss: {
				readMissStateChange(addr, c);
				break;
			}
			
			case DirectoryWriteMiss: {
				writeStateChange(addr, c);
				break;
			}
			
			case EvictCacheLine:
			case DirectoryEvictedFromSharedCache: {
				evictedFromSharedCacheStateChange(addr, c);
				break;
			}
			
			case DirectoryEvictedFromCoherentCache: {
				evictedFromCoherentCacheStateChange(addr, c);
				break;
			}
		}
		
		if(currentEvent.getRequestingElement()!=null) {
			//requesting element would be null in the directory line eviction scenario
			((Cache)currentEvent.getRequestingElement()).unlock(addr, null);			
		}
	}

	public void evictedFromSharedCache(long addr, Cache c) {
//		XXX if(ArchitecturalComponent.getCore(0).getNoOfInstructionsExecuted()>3000000l) {
//			System.out.println("Directory EvictShared t : " + GlobalClock.getCurrentTime() + " addr : " + addr + " cache : " + c);
//		}
		evictedFromSharedCacheAccesses++;
		sendAnEventFromCacheToDirectoryAndAddToPendingList(addr, c, RequestType.DirectoryEvictedFromSharedCache);
	}
	
	public void evictedFromCoherentCache(long addr, Cache c) {
//		XXX if(ArchitecturalComponent.getCore(0).getNoOfInstructionsExecuted()>3000000l) {
//			System.out.println("Directory EvictCoherent t : " + GlobalClock.getCurrentTime() + " addr : " + addr + " cache : " + c);
//		}
		evictedFromCoherentCacheAccesses++;
		sendAnEventFromCacheToDirectoryAndAddToPendingList(addr, c, RequestType.DirectoryEvictedFromCoherentCache);
	}
	
	private void evictedFromSharedCacheSendMessage(long addr, Cache c) {
		CacheLine dirEntry = access(addr);
		
		switch(dirEntry.getState()) {
			case MODIFIED:
			case EXCLUSIVE:
			case SHARED:
			{
				evictCacheLine(dirEntry);
				break;
			}
			
			case INVALID: {
				unlock(addr, null);
				break;
			}
		}
	}
	
	private void evictedFromSharedCacheStateChange(long addr, Cache c) {
		CacheLine dirEntry = access(addr);
		
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
		long addr = event.getAddress();

//		if(ArchitecturalComponent.getCores()[0].getNoOfInstructionsExecuted() > 4000000) {
//			System.out.println("\n\nDirectory handleEvent currEvent : " + event);
//			toStringPendingEvents();
//		}
		
		if(e.serializationID==17532037) {
			System.out.println("culprint");
		}
		
		if(lockCacheLineAndRemovePendingEvent(event)==false) {
			return;
		}
		
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
	
	protected void evictCacheLine(CacheLine evictedEntry) {
		long addr = evictedEntry.getAddress();
		
		if(evictedEntry.getListOfAwaitedCacheResponses().size()!=0) {
			misc.Error.showErrorAndExit("Cannot invalidate an entry which is already expecting cache responses");
		}
		
		for(Cache c : evictedEntry.getSharers()) {
			evictedEntry.addCacheToAwaitedCacheList(c);
			sendAnEventFromMeToCache(addr, c, RequestType.EvictCacheLine);
		}
	}

	private void readMissSendMessage(long addr, Cache c) {
		CacheLine dirEntry = access(addr);
		
		switch(dirEntry.getState()) {
			case MODIFIED: 
			case EXCLUSIVE: {
				
				if(dirEntry.getOwner()==c) {
					c.fillAndSatisfyRequests(addr); // XXX change
					unlock(addr, null); // Invalid directory state
				} else {
					dirEntry.addCacheToAwaitedCacheList(c);
					sendCachelineForwardRequest(dirEntry.getOwner(), c, addr);
				}
				
				break;
			}
			
			case SHARED: {
				if(dirEntry.getSharers().contains(c)) {
					c.fillAndSatisfyRequests(addr); // XXX change
					unlock(addr, null); // Invalid directory state
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
				// Note that the directory is not coming into the picture. This is just a minor hack to maintain readability of the code
				c.sendRequestToNextLevel(addr, RequestType.Cache_Read);
				break;
			}
		}
	}
	
	private void writeMissSendMessage(long addr, Cache c) {
		CacheLine dirEntry = access(addr);
		
		switch(dirEntry.getState()) {
			case MODIFIED: 
			case EXCLUSIVE: {
				
				if(dirEntry.getOwner()==c) {
					c.fillAndSatisfyRequests(addr); // XXX change
					unlock(addr, null); // Invalid directory state
				} else {
					dirEntry.addCacheToAwaitedCacheList(c);
					sendCachelineForwardRequest(dirEntry.getOwner(), c, addr);					
				}
				
				break;
			}
			
			case SHARED: {
				if(dirEntry.getSharers().contains(c)) {
					c.fillAndSatisfyRequests(addr); // XXX change
					unlock(addr, null); // Invalid directory state
				} else {
					evictCacheLine(dirEntry);
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
		CacheLine dirEntry = access(addr);
		
		if(c.access(addr)==null) {
			forceInvalidate(dirEntry);
		} else {			
			switch(dirEntry.getState()) {
				case MODIFIED: 
				case EXCLUSIVE: {
					if(dirEntry.getSharers().contains(c) == false) {
						dirEntry.getOwner().updateStateOfCacheLine(addr, MESI.SHARED);
						c.updateStateOfCacheLine(addr, MESI.SHARED);
						dirEntry.setState(MESI.SHARED);
						dirEntry.addSharer(c);
					}
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
	}
	
	private void evictedFromCoherentCacheSendMessage(long addr, Cache c) {
		CacheLine dirEntry = access(addr);
		
		switch(dirEntry.getState()) {
			case MODIFIED:
			case EXCLUSIVE: {
				if(dirEntry.getOwner()!=c) {
					unlock(addr, null); // Invalid directory state
				} else {
					unlock(addr, null);
				}
				
				break;
			}
			
			case SHARED: {
				if(dirEntry.getSharers().contains(c)==false) {
					unlock(addr, null); // Invalid directory state
				} else {
					if(dirEntry.getSharers().size()==2) {
						for(Cache sharer : dirEntry.getSharers()) {
							if(sharer!=c) {
								dirEntry.addCacheToAwaitedCacheList(sharer);
								sendAnEventFromMeToCache(addr, sharer, RequestType.DirectorySharedToExclusive);
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
				unlock(addr, null); // Invalid directory state
				break;
			}
		}	
	}
	
	private void evictedFromCoherentCacheStateChange(long addr, Cache c) {
		CacheLine dirEntry = access(addr);
		
		switch(dirEntry.getState()) {
			case MODIFIED:
			case EXCLUSIVE: {
				// Directory need not call unlock of the cache. The state change function of cache should do the needful.
				dirEntry.clearAllSharers();
				dirEntry.setState(MESI.INVALID);
				
				break;
			}
			
			case SHARED: {
				// Directory need not call unlock of the cache. The state change function of cache should do the needful.
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
				break;
			}
		}
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
}
