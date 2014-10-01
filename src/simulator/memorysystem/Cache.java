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

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.RequestType;
import generic.SimulationElement;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeSet;

import main.ArchitecturalComponent;
import memorysystem.coherence.Coherence;
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
	
	public MissStatusHoldingRegister missStatusHoldingRegister;
	
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
	
	//does NOT refer to a real hardware structure; used to avoid repeated creation of
	//ArrayList<AddressCarryingEvent> objects when processing a cache hit
	ArrayList<AddressCarryingEvent> tmpHitEventList;
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
		makeCache();
		
		if(this.containingMemSys!=null) {
			missStatusHoldingRegister = new Mode3MSHR(blockSizeBits, cacheParameters.mshrSize, 
				this.containingMemSys.core.eventQueue);
		} else {
			missStatusHoldingRegister = new Mode3MSHR(blockSizeBits, cacheParameters.mshrSize, 
				null);
		}
		
		this.nucaType = NucaType.NONE;
		
		energy = cacheParameters.power;
		
		tmpHitEventList = new ArrayList<AddressCarryingEvent>();
		eventsWaitingOnLowerMSHR = new ArrayList<AddressCarryingEvent>();
	}
	
	public void setCoherence(Coherence c) {
		this.mycoherence = c;
	}
	
	private AddressCarryingEvent eventBeingServiced = null;
	
	private boolean printCacheDebugMessages = false;
	public void handleEvent(EventQueue eventQ, Event event)
	{
		printCacheDebugMessage(event);
		
		long addr = ((AddressCarryingEvent)event).getAddress();
		RequestType requestType = event.getRequestType();
		eventBeingServiced = (AddressCarryingEvent)event;
		
		switch(event.getRequestType()) {
			case Cache_Read:
			case Cache_Write: {
				handleAccess(addr, requestType);
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
		
		eventBeingServiced = null;
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

	public void handleAccess(long addr, RequestType requestType) {
		
		if(requestType == RequestType.Cache_Write) {
			noOfWritesReceived++;
		}
		
		CacheLine cl = this.accessAndMark(addr);
					
		//IF HIT
		if (cl != null) {
			cacheHit(addr, requestType, cl);
		} else {
			//add to MSHR
			boolean newOMREntryCreated = missStatusHoldingRegister.addOutstandingRequest(eventBeingServiced);
			
			if(this.mycoherence != null) {
				if(requestType == RequestType.Cache_Write) {
					mycoherence.writeMiss(addr, this);
				} else if(requestType == RequestType.Cache_Read) {
					mycoherence.readMiss(addr, this);
				}
			} else {
				if(newOMREntryCreated) {
					sendRequestToNextLevel(addr, RequestType.Cache_Read);
				}
			}
		}
	}

	private void cacheHit(long addr, RequestType requestType, CacheLine cl) {
		hits++;
		noOfRequests++;
		noOfAccesses++;
		
		if(requestType==RequestType.Cache_Write && 
			(cl.getState()==MESI.SHARED || cl.getState()==MESI.EXCLUSIVE))
		{
			if(mycoherence!=null) {
				mycoherence.writeHit(addr, this);
			} else {
				sendRequestToNextLevel(addr, RequestType.Cache_Write);
			}
		}			
		
		tmpHitEventList.clear();
		tmpHitEventList.add(eventBeingServiced);
		sendResponseToWaitingEvent(tmpHitEventList);			
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
	
	protected void sendResponseToWaitingEvent(ArrayList<AddressCarryingEvent> outstandingRequestList) {
		AddressCarryingEvent lastWriteEvent = null;
		while (!outstandingRequestList.isEmpty())
		{
			AddressCarryingEvent eventPoppedOut = (AddressCarryingEvent) outstandingRequestList.remove(0); 
			if (eventPoppedOut.getRequestType() == RequestType.Cache_Read) {
				sendMemResponse(eventPoppedOut);
			}
			
			else if (eventPoppedOut.getRequestType() == RequestType.Cache_Write) {
				if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH) 	{
					lastWriteEvent = (AddressCarryingEvent) eventPoppedOut.clone();
				}
			}
		}
		
		if(lastWriteEvent!=null) {
			sendRequestToNextLevel(lastWriteEvent.getAddress(), RequestType.Cache_Write);
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
		if(this.nextLevel.getMissStatusHoldingRegister().isFull() == false)
		{
			sendEvent(event);
			this.nextLevel.workingSetUpdate();
			return true;
		}
		else
		{
			handleLowerMshrFull((AddressCarryingEvent)event);
			return false;
		}
	}
	
	protected void fillAndSatisfyRequests(long addr)
	{
		ArrayList<AddressCarryingEvent> eventsToBeServed = missStatusHoldingRegister.removeRequestsByAddress(addr);
		
		misses += eventsToBeServed.size();
		noOfRequests += eventsToBeServed.size();
		noOfAccesses+=eventsToBeServed.size() + 1;
		
		//System.out.println(this.levelFromTop + "    hits : " + hits + "\tmisses : " + misses + "\trequests : " + noOfRequests);
		CacheLine evictedLine = this.fill(addr, MESI.EXCLUSIVE);
		
		//This does not ensure inclusiveness
		if (evictedLine != null && 	evictedLine.getState() != MESI.INVALID) {
			if(mycoherence!=null) {
				mycoherence.evictedFromCoherentCache(evictedLine.getAddress(), this);
				if(evictedLine.isModified() && writePolicy==WritePolicy.WRITE_BACK) {
					sendRequestToNextLevel(evictedLine.getAddress(), RequestType.Cache_Write);
				}
				// coherence would do the invalidation for this cache 
			} else {
				if(evictedLine.isModified()) {
					sendRequestToNextLevel(evictedLine.getAddress(), RequestType.Cache_Write);
				}
				updateStateOfCacheLine(evictedLine.getAddress(), MESI.INVALID);
			}
		}
		
		sendResponseToWaitingEvent(eventsToBeServed);
	}
	
	public void sendMemResponse(AddressCarryingEvent event)
	{
		event.update(event.getEventQ(), 0,
			event.getProcessingElement(), event.getRequestingElement(),
			RequestType.Mem_Response);
		
		if(getComInterface()!=event.getProcessingElement().getComInterface()) {
			sendEvent(event);
		} else {
			event.getProcessingElement().handleEvent(event.getEventQ(), event);
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
	
	public CacheLine getCacheLine(int idx) {
		return this.lines[idx];
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
			CacheLine ll = getCacheLine(index);
			// If the tag is matching, we have a hit
			if(ll.hasTagMatch(tag) && (ll.getState() != MESI.INVALID)) {
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
	
	private void makeCache()
	{
		lines = new CacheLine[numLines];
		for(int i = 0; i < numLines; i++)
		{
			lines[i] = new CacheLine(i);
		}
	}
	
	private int getNumLines()
	{
		long totSize = size;
		return (int)(totSize / (long)(blockSize));
	}
	
	protected CacheLine accessAndMark(long addr) {
		CacheLine cl = access(addr);
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
			CacheLine ll = getCacheLine(nextIdx);
			if (ll.getTag() == tag && ll.getState() != MESI.INVALID) 
			{	
				addressAlreadyPresent = true;
				fillLine = ll;
				break;
			}
		}
		
		for (int idx = 0;!addressAlreadyPresent && idx < assoc; idx++) 
		{
			int nextIdx = getNextIdx(startIdx, idx);
			CacheLine ll = getCacheLine(nextIdx);
			if (!(ll.isValid())) 
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
				CacheLine ll = getCacheLine(index);
				if(minTimeStamp > ll.getTimestamp()) 
				{
					minTimeStamp = ll.getTimestamp();
					fillLine = ll;
				}
			}
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
			event.setEventTime(processingCache.getLatency());
			
			if(addEventAtLowerCache((AddressCarryingEvent)event) == false)
				break;
		}
	}
	
	//getters and setters
	public MissStatusHoldingRegister getMissStatusHoldingRegister() {
		return missStatusHoldingRegister;
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
		}
		
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
}