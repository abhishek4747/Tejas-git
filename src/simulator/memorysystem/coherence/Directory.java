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
		sendAnEventFromCacheToDirectory(addr, c, RequestType.DirectoryWriteHit);
	}

	public void readMiss(long addr, Cache c) {
//		XXX if(ArchitecturalComponent.getCore(0).getNoOfInstructionsExecuted()>3000000l) {
//			System.out.println("Directory ReadMiss t : " + GlobalClock.getCurrentTime() + " addr : " + addr + " cache : " + c);
//		}
		readMissAccesses++;
		sendAnEventFromCacheToDirectory(addr, c, RequestType.DirectoryReadMiss);
	}

	public void writeMiss(long addr, Cache c) {
//		XXX if(ArchitecturalComponent.getCore(0).getNoOfInstructionsExecuted()>3000000l) {
//			System.out.println("Directory WriteMiss t : " + GlobalClock.getCurrentTime() + " addr : " + addr + " cache : " + c);
//		}
		writeMissAccesses++;
		sendAnEventFromCacheToDirectory(addr, c, RequestType.DirectoryWriteMiss);
	}

	private AddressCarryingEvent sendAnEventFromCacheToDirectory(long addr, Cache c, RequestType request) {
		
		incrementHitMissInformation(addr);
		
		// Create an event
		Directory directory = this;
		AddressCarryingEvent event = new AddressCarryingEvent(
				c.getEventQueue(), 0, c, directory, request, addr);

		// 2. Send event to directory
		c.sendEvent(event);
		
		return event;
	}
	
	private void incrementHitMissInformation(long addr) {
		CacheLine dirEntry = access(addr);
		
		if(dirEntry==null || dirEntry.getState()==MESI.INVALID) {
			misses++;
		} else {
			hits++;
		}		
	}

	public void handleWriteHit(long addr, Cache c, AddressCarryingEvent event) {
		CacheLine dirEntry = access(addr);

		switch (dirEntry.getState()) {
			case MODIFIED:
			case EXCLUSIVE:
			case SHARED: {
				
				if(dirEntry.isSharer(c)==false) {
					System.err.println("WriteHit expects cache to be a sharer. Cache : " + c + ". Addr : " + addr);
				}
				
				for(Cache sharerCache : dirEntry.getSharers()) {
					if(sharerCache!=c) {
						sendAnEventFromMeToCache(addr, sharerCache, RequestType.EvictCacheLine);
					}
				}
				
				dirEntry.clearAllSharers();
				dirEntry.addSharer(c);
				dirEntry.setState(MESI.MODIFIED);
				
				break;
			}
	
			case INVALID: {
				dirEntry.clearAllSharers();
				dirEntry.setState(MESI.MODIFIED);
				dirEntry.addSharer(c);				
				break;
			}
		}
		
		sendAnEventFromMeToCache(addr, c, RequestType.AckDirectoryWriteHit);
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

	public AddressCarryingEvent evictedFromSharedCache(long addr, Cache c) {
//		XXX if(ArchitecturalComponent.getCore(0).getNoOfInstructionsExecuted()>3000000l) {
//			System.out.println("Directory EvictShared t : " + GlobalClock.getCurrentTime() + " addr : " + addr + " cache : " + c);
//		}
		evictedFromSharedCacheAccesses++;
		return sendAnEventFromCacheToDirectory(addr, c, RequestType.DirectoryEvictedFromSharedCache);
	}
	
	public AddressCarryingEvent evictedFromCoherentCache(long addr, Cache c) {
//		XXX if(ArchitecturalComponent.getCore(0).getNoOfInstructionsExecuted()>3000000l) {
//			System.out.println("Directory EvictCoherent t : " + GlobalClock.getCurrentTime() + " addr : " + addr + " cache : " + c);
//		}
		evictedFromCoherentCacheAccesses++;
		return sendAnEventFromCacheToDirectory(addr, c, RequestType.DirectoryEvictedFromCoherentCache);
	}
	
	public void handleEvent(EventQueue eventQ, Event e) {
		AddressCarryingEvent event = (AddressCarryingEvent) e;
		long addr = event.getAddress();
		RequestType reqType = e.getRequestType();

//		if(ArchitecturalComponent.getCores()[0].getNoOfInstructionsExecuted() > 4000000) {
//			System.out.println("\n\nDirectory handleEvent currEvent : " + event);
//			toStringPendingEvents();
//		}
		
		if(access(addr)==null && (reqType==RequestType.DirectoryWriteHit || reqType==RequestType.DirectoryWriteMiss ||
			reqType==RequestType.DirectoryReadMiss || reqType==RequestType.DirectoryEvictedFromCoherentCache)) {
			
			// This events expect a directory entry to be present.
			// Create a directory entry.
			CacheLine evictedEntry = fill(addr, MESI.INVALID);
			
			if(evictedEntry!=null && evictedEntry.isValid()) {
				handleEvictFromSharedCache(evictedEntry.getAddress());
			}
		}
		
		Cache senderCache = (Cache) event.getRequestingElement();

		switch (event.getRequestType()) {
			case DirectoryWriteHit: {
				handleWriteHit(addr, senderCache, event);
				break;
			}
			
			case DirectoryReadMiss: {
				handleReadMiss(addr, senderCache);
				break;
			}
			
			case DirectoryWriteMiss: {
				handleWriteMiss(addr, senderCache);
				break;
			}
			
			case DirectoryEvictedFromSharedCache: {
				handleEvictFromSharedCache(addr);
				break;
			}
			
			case DirectoryEvictedFromCoherentCache: {
				handleEvictedFromCoherentCache(addr, senderCache);
				break;
			}
		}
	}
	
	private void handleEvictedFromCoherentCache(long addr, Cache c) {
		CacheLine dirEntry = access(addr);
		
		if(dirEntry.isSharer(c)) {
			dirEntry.removeSharer(c);
			if(dirEntry.getSharers().size()==0) {
				dirEntry.setState(MESI.INVALID);
			} else if(dirEntry.getSharers().size()==1) {
				dirEntry.setState(MESI.EXCLUSIVE);
				sendAnEventFromMeToCache(addr, dirEntry.getOwner(), RequestType.DirectorySharedToExclusive);
			}
		}
		
		sendAnEventFromMeToCache(addr, c, RequestType.AckEvictCacheLine);
	}

	private void handleWriteMiss(long addr, Cache c) {
		CacheLine dirEntry = access(addr);
		
		handleReadMiss(addr, c);
		for(Cache sharerCache : dirEntry.getSharers()) {
			if(sharerCache!=c) {
				sendAnEventFromMeToCache(addr, sharerCache, RequestType.EvictCacheLine);
			}
		}
		
		dirEntry.clearAllSharers();
		dirEntry.addSharer(c);
		dirEntry.setState(MESI.MODIFIED);
	}

	private void handleEvictFromSharedCache(long addr) {
		CacheLine cl = access(addr);
		
		if(cl==null || cl.isValid()==false) {
			return;
		} else {
			for(Cache c : cl.getSharers()) {
				sendAnEventFromMeToCache(addr, c, RequestType.EvictCacheLine);
			}
			
			cl.clearAllSharers();
			cl.setState(MESI.INVALID);			
		}
	}

	private void handleReadMiss(long addr, Cache c) {
		CacheLine dirEntry = access(addr);
		
		switch(dirEntry.getState()) {
			case MODIFIED: 
			case EXCLUSIVE: 
			case SHARED : {
				
				if(dirEntry.isSharer(c)==true) {
					System.err.println("ReadMiss from a sharer cache. Addr : " + addr + ". Cache : " + c);
					invalidAccesses++;
					sendAnEventFromMeToCache(addr, c, RequestType.Mem_Response);
				} else {
					Cache sharerCache = dirEntry.getFirstSharer();
					sendCachelineForwardRequest(sharerCache, c, addr);
				}
				
				dirEntry.setState(MESI.SHARED);
				dirEntry.addSharer(c);
				
				break;
			}
			
			
			case INVALID: {
				dirEntry.setState(MESI.EXCLUSIVE);
				dirEntry.clearAllSharers();
				dirEntry.addSharer(c);
				// If the line is supposed to be fetched from the next level cache, 
				// we will just send a cacheRead request to this cache
				// Note that the directory is not coming into the picture. This is just a minor hack to maintain readability of the code
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
