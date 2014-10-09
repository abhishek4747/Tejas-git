/*****************************************************************************
				Tejas Simulator
------------------------------------------------------------------------------------------------------------

   Copyright 2010 Indian Institute of Technology, Delhi
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

	Contributors:  Moksh Upadhyay
*****************************************************************************/
package memorysystem;

import emulatorinterface.translator.visaHandler.Invalid;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.RequestType;
import generic.SimulationElement;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;

import main.ArchitecturalComponent;
import memorysystem.coherence.Coherence;
import memorysystem.coherence.Directory;
import memorysystem.nuca.NucaCache.NucaType;
import misc.Util;
import config.CacheConfig;
import config.CacheConfig.WritePolicy;
import config.CacheDataType;
import config.CacheEnergyConfig;
import config.EnergyConfig;

public class Cache extends SimulationElement
{
	/* cache parameters */
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
	
	public Coherence mycoherence;
	
	//public CacheType levelFromTop; 
	public boolean isLastLevel; //Tells whether there are any more levels of cache
	public CacheConfig.WritePolicy writePolicy; //WRITE_BACK or WRITE_THROUGH
	public String nextLevelName; //Name of the next level cache according to the configuration file
	public ArrayList<Cache> prevLevel = new ArrayList<Cache>(); //Points towards the previous level in the cache hierarchy
	public Cache nextLevel; //Points towards the next level in the cache hierarchy
	protected CacheLine lines[];
	
	public long noOfRequests;
	public long noOfAccesses;
	public long noOfResponsesReceived;
	public long noOfResponsesSent;
	public long noOfWritesForwarded;
	public long noOfWritesReceived;
	public long hits;
	public long misses;
	public long evictions;
	public boolean debug =false;
	public NucaType nucaType;
	
	CacheEnergyConfig energy;
	
	public String cacheName;
		
	public void createLinkToNextLevelCache(Cache nextLevelCache) {
		this.nextLevel = nextLevelCache;			
		this.nextLevel.prevLevel.add(this);
	}
	
	public CacheConfig cacheConfig;
	public int id;
	
	public long mshrMaxSize;
	public LinkedList<AddressCarryingEvent> mshr;
	
	ArrayList<AddressCarryingEvent> eventsWaitingOnLowerMSHR;
	
	public Cache(String cacheName, int id, 
		CacheConfig cacheParameters, CoreMemorySystem containingMemSys)
	{
		super(cacheParameters.portType,
				cacheParameters.getAccessPorts(), 
				cacheParameters.getPortOccupancy(),
				cacheParameters.getLatency(),
				cacheParameters.operatingFreq);
		
		// add myself to the global cache list
		MemorySystem.addToCacheList(cacheName, this);
				
		if(cacheParameters.collectWorkingSetData==true) {
			workingSet = new TreeSet<Long>();
			workingSetChunkSize = cacheParameters.workingSetChunkSize;
		}
		
		this.containingMemSys = containingMemSys;
		
		// set the parameters
		this.blockSize = cacheParameters.getBlockSize();
		this.assoc = cacheParameters.getAssoc();
		this.size = cacheParameters.getSize();
		this.blockSizeBits = Util.logbase2(blockSize);
		this.assocBits = Util.logbase2(assoc);
		this.numLines = getNumLines();
		this.numLinesBits = Util.logbase2(numLines);
		this.numSetsBits = numLinesBits - assocBits;

		this.writePolicy = cacheParameters.getWritePolicy();
		
		this.cacheConfig = cacheParameters;
		if(this.containingMemSys==null) {
			// Use the core memory system of core 0 for all the shared caches.
			this.isSharedCache = true;
			//this.containingMemSys = ArchitecturalComponent.getCore(0).getExecEngine().getCoreMemorySystem(); 
		}
		
		if(cacheParameters.nextLevel=="") {
			this.isLastLevel = true;
		} else {
			this.isLastLevel = false;
		}
		
		this.cacheName = cacheName;
		this.id = id;
		
		this.nextLevelName = cacheParameters.getNextLevel();
//			this.enforcesCoherence = cacheParameters.isEnforcesCoherence();
		
		this.timestamp = 0;
		this.numLinesMask = numLines - 1;
		this.noOfRequests = 0;
		noOfAccesses = 0;noOfResponsesReceived = 0;noOfResponsesSent = 0;noOfWritesForwarded = 0;
		noOfWritesReceived = 0;
		this.hits = 0;
		this.misses = 0;
		this.evictions = 0;
		// make the cache
		makeCache(cacheParameters.isDirectory);
		
		this.mshrMaxSize = cacheParameters.mshrSize;
		this.mshr = new LinkedList<AddressCarryingEvent>();
		
		this.nucaType = NucaType.NONE;
		
		energy = cacheParameters.power;
		
		eventsWaitingOnLowerMSHR = new ArrayList<AddressCarryingEvent>();
		
        // Create a pending events field of each set in the directory cache
        int numSets = lines.length/assoc;
        for(int i=0; i<numSets; i++) {
        	pendingEvents.add(new LinkedList<AddressCarryingEvent>());
        }
	}
	
	public void unlock(long addr, Cache c) {
		CacheLine cl = access(addr);
		//case : read miss in both cache and directory. when cache gets the line
		//from the lower level, it calls directory's unlock -- at this point
		//directory's line is invalid, which is actually legal
		if(cl==null) {
			misc.Error.showErrorAndExit("Invalid state");
		}

		if (c != null) {
			if (cl.getListOfAwaitedCacheResponses().contains(c)) {
				cl.removeCacheFromAwaitedCacheList(c);
			} else {
				misc.Error.showErrorAndExit("No response expected from this cache : "	+ c);
			}
		}

		if (cl.getListOfAwaitedCacheResponses().isEmpty() == true) {
			AddressCarryingEvent currentEvent = cl.getCurrentEvent();
			cl.setCurrentEvent(null);
			callStateChangeFunction(currentEvent);

			AddressCarryingEvent nextEvent = getPendingEventToProcess(addr);
			if (nextEvent != null) {
				handleEvent(nextEvent.getEventQ(), nextEvent);
			}
		}
	}
	
	protected void callStateChangeFunction(AddressCarryingEvent event) {
		switch(event.getRequestType()) {
			case Cache_Read: {
				sendResponseToWaitingEvent(event);
				break;
			}
			
			case Cache_Write: {
				sendResponseToWaitingEvent(event);
				updateStateOfCacheLine(event.getAddress(), MESI.MODIFIED);
				break;
			}
			
			case EvictCacheLine: {
				updateStateOfCacheLine(event.getAddress(), MESI.INVALID);
				break;
			}
		}	
	}

	public void setCoherence(Coherence c) {
		this.mycoherence = c;
	}
	
	private boolean printCacheDebugMessages = false;
	public void handleEvent(EventQueue eventQ, Event e)
	{
//		if(ArchitecturalComponent.getCores()[1].getNoOfInstructionsExecuted() > 6000000l) {
//			System.out.println("\n\nCache[ " + this + "] handleEvent currEvent : " + e);
//			toStringPendingEvents();
//		}
//		
//		if(e.serializationID==30208059) {
//			System.out.println("culprint");
//		}
		
		AddressCarryingEvent event = (AddressCarryingEvent)e;
		printCacheDebugMessage(event);
		
		long addr = ((AddressCarryingEvent)event).getAddress();
		RequestType requestType = event.getRequestType();
				
		switch(event.getRequestType()) {
			case Cache_Read:
			case Cache_Write: {
				if(lockCacheLineAndRemovePendingEvent(event)==false) {
					return;
				}
				handleAccess(addr, requestType, event);
				break;
			}
			
			case Mem_Response: {
				handleMemResponse(addr);
				break;
			}
			
			case DirectoryInvalidate: {
				handleDirectoryInvalidate(addr);
				break;
			}
			
			case DirectoryCachelineForwardRequest: {
				handleDirectoryCachelineForwardRequest(addr, (Cache)(((AddressCarryingEvent)event).getPayloadElement()));
				break;
			}
			
			case DirectorySharedToExclusive: {
				handleDirectorySharedToExclusive(addr);
				break;
			}
		}
	}
	
	protected boolean lockCacheLineAndRemovePendingEvent(
			AddressCarryingEvent event) {
		event.setHasArrivedAtDestination(true);
		long addr = event.getAddress();

		if (canProcess(addr, event) == false) {
			return false;
		}
		
		// Lock the directory entry
		CacheLine dirEntry = access(addr);
		dirEntry.setCurrentEvent(event);
		
		// Remove this entry from the pending events queue
		removePendingEvent(event);
		return true;
	}
	
	private void handleDirectorySharedToExclusive(long addr) {
		mycoherence.unlock(addr, this);			
	}

	private void handleDirectoryCachelineForwardRequest(long addr, Cache cache) {
		AddressCarryingEvent event = new AddressCarryingEvent(cache.getEventQueue(), 0, 
			this, cache, RequestType.Mem_Response, addr);
		
		this.sendEvent(event);
	}

	private void printCacheDebugMessage(Event event) {
		if(printCacheDebugMessages==true) {
			if(event.getClass()==AddressCarryingEvent.class) {
				System.out.println("CACHE : globalTime = " + GlobalClock.getCurrentTime() + 
					"\teventTime = " + event.getEventTime() + "\t" + event.getRequestType() +
					"\trequestingElelement = " + event.getRequestingElement() +
					"\taddress = " + ((AddressCarryingEvent)event).getAddress() +
					"\t" + this);
			}
		}
	}
	
	public void addToMSHR(AddressCarryingEvent event) {
		if(mshr.contains(event)==true) {
			misc.Error.showErrorAndExit("Event already present in the MSHR : " + event);
		}
		
		mshr.add(event);
	}
	
	public void removeFromMSHR(long addr) {
		CacheLine cl = access(addr);
		if(cl!=null && cl.isLocked()) {
			mshr.remove(cl.getCurrentEvent());
		} else {
			// There is no (locked) cacheline for this addr. 
			// In its absence, we should search for an event in the mshr with address==addr, and remove it 
			for(AddressCarryingEvent event : mshr) {
				if(event.getAddress()==addr) {
					misc.Error.showErrorAndExit("cacheline for addr : " + addr + " not locked. Suspect event : " + event);
				}
			}
			
			misc.Error.showErrorAndExit("cacheline for addr : " + addr + " not locked. Suspect event : NONE !!");
		}
	}
	
	public void printMSHR() {
		int i=0;
		System.out.println("\nMSHR of " + this + "\n");
		for(AddressCarryingEvent event : mshr) {
			System.out.println(i + " : " + event);
			i++;
		}
	}

	public void handleAccess(long addr, RequestType requestType, AddressCarryingEvent event) {
		
		if(requestType == RequestType.Cache_Write) {
			noOfWritesReceived++;
		}
		
		CacheLine cl = this.accessAndMark(addr);
					
		//IF HIT
		if (cl != null) {
			cacheHit(addr, requestType, cl, event);
		} else {
			addToMSHR(event);
			
			if(this.mycoherence != null) {
				if(requestType == RequestType.Cache_Write) {
					mycoherence.writeMiss(addr, this);
				} else if(requestType == RequestType.Cache_Read) {
					mycoherence.readMiss(addr, this);
				}
			} else {
				sendRequestToNextLevel(addr, RequestType.Cache_Read);
			}
		}
	}

	private void cacheHit(long addr, RequestType requestType, CacheLine cl, AddressCarryingEvent event) {
		hits++;
		noOfRequests++;
		noOfAccesses++;
		
		if(requestType==RequestType.Cache_Write && 
			(cl.getState()==MESI.SHARED || cl.getState()==MESI.EXCLUSIVE))
		{
			if(mycoherence!=null) {
				mycoherence.writeHit(addr, this);				
			} else {
				// We are already writing into the next level cache in sendResponseToWaitingEvent function
				if (this.writePolicy == CacheConfig.WritePolicy.WRITE_BACK) 	{
					sendRequestToNextLevel(addr, RequestType.Cache_Write);
				}			
			}
		}
		
		// If I am not updating the state of any other cache as a response to this event, unlock the cacheline
			// ReadHit does not change any state
		if(requestType==RequestType.Cache_Read ||
			// WriteHit without coherence does not change any state
			(requestType==RequestType.Cache_Write && mycoherence==null) ||
			// WriteHit for an already modified line does not change any state
			(mycoherence!=null && cl.isModified())) {
			unlock(addr, null);
		}		
	}
	
	protected void handleMemResponse(long addr) {
		noOfResponsesReceived++;
		this.fillAndSatisfyRequests(addr);
		if(mycoherence!=null) {
			mycoherence.unlock(addr, this);
		}
	}
	
	public void handleDirectoryInvalidate(long addr) {
		mycoherence.unlock(addr, this);
	}
	
	public void sendRequestToNextLevel(long addr, RequestType requestType) {
		Cache c = this.nextLevel;
		AddressCarryingEvent event = null;
		if(c!=null) {
			event = new AddressCarryingEvent(c.getEventQueue(), 0, 
				this, c, requestType, addr);
			addEventAtLowerCache(event);
		} else {
			Core core0 = ArchitecturalComponent.getCores()[0];
			MainMemoryController memController = getComInterface().getNearestMemoryController();
			event = new AddressCarryingEvent(core0.getEventQueue(), 0, 
				this, memController, requestType, addr);
			sendEvent(event);
		}
	}
	
	protected void sendResponseToWaitingEvent(AddressCarryingEvent event) {
		if (event.getRequestType() == RequestType.Cache_Read) {
			sendMemResponse(event);
		} else if (event.getRequestType() == RequestType.Cache_Write) {
			if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH) 	{
				AddressCarryingEvent lastWriteEvent = (AddressCarryingEvent) event.clone();
				sendRequestToNextLevel(lastWriteEvent.getAddress(), RequestType.Cache_Write);
			}
		}
	}
	
	public boolean isL2cache() {
		// I am not a first level cache.
		// But a cache connected on top of me is a first level cache
		return (this.cacheConfig.firstLevel==false && 
			this.prevLevel.get(0).cacheConfig.firstLevel==true);
	}
	
	public boolean isIcache() {
		return (this.cacheConfig.firstLevel==true && 
			(this.cacheConfig.cacheDataType==CacheDataType.Instruction || 
			this.cacheConfig.cacheDataType==CacheDataType.Unified) );
	}
	
	public boolean isL1cache() {
		return (this.cacheConfig.firstLevel==true && 
			(this.cacheConfig.cacheDataType==CacheDataType.Data || 
			this.cacheConfig.cacheDataType==CacheDataType.Unified) );
	}
	
	private boolean isSharedCache = false;
	public boolean isSharedCache() {
		return isSharedCache; 
	}
	
	public boolean isPrivateCache() {
		return !isSharedCache();
	}
	
	public boolean addEventAtLowerCache(AddressCarryingEvent event)
	{
		if(this.nextLevel.isMSHRFull() == false)
		{
			sendEvent(event);
			this.nextLevel.addPendingEvent(event);
			this.nextLevel.workingSetUpdate();
			return true;
		}
		else
		{
			handleLowerMshrFull((AddressCarryingEvent)event);
			return false;
		}
	}
	
	public void fillAndSatisfyRequests(long addr)
	{
		removeFromMSHR(addr);
		
		misses += 1;
		noOfRequests += 1;
		noOfAccesses+= 1 + 1;
		
		CacheLine evictedLine = this.fill(addr, MESI.EXCLUSIVE);
		
		if (evictedLine != null && 	evictedLine.getState() != MESI.INVALID) {
			misc.Error.showErrorAndExit("fill should not have evicted a valid cacheline !!");
			if(mycoherence!=null) {
				misc.Error.showErrorAndExit("fill should not have evicted a valid cacheline !!");
//				mycoherence.forceInvalidate(addr);
//				mycoherence.evictedFromCoherentCache(evictedLine.getAddress(), this);
//				if(evictedLine.isModified() && writePolicy==WritePolicy.WRITE_BACK) {
//					sendRequestToNextLevel(evictedLine.getAddress(), RequestType.Cache_Write);
//				}
			} else {
				if(evictedLine.isModified() && writePolicy==WritePolicy.WRITE_BACK) {
					sendRequestToNextLevel(evictedLine.getAddress(), RequestType.Cache_Write);
				}
				updateStateOfCacheLine(evictedLine.getAddress(), MESI.INVALID);
			}
		}
		
		if(mycoherence==null) {
			// If I am a coherent cache, the directory would unlock me.
			unlock(addr, null);
		}
	}
	
	public void sendMemResponse(AddressCarryingEvent event)
	{
		AddressCarryingEvent memResponseEvent = new AddressCarryingEvent(
				event.getEventQ(), 0, event.getProcessingElement(),
				event.getRequestingElement(), RequestType.Mem_Response, event.getAddress());
		
//		if(ArchitecturalComponent.getCores()[1].getNoOfInstructionsExecuted() > 6000000l)
//			System.out.println("sending mem response from " + event.getProcessingElement() + " to " + event.getRequestingElement() + " for addr : " + event.getAddress());
		
		if(getComInterface()!=memResponseEvent.getProcessingElement().getComInterface()) {
			sendEvent(memResponseEvent);
		} else {
			memResponseEvent.getProcessingElement().handleEvent(memResponseEvent.getEventQ(), memResponseEvent);
		}
		
		noOfResponsesSent++;
	}
	
	public long computeTag(long addr) {
		long tag = addr >>> (numSetsBits + blockSizeBits);
		return tag;
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
	
	public CacheLine accessValid(long addr)
	{
		CacheLine cl = access(addr);
		if(cl!=null && cl.getState()!=MESI.INVALID) {
			return cl;
		} else {
			return null;
		}
	}
	
	public CacheLine access(long addr)
	{
		/* compute startIdx and the tag */
		int startIdx = getStartIdx(addr);
		long tag = computeTag(addr);
		
		/* search in a set */
		for(int idx = 0; idx < assoc; idx++) 
		{
			// calculate the index
			int index = getNextIdx(startIdx,idx);
			// fetch the cache line
			CacheLine ll = this.lines[index];
			// If the tag is matching, we have a hit
			if(ll.hasTagMatch(tag)) {
				return  ll;
			}
		}
		return null;
	}
	
	protected void mark(CacheLine ll, long tag)
	{
		ll.setTag(tag);
		mark(ll);
	}
	
	private void mark(CacheLine ll)
	{
		ll.setTimestamp(timestamp);
		timestamp += 1.0;
	}
	
	private void makeCache(boolean isDirectory)
	{
		lines = new CacheLine[numLines];
		for(int i = 0; i < numLines; i++) {
			lines[i] = new CacheLine(isDirectory);
		}
	}
	
	private int getNumLines()
	{
		long totSize = size;
		return (int)(totSize / (long)(blockSize));
	}
	
	protected CacheLine accessAndMark(long addr) {
		CacheLine cl = accessValid(addr);
		if(cl != null) {
			mark(cl);
		}
		return cl;
	}

	public CacheLine fill(long addr, MESI stateToSet) //Returns a copy of the evicted line
	{
		CacheLine evictedLine = null;
		/* compute startIdx and the tag */
		int startIdx = getStartIdx(addr);
		long tag = computeTag(addr); 
		boolean addressAlreadyPresent = false;
		/* find any invalid lines -- no eviction */
		CacheLine fillLine = null;
		boolean evicted = false;

		for (int idx = 0; idx < assoc; idx++) 
		{
			int nextIdx = getNextIdx(startIdx, idx);
			CacheLine ll = this.lines[nextIdx];
			if (ll.getTag() == tag) 
			{	
				addressAlreadyPresent = true;
				fillLine = ll;
				break;
			}
		}
		
		for (int idx = 0;!addressAlreadyPresent && idx < assoc; idx++) 
		{
			int nextIdx = getNextIdx(startIdx, idx);
			CacheLine ll = this.lines[nextIdx];
			if (ll.isValid()==false && ll.isLocked()==false) 
			{
				fillLine = ll;
				break;
			}
		}
		
		/* LRU replacement policy -- has eviction*/
		if (fillLine == null) 
		{
			evicted = true; // We need eviction in this case
			double minTimeStamp = Double.MAX_VALUE;
			for(int idx=0; idx<assoc; idx++) 
			{
				int index = getNextIdx(startIdx, idx);
				CacheLine ll = this.lines[index];
				
				if(ll.isLocked()==true) {
					continue;
				}
				
				if(minTimeStamp > ll.getTimestamp()) {
					minTimeStamp = ll.getTimestamp();
					fillLine = ll;
				}
			}
		}
		
		if(fillLine==null) {
			misc.Error.showErrorAndExit("Unholy mess !!");
		}

		/* if there has been an eviction */
		if (evicted) 
		{
			evictedLine = (CacheLine) fillLine.clone();
			long evictedLinetag = evictedLine.getTag();
			evictedLinetag = (evictedLinetag << numSetsBits ) + (startIdx/assoc) ;
			evictedLine.setTag(evictedLinetag);
			this.evictions++;
		}

		/* This is the new fill line */
		fillLine.setState(stateToSet);
		fillLine.setAddress(addr);
		mark(fillLine, tag);
		return evictedLine;
	}

	public void handleLowerMshrFull( AddressCarryingEvent event)
	{
		AddressCarryingEvent eventToBeSent = (AddressCarryingEvent)event.clone();
		eventToBeSent.payloadElement = eventToBeSent
				.getProcessingElement();
		eventToBeSent
				.setProcessingElement(eventToBeSent.getRequestingElement());

		eventToBeSent.setEventTime(GlobalClock.getCurrentTime() + 1);

		eventsWaitingOnLowerMSHR.add(eventToBeSent);
		//eventToBeSent.getEventQ().addEvent(eventToBeSent);
	}
	
	public void oneCycleOperation()
	{
		while(!eventsWaitingOnLowerMSHR.isEmpty())
		{
			AddressCarryingEvent event = eventsWaitingOnLowerMSHR.remove(0);
			
			((AddressCarryingEvent)event).setProcessingElement(((AddressCarryingEvent)event).payloadElement);
			((AddressCarryingEvent)event).payloadElement = null;
			Cache processingCache = (Cache)event.getProcessingElement();
			//event.setEventTime(event.getEventTime()-GlobalClock.getCurrentTime());
			event.setEventTime(0);
			
			if(addEventAtLowerCache((AddressCarryingEvent)event) == false)
				break;
		}
	}
	
	public boolean isMSHRFull() {
		return (mshr.size()>=mshrMaxSize);
	}

	public String toString()
	{
		return cacheName;
	}
	
	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException
	{
		EnergyConfig newPower = new EnergyConfig(energy.leakageEnergy, energy.readDynamicEnergy);
		EnergyConfig cachePower = new EnergyConfig(newPower, noOfAccesses);
		cachePower.printEnergyStats(outputFileWriter, componentName);
		return cachePower;
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

	public void updateStateOfCacheLine(long addr, MESI newState) {
		CacheLine cl = this.access(addr);
		
		if(cl!=null) {
			cl.setState(newState);
			
			if(prevLevel!=null && prevLevel.size()!=0) {
				if(newState==MESI.INVALID && prevLevel.get(0).mycoherence!=null) {
					// If the previous level caches have coherence, then the coherence
					// system should be asked to invalidate the address from the sharer caches
					prevLevel.get(0).mycoherence.evictedFromSharedCache(addr, this);
				} else {
					for(Cache c : prevLevel) {
						c.updateStateOfCacheLine(addr, newState);
					}
				}
			}
			
			if(newState==MESI.INVALID && cl.isLocked()) {
				unlock(addr, null);
			}
		}	
	}

	public EventQueue getEventQueue() {
		if(containingMemSys!=null) {
			return containingMemSys.getCore().eventQueue;
		} else {
			return (ArchitecturalComponent.getCores()[0]).eventQueue;
		}
	}

	public Cache getNextLevelCache(long addr) {
		return nextLevel;
	}
	
	public void workingSetUpdate()
	{
		// Clear the working set data after every x instructions
		if(this.containingMemSys!=null && this.workingSet!=null) {
			
			if(isIcache()) {
				long numInsn = containingMemSys.getiCache().hits + containingMemSys.getiCache().misses; 
				long numWorkingSets = numInsn/workingSetChunkSize; 
				if(numWorkingSets>containingMemSys.numInstructionSetChunksNoted) {
					this.clearWorkingSet();
					containingMemSys.numInstructionSetChunksNoted++;
				}
			} else if(isL1cache()) {
				long numInsn = containingMemSys.getiCache().hits + containingMemSys.getiCache().misses;
				long numWorkingSets = numInsn/workingSetChunkSize; 
				if(numWorkingSets>containingMemSys.numDataSetChunksNoted) {
					this.clearWorkingSet();
					containingMemSys.numDataSetChunksNoted++;
				}
			}
		}
	}
	
	TreeSet<Long> workingSet = null;
	long workingSetChunkSize = 0;
	public long numWorkingSetHits = 0;
	public long numWorkingSetMisses = 0;
	public long numFlushesInWorkingSet = 0;
	public long totalWorkingSetSize = 0;
	public long maxWorkingSetSize = Long.MIN_VALUE;
	public long minWorkingSetSize = Long.MAX_VALUE;
	
	void addToWorkingSet(long addr) {
		long lineAddr = addr >>> blockSizeBits;
		if(workingSet!=null) {
			if(workingSet.contains(lineAddr)==true) {
				numWorkingSetHits++;
				return;
			} else {
				numWorkingSetMisses++;
				workingSet.add(lineAddr);
			}
		}
	}
	
	float getWorkingSetHitrate() {
		if(numWorkingSetHits==0 && numWorkingSetMisses==0) {
			return 0.0f;
		} else {
			return (float)numWorkingSetHits/(float)(numWorkingSetHits + numWorkingSetMisses);
		}
	}
	
	void clearWorkingSet() {
		numFlushesInWorkingSet++;
		totalWorkingSetSize += workingSet.size();
		if(workingSet.size()>maxWorkingSetSize) {
			maxWorkingSetSize = workingSet.size();
		}
		
		if(workingSet.size()<minWorkingSetSize) {
			minWorkingSetSize = workingSet.size();
		}
		
		//System.out.println(this + " : For chunk " + (numFlushesInWorkingSet-1) + 
		//	"\tworkSet = " + workingSet.size() +
		//	"\tminSet = " + minWorkingSetSize + 
		//	"\tavgSet = " + (float)totalWorkingSetSize/(float)numFlushesInWorkingSet + 
		//	"\tmaxSet = " + maxWorkingSetSize + 
		//	"\tworkSetHitrate = " + getWorkingSetHitrate());
		
		if(workingSet!=null) {
			workingSet.clear();
		}
	}
	
	// List of pending event list for each set in the cache
	private ArrayList<LinkedList<AddressCarryingEvent>> pendingEvents = new ArrayList<LinkedList<AddressCarryingEvent>>();
	
	public void addPendingEvent(AddressCarryingEvent event) {
		long addr = event.getAddress();
		long setAddress = getSetIdx(addr);
		pendingEvents.get((int) setAddress).add(event);
		event.setHasArrivedAtDestination(false);
	}
	
	private long getLineAddress(long addr) {
		return addr >>> blockSizeBits;
	}
	
	/*
	 * Returns the first event which satisfies these properties : 
	 * (a) has reached the directory
	 * (b) belongs to the same lineAddr as that of addr or belongs to a line which is not present in the set or belongs to a line which is present in the set and is unlocked
	 */
	protected AddressCarryingEvent getPendingEventToProcess(long addr) {
		long myLineAddr = getLineAddress(addr);
		int setAddr = (int) getSetIdx(addr);

		for (AddressCarryingEvent event : pendingEvents.get(setAddr)) {
			long nextAddr = event.getAddress();
			long nextLineAddr = getLineAddress(nextAddr);
			CacheLine dirEntry = access(nextAddr);
			boolean validDirEntry = dirEntry!=null && dirEntry.isValid()==true;
			
			if (myLineAddr!=nextLineAddr && validDirEntry) {
				if(dirEntry.isLocked()==false && event.hasArrivedAtDestination()==true) {
					// this event belongs to a different (valid) line in the same set. If its unlocked, and it has reached destination, return it
					return event;
				}
			} else if (event.hasArrivedAtDestination()) {
				// If you reach here, it means -
				// (a) this event belongs to the same line (As this line is unlocked, there's no issue in returning this event)
				// (b) this event belongs to a line which is not present in the set
				return event;
			}
		}
		
		return null;
	}
	
	protected void removePendingEvent(AddressCarryingEvent event) {
		long addr = event.getAddress();
		if(pendingEvents.get((int)getSetIdx(addr)).remove(event)==false) {
			misc.Error.showErrorAndExit("Unable to remove pending event : " + event);
		}
	}
	
	protected boolean isThereAPendingEventForTheSameLineBeforeMe(
			AddressCarryingEvent event) {
		long addr = event.getAddress();
		int setAddr = (int) getSetIdx(addr);
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
	
	private CacheLine getLRUUnlockedEntry(long addr) {
		/* compute startIdx and the tag */
		int startIdx = getStartIdx(addr);
		long tag = computeTag(addr); 
		/* find any invalid lines -- no eviction */
		CacheLine fillLine = null;
				
		for (int idx = 0; idx < assoc; idx++) 
		{
			int nextIdx = getNextIdx(startIdx, idx);
			CacheLine ll = this.lines[nextIdx];
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
	
	private AddressCarryingEvent createEvictionEventForAddr(long addr) {
		AddressCarryingEvent event =  new AddressCarryingEvent();
		event.setAddress(addr);
		event.setRequestType(RequestType.EvictCacheLine);
		return event;
	}
	
	protected void sendAnEventFromMeToCache(long addr, Cache c, RequestType request) {
		// Create an event
		
		AddressCarryingEvent event = new AddressCarryingEvent(
			c.getEventQueue(), 0, this, c, request, addr);
	
		// 2. Send event to cache
		this.sendEvent(event);
	}
	
	protected void evictCacheLine(CacheLine evictedEntry) {
		long addr = evictedEntry.getAddress();
		
		if(mycoherence!=null) {
			mycoherence.evictedFromCoherentCache(addr, this);
		} else {
			//This line has been locked by canProcess method.
			//Unlocking the cacheline would unlock it, and update the cacheline state to Invalid
			unlock(addr, null);
		}
	}
	
	protected boolean canProcess(long addr, AddressCarryingEvent event) {
		
		if(isThereAPendingEventForTheSameLineBeforeMe(event)) {
			return false;
		}

		CacheLine dirEntry = access(addr);
		if (dirEntry != null) {
			if (dirEntry.isLocked() == false) {
				// The directory entry is present and its unlocked right now
				return true;
			} else {
				return false;
			}
		} else {
			// If there is an invalid entry in the cache, then use this directory entry
			if (isThereAnInvalidUnlockedEntryInCacheSet(addr)) {
				CacheLine evictedEntry = (CacheLine) this.fill(addr, MESI.INVALID);
				return true;
			} else if (isThereAnUnlockedEntryInCacheSet(addr)) {
				CacheLine evictedEntry = getLRUUnlockedEntry(addr);
				evictedEntry.setCurrentEvent(createEvictionEventForAddr(evictedEntry.getAddress()));
				evictCacheLine(evictedEntry);
				return false;
			} else {
				return false;
			}
		}
	}
	
	protected boolean isThereAnInvalidUnlockedEntryInCacheSet(long addr) {
		/* compute startIdx and the tag */
		int startIdx = getStartIdx(addr);
		
		/* search in a set */
		for(int idx = 0; idx < assoc; idx++) 
		{
			// calculate the index
			int index = getNextIdx(startIdx,idx);
			// fetch the cache line
			CacheLine ll = lines[index];
			// If the tag is matching, we have a hit
			if(ll.getState() == MESI.INVALID && ll.isLocked()==false) {
				return  true;
			}
		}
		return false;
	}
	
	protected boolean isThereAnUnlockedEntryInCacheSet(long addr) {
		/* compute startIdx and the tag */
		int startIdx = getStartIdx(addr);
		
		/* search in a set */
		for(int idx = 0; idx < assoc; idx++) 
		{
			// calculate the index
			int index = getNextIdx(startIdx,idx);
			// fetch the cache line
			CacheLine ll = lines[index];
			// If the tag is matching, we have a hit
			if(ll.isLocked() == false) {
				return  true;
			}
		}
		return false;
	}
	
	public void toStringPendingEvents() {
		int set = 0;
		for(LinkedList<AddressCarryingEvent> pendingEventList : pendingEvents) {
			if(pendingEventList.size()>0) {
				System.out.println("Pending events for set " + set + " . size :  " + pendingEventList.size() + " : ");
				for(AddressCarryingEvent event : pendingEventList) {
					System.out.println(event);
				}
			}
			
			set++;
		}
	}
}