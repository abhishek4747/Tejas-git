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

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import net.NOC.CONNECTIONTYPE;

import main.ArchitecturalComponent;
import memorysystem.directory.CentralizedDirectoryCache;
import memorysystem.directory.DirectoryEntry;
import memorysystem.nuca.NucaCache.Mapping;
import memorysystem.nuca.NucaCache.NucaType;
import memorysystem.nuca.NucaCacheBank;
import config.CacheConfig;
import config.CacheConfig.WritePolicy;
import config.CacheDataType;
import config.CacheEnergyConfig;
import config.Interconnect;
import config.EnergyConfig;
import config.SimulationConfig;
import config.SystemConfig;
import misc.Util;
import generic.*;

public class Cache extends SimulationElement
{
		public static enum CacheType{
			L1,
			iCache,
			Lower,
			Directory
		}
		
		public static enum CoherenceType{
			Snoopy,
			Directory,
			None,
			LowerLevelCoherent
		}
		
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
		protected Vector<Long> evictedLines = new Vector<Long>();
		
		public CoherenceType coherence = CoherenceType.None;
		public int numberOfBuses = 1;
		
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
		
		String cacheName;
		
		public void createLinkToNextLevelCache(Cache nextLevelCache) {
			this.nextLevel = nextLevelCache;			
			this.nextLevel.prevLevel.add(this);
		}
		
		public CacheConfig cacheConfig;
		public int id;
		
		ArrayList<AddressCarryingEvent> tmpHitEventList; //does NOT refer to a real
														//hardware structure; used to
														//avoid repeated creation of
														//ArrayList<AddressCarryingEvent>
														//objects when processing a
														//cache hit
		
		ArrayList<AddressCarryingEvent> eventsWaitingOnLowerMSHR;
		
			
		public Cache(String cacheName, int id, 
				CacheConfig cacheParameters, CoreMemorySystem containingMemSys)
		{
			super(cacheParameters.portType,
					cacheParameters.getAccessPorts(), 
					cacheParameters.getPortOccupancy(),
					cacheParameters.getLatency(),
					cacheParameters.operatingFreq);
			
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
			
			//this.levelFromTop = cacheParameters.getLevelFromTop();
			
			this.cacheConfig = cacheParameters;
			if(this.containingMemSys==null) {
				// Use the core memory system of core 0 for all the shared caches.
				this.isSharedCache = true;
				this.containingMemSys = MemorySystem.getCoreMemorySystems()[0];
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
			this.coherence = cacheParameters.getCoherence();
			this.numberOfBuses = cacheParameters.getNumberOfBuses();
			
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
		
		public Cache(
				String cacheName,
				CacheConfig cacheConfig,
				int size,
				int associativity,
				int blockSize,
				WritePolicy writePolicy,
				int mshrSize
				)
		{
			super(PortType.FirstComeFirstServe,
					2, 
					2,
					2,
					3600);
			
			// set the parameters
			this.blockSize = blockSize;
			this.assoc = associativity;
			this.size = size;
			this.blockSizeBits = Util.logbase2(blockSize);
			this.assocBits = Util.logbase2(assoc);
			this.numLines = getNumLines();
			this.numLinesBits = Util.logbase2(numLines);
			this.numSetsBits = numLinesBits - assocBits;
	
			this.writePolicy = writePolicy;
			
			this.isLastLevel = true;
			this.cacheName = cacheName;
			this.cacheConfig = cacheConfig;
			//this.nextLevelName = cacheParameters.getNextLevel();
//			this.enforcesCoherence = cacheParameters.isEnforcesCoherence();
			//this.coherence = cacheParameters.getCoherence();
			//this.numberOfBuses = cacheParameters.getNumberOfBuses();
			//if (this.coherence == CoherenceType.Snoopy)
			//	busController = new BusController(prevLevel, this, numberOfBuses, this, cacheParameters.getBusOccupancy());
			
			
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
			
			//missStatusHoldingRegister = new Mode3MSHR(blockSizeBits, cacheParameters.mshrSize);
			//if(this.level)
			missStatusHoldingRegister = new Mode3MSHR(blockSizeBits, 10000, null);
			
			tmpHitEventList = new ArrayList<AddressCarryingEvent>();
			eventsWaitingOnLowerMSHR = new ArrayList<AddressCarryingEvent>();
			
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
		
		
		private boolean printCacheDebugMessages = false;
		public void handleEvent(EventQueue eventQ, Event event)
		{
			if(printCacheDebugMessages==true) {
				if(event.getClass()==AddressCarryingEvent.class)// &&
//					((AddressCarryingEvent)event).getAddress()>>blockSizeBits==48037994l &&
//					this.levelFromTop==CacheType.L1)
				{
					System.out.println("CACHE : globalTime = " + GlobalClock.getCurrentTime() + 
						"\teventTime = " + event.getEventTime() + "\t" + event.getRequestType() +
						"\trequestingElelement = " + event.getRequestingElement() +
						"\taddress = " + ((AddressCarryingEvent)event).getAddress() +
						"\t" + this);
				}
			}
			
			if (event.getRequestType() == RequestType.Cache_Read
					|| event.getRequestType() == RequestType.Cache_Write)
			{
				this.handleAccess(eventQ, (AddressCarryingEvent) event);
				
				// Only for read/write we should send the request to the working set.
				// All other events like mem_response, etc do not count as an access 
				// for the working set
				long address = ((AddressCarryingEvent) event).getAddress();
				this.addToWorkingSet(address);
			}
			
			else if (event.getRequestType() == RequestType.Cache_Read_Writeback
					|| event.getRequestType() == RequestType.Send_Mem_Response 
					|| event.getRequestType() == RequestType.Send_Mem_Response_Invalidate) 
			{
				this.handleAccessWithDirectoryUpdates(eventQ, (AddressCarryingEvent) event);
			}
			
			else if (event.getRequestType() == RequestType.Mem_Response)
			{
				this.handleMemResponse(eventQ, event);
			}
			
			else if (event.getRequestType() == RequestType.MESI_Invalidate)
			{
				this.handleInvalidate((AddressCarryingEvent) event);
			}
		}
		
		public void handleAccess(EventQueue eventQ, AddressCarryingEvent event)
		{
			long address = event.getAddress();			
			RequestType requestType = event.getRequestType();
			
			if(requestType == RequestType.Cache_Write)
			{
				noOfWritesReceived++;
			}
			
			CacheLine cl = this.processRequest(requestType, address, event);
						
			//IF HIT
			if (cl != null)
			{
				hits++;
				noOfRequests++;
				noOfAccesses++;
				
				tmpHitEventList.clear();
				tmpHitEventList.add(event);
				sendResponseToWaitingEvent(tmpHitEventList);
			}
			
			//IF MISS
			else
			{
				//add to MSHR
				boolean newOMREntryCreated = missStatusHoldingRegister.addOutstandingRequest(event);
				
				if(this.coherence == CoherenceType.Directory 
						&& event.getRequestType() == RequestType.Cache_Write)
				{
					writeMissUpdateDirectory(event.coreId,( address>>> blockSizeBits ), event, address);
				}
				else if(this.coherence == CoherenceType.Directory 
						&& event.getRequestType() == RequestType.Cache_Read )
				{
					readMissUpdateDirectory(event.coreId,( address>>> blockSizeBits ), event, address);
				} 
				else 
				{
					if(newOMREntryCreated)
					{
						sendReadRequest(event);
					}
				}
			}
		}

		private void handleAccessWithDirectoryUpdates(EventQueue eventQ, AddressCarryingEvent event) 
		{
			if(event.getRequestType() == RequestType.Cache_Read_Writeback ) 
			{
				if (this.isLastLevel)
				{
					putEventToPort(event, MemorySystem.mainMemoryController, RequestType.Main_Mem_Write, true, true);
				}
				else
				{
					long addr = event.getAddress();
					long set_addr = addr>>blockSizeBits;
					CacheLine cl = access(addr);
					//System.out.println("DIRECTORY_CHECK : time = " + GlobalClock.getCurrentTime() +
					//		"\taddr = " +  addr + "\tset-addr = " + set_addr + 
					//		"\tstate-before = " + cl.getState());
					
					if(cl!=null) {
						cl.setState(MESI.EXCLUSIVE);
					}
					//propogateWrite(event);
				}
			}
			else if(event.getRequestType() == RequestType.Send_Mem_Response_Invalidate)
			{
				
				handleInvalidate(event);
			}

			sendMemResponseDirectory(event);
		}
		
		protected void handleMemResponse(EventQueue eventQ, Event event)
		{
			noOfResponsesReceived++;
			this.fillAndSatisfyRequests(eventQ, event, MESI.EXCLUSIVE);
		}
		
		public void handleInvalidate(Event event)
		{
			CacheLine cl = this.access(((AddressCarryingEvent)event).getAddress());
			if (cl != null) {
				cl.setState(MESI.INVALID);
			}
			
			if(this.coherence==CoherenceType.Directory) {
				long addr = ((AddressCarryingEvent)event).getAddress();
				int coreNumber = 0;
				if(this.containingMemSys!=null) {
					coreNumber = this.containingMemSys.getCore().getCore_number();
				}
				
				evictionUpdateDirectory(coreNumber, event, addr);
			}
			
			invalidatePreviousLevelCaches(((AddressCarryingEvent)event).getAddress());
		}
		
		private void invalidatePreviousLevelCaches(long addr)
		{
			// If I am invalidating a cache entry, I must inform all the previous level caches 
			// about the same
			if(prevLevel==null) {
				return;
			}
			
			for(int i=0; i<prevLevel.size(); i++) 
			{
				if(SystemConfig.interconnect == Interconnect.Bus)
				{
					Cache c = prevLevel.get(i);
					// Use event queue and core id as zero.
					EventQueue eventQueue = ArchitecturalComponent.getCores()[0].eventQueue;
					int coreNumber = 0;
					
					if(c.containingMemSys!=null) {
						eventQueue = c.containingMemSys.getCore().getEventQueue();
						coreNumber = c.containingMemSys.getCore().getCore_number();
					}
					
					c.getPort().put(
						new AddressCarryingEvent(
							eventQueue,
							c.getLatency(),
							this, 
							c,
							RequestType.MESI_Invalidate, 
							addr,
							coreNumber));
				}
				else if(SystemConfig.interconnect == Interconnect.Noc)
				{
					Cache c = prevLevel.get(i);
					EventQueue eventQueue = this.containingMemSys.getCore().getEventQueue();
					
					Vector<Integer> destinationId = c.containingMemSys.getCore().getId();
					AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(eventQueue,
							 0,this.containingMemSys.getCore(), 
							 this.containingMemSys.getCore().getRouter(),
							 RequestType.MESI_Invalidate,
							 addr,
							 this.containingMemSys.coreID,
							 this.containingMemSys.getCore().getId(),destinationId);
//					if(SystemConfig.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL) 
//					{
						this.containingMemSys.getCore().getRouter().getPort().put(eventToBeSent);
//					}
				}
			}
		}
		
		private void sendWriteRequest(AddressCarryingEvent receivedEvent)
		{
			if(this.isLastLevel)
			{
				//System.out.println(receivedEvent.getEventTime()+"  cache miss for address "+ receivedEvent.getAddress() + "with tag = "+ computeTag(receivedEvent.getAddress()));
				sendWriteRequestToMainMemory(receivedEvent);
			} 
			else
			{
				sendWriteRequestToLowerCache(receivedEvent);
			}			
		}		

		private void sendReadRequest(AddressCarryingEvent receivedEvent)
		{
			if(this.isLastLevel)
			{
				//System.out.println(receivedEvent.getEventTime()+"  cache miss for address "+ receivedEvent.getAddress() + "with tag = "+ computeTag(receivedEvent.getAddress()));
				sendReadRequestToMainMemory(receivedEvent);
			} 
			else
			{
				sendReadRequestToLowerCache(receivedEvent);
			}			
		}
		
		/*
		 * forward memory request to next level
		 * handle related lower level mshr scenarios
		 */
		public void sendWriteRequestToLowerCache(AddressCarryingEvent receivedEvent)
		{
			/*AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(
													receivedEvent.getEventQ(), 
													this.nextLevel.getLatency(), 
													this, 
													this.nextLevel, 
													RequestType.Cache_Read, 
													receivedEvent.getAddress(),
													receivedEvent.coreId);*/
			receivedEvent.update(receivedEvent.getEventQ(),
									this.nextLevel.getLatency(),
									this,
									this.nextLevel,
									RequestType.Cache_Write);
			
			addEventAtLowerCache(receivedEvent);
		}
		
		/*
		 * forward memory request to next level
		 * handle related lower level mshr scenarios
		 */
		public void sendReadRequestToLowerCache(AddressCarryingEvent receivedEvent)
		{
			receivedEvent.update(receivedEvent.getEventQ(),
									this.nextLevel.getLatency(),
									this,
									this.nextLevel,
									RequestType.Cache_Read);
			
			addEventAtLowerCache(receivedEvent);
		}
		
		private void sendWriteRequestToMainMemory(AddressCarryingEvent receivedEvent)
		{
			/*AddressCarryingEvent eventToForward = new AddressCarryingEvent(
													receivedEvent.getEventQ(),
												    MemorySystem.mainMemory.getLatency(), 
												    this, 
												    MemorySystem.mainMemory, 
												    RequestType.Main_Mem_Read, 
												    receivedEvent.getAddress(),
												    receivedEvent.coreId);*/
			if(SystemConfig.interconnect == Interconnect.Bus)
			{
				receivedEvent.update(receivedEvent.getEventQ(),
					MemorySystem.mainMemoryController.getLatency(),
					this,
					MemorySystem.mainMemoryController,
					RequestType.Main_Mem_Write);
				MemorySystem.mainMemoryController.getPort().put(receivedEvent);
			}
			
			else if(SystemConfig.interconnect == Interconnect.Noc)
			{
				Vector<Integer> destinationId = SystemConfig.nocConfig.nocElements.getMemoryControllerId(this.containingMemSys.getCore().getId());
				AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(receivedEvent.getEventQ(),
						 0,this.containingMemSys.getCore(), 
						 this.containingMemSys.getCore().getRouter(),
						 RequestType.Main_Mem_Write,
						 receivedEvent.getAddress(),receivedEvent.coreId,
						 this.containingMemSys.getCore().getId(),destinationId);
//				if(SystemConfig.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL) 
//				{
					this.containingMemSys.getCore().getRouter().getPort().put(eventToBeSent);
//				}
			}
		}
		
		private void sendReadRequestToMainMemory(AddressCarryingEvent receivedEvent)
		{
			/*AddressCarryingEvent eventToForward = new AddressCarryingEvent(
													receivedEvent.getEventQ(),
												    MemorySystem.mainMemory.getLatency(), 
												    this, 
												    MemorySystem.mainMemory, 
												    RequestType.Main_Mem_Read, 
												    receivedEvent.getAddress(),
												    receivedEvent.coreId);*/
			if(SystemConfig.interconnect == Interconnect.Bus)
			{
				receivedEvent.update(receivedEvent.getEventQ(),
						MemorySystem.mainMemoryController.getLatency(),
						this,
						MemorySystem.mainMemoryController,
						RequestType.Main_Mem_Read);
				MemorySystem.mainMemoryController.getPort().put(receivedEvent);
			}
			
			else if(SystemConfig.interconnect == Interconnect.Noc)
			{
				Vector<Integer> destinationId = SystemConfig.nocConfig.nocElements.getMemoryControllerId(this.containingMemSys.getCore().getId());
				AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(receivedEvent.getEventQ(),
						 0,this.containingMemSys.getCore(), 
						 this.containingMemSys.getCore().getRouter(),
						 RequestType.Main_Mem_Read,
						 receivedEvent.getAddress(),receivedEvent.coreId,
						 this.containingMemSys.getCore().getId(),destinationId);
//				if(SystemConfig.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL) 
//				{
					this.containingMemSys.getCore().getRouter().getPort().put(eventToBeSent);
//				}
			}
		}
		
		
		
		protected void sendResponseToWaitingEvent(ArrayList<AddressCarryingEvent> outstandingRequestList)
		{
			int numberOfWrites = 0;
			AddressCarryingEvent sampleWriteEvent = null;
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
							if (this.isLastLevel)
							{
								putEventToPort(eventPoppedOut,eventPoppedOut.getRequestingElement(), RequestType.Main_Mem_Write, true,true);
							}
							else if (this.coherence == CoherenceType.None)
							{
								numberOfWrites++;
								sampleWriteEvent = (AddressCarryingEvent) eventPoppedOut.clone();
							}
					}
					else
					{
						CacheLine cl = this.access(eventPoppedOut.getAddress());
						if (cl != null && cl.getState()!=MESI.MODIFIED) {
							cl.setState(MESI.MODIFIED);
							numberOfWrites++;
							sampleWriteEvent = (AddressCarryingEvent) eventPoppedOut;
							//manageAJustModifiedLine(eventPoppedOut);
						} else {
							numberOfWrites++;
							sampleWriteEvent = (AddressCarryingEvent) eventPoppedOut;
						}
					}
				}
			}
			
			if(numberOfWrites > 0)
			{
				//for all writes to the same block at this level,
				//one write is sent to the next level
				manageAJustModifiedLine(sampleWriteEvent);
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
		
		private boolean isSharedCache = true;
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
				this.nextLevel.getPort().put(event);
				this.nextLevel.workingSetUpdate();
				return true;
			}
			else
			{
				handleLowerMshrFull((AddressCarryingEvent)event);
				return false;
			}
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
		
		protected void fillAndSatisfyRequests(EventQueue eventQ, Event event, MESI stateToSet)
		{		
			long addr = ((AddressCarryingEvent)(event)).getAddress();
			ArrayList<AddressCarryingEvent> eventsToBeServed = missStatusHoldingRegister.removeRequestsByAddress((AddressCarryingEvent)event);
			
			misses += eventsToBeServed.size();			
			noOfRequests += eventsToBeServed.size();
			noOfAccesses+=eventsToBeServed.size() + 1;
			
			//System.out.println(this.levelFromTop + "    hits : " + hits + "\tmisses : " + misses + "\trequests : " + noOfRequests);
			CacheLine evictedLine = this.fill(addr, stateToSet);
			
			//This does not ensure inclusiveness
			if (evictedLine != null && 	evictedLine.getState() != MESI.INVALID) {
				// if the line is modified, the cache write policy must NOT be WRITE_THROUGH
				if((evictedLine.getState()==MESI.MODIFIED && this.writePolicy!=WritePolicy.WRITE_THROUGH))
				{
					//Update directory in case of eviction
					if(this.coherence==CoherenceType.Directory)
						
					{
							int requestingCore = containingMemSys.getCore().getCore_number();
							long address= evictedLine.getAddress();	//Generating an address of this cache line
							evictionUpdateDirectory(requestingCore, event, address);
					}
					else if (this.isLastLevel)
					{
							putEventToPort(event, MemorySystem.mainMemoryController, RequestType.Main_Mem_Write, false,true);
					}
					else
					{
						AddressCarryingEvent eventToForward = new AddressCarryingEvent(event.getEventQ(),
																					   this.nextLevel.getLatency(), 
																					   this,
																					   this.nextLevel, 
																					   RequestType.Cache_Write, 
																					   evictedLine.getAddress(),
																					   event.coreId);
						propogateWrite(eventToForward);
						
						//putEventToPort(event,this.nextLevel, RequestType.Cache_Write, false,true);
					}
				}
				
				invalidatePreviousLevelCaches(evictedLine.getAddress());
			}
			
			if(this.coherence == CoherenceType.Directory)
			{
				AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
				memResponseUpdateDirectory(addrEvent.coreId, addrEvent.getAddress() >>> blockSizeBits, addrEvent, addrEvent.getAddress());
			}
			//System.out.println( event.getEventTime()+"  response received for address "+((AddressCarryingEvent)event).getAddress() + "with tag = "+ computeTag(((AddressCarryingEvent)event).getAddress()));

			sendResponseToWaitingEvent(eventsToBeServed);
		}
		
		public void propogateWrite(AddressCarryingEvent event)
		{
			AddressCarryingEvent eventToForward = event.updateEvent(event.getEventQ(),
					   										this.nextLevel.getLatency(),
					   										this,
					   										this.nextLevel,
					   										RequestType.Cache_Write,
					   										event.getAddress(),
					   										event.coreId);
			
			addEventAtLowerCache(eventToForward);
			noOfWritesForwarded++;
		}
		
		private void processBlockAvailable(AddressCarryingEvent event)
		{
			ArrayList<AddressCarryingEvent> eventsToBeServed = missStatusHoldingRegister.removeRequestsByAddress(event);
			hits += eventsToBeServed.size();
			noOfRequests += eventsToBeServed.size();
			noOfAccesses+=eventsToBeServed.size();
			//System.out.println(this.levelFromTop + "    hits : " + hits + "\tmisses : " + misses + "\trequests : " + noOfRequests);
			sendResponseToWaitingEvent(eventsToBeServed);
		}
		
		public void sendMemResponse(AddressCarryingEvent eventToRespondTo)
		{
			long delay = 0;
			
			if(eventToRespondTo.getRequestingElement().getClass()==Cache.class &&
					((Cache)eventToRespondTo.getRequestingElement()).isSharedCache()
					|| ((Cache)eventToRespondTo.getProcessingElement()).isSharedCache())
			{
				// Any communication involving a shared cache should consider the network latency
				if(SystemConfig.interconnect == Interconnect.Bus)
				{
					// TODO : This delay should ideally be much lesser than the 
					// network delay
					delay += CentralizedDirectoryCache.getNetworkDelay();
				}
				else if(SystemConfig.interconnect == Interconnect.Noc)
				{
					//TODO
				}
			}
						
			noOfResponsesSent++;
			
			eventToRespondTo.update(
					eventToRespondTo.getEventQ(),
					delay,
					eventToRespondTo.getProcessingElement(),
					eventToRespondTo.getRequestingElement(),
					RequestType.Mem_Response);
			
			if(delay == 0)
			{
				//private caches communicating with each other
				//can optimize simulation by employing function call rather than an event
				if(eventToRespondTo.getProcessingElement().getClass() == Cache.class)
				{
					((Cache)eventToRespondTo.getProcessingElement()).handleMemResponse(
											eventToRespondTo.getEventQ(), eventToRespondTo);
				}
				else
				{
					((CoreMemorySystem)eventToRespondTo.getProcessingElement()).handleEvent(
							eventToRespondTo.getEventQ(), eventToRespondTo);
				}
			}
			else
			{
				eventToRespondTo.getRequestingElement().getPort().put(eventToRespondTo);
			}
		}
		
		public void sendMemResponseDirectory(AddressCarryingEvent eventToRespondTo)
		{
			if(SystemConfig.interconnect == Interconnect.Bus)
			{
				eventToRespondTo.getRequestingElement().getPort().put(
										eventToRespondTo.update(
												eventToRespondTo.getEventQ(),
												MemorySystem.getDirectoryCache().getNetworkDelay(),
												eventToRespondTo.getProcessingElement(),
												eventToRespondTo.getRequestingElement(),
												RequestType.Mem_Response));
			}
			else if(SystemConfig.interconnect == Interconnect.Noc)
			{
				Vector<Integer> destinationId = ArchitecturalComponent.getCores()[eventToRespondTo.coreId].getId();
				AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(eventToRespondTo.getEventQ(),
						 0,((Cache)eventToRespondTo.getProcessingElement()).containingMemSys.getCore(), 
						 ((Cache)eventToRespondTo.getProcessingElement()).containingMemSys.getCore().getRouter(),
						 RequestType.Mem_Response,
						 eventToRespondTo.getAddress(),eventToRespondTo.coreId,
						 this.containingMemSys.getCore().getId(),destinationId);
				this.containingMemSys.getCore().getRouter().getPort().put(eventToBeSent);
			}
		}
		
		private AddressCarryingEvent  putEventToPort(Event event, SimulationElement simElement, RequestType requestType, boolean flag, boolean time  )
		{
			//used when write_through policy or isLastLevel
			long eventTime = 0;
			if(time) {
				eventTime = simElement.getLatency();
			}
			else {
				eventTime = 1;
			}
			if(flag){
				simElement.getPort().put(
						event.update(
								event.getEventQ(),
								eventTime,
								this,
								simElement,
								requestType));
				return null;
			} 
			else 
			{
				AddressCarryingEvent addressEvent = new AddressCarryingEvent(event.getEventQ(),		
												    eventTime,this,simElement,
												    requestType, ((AddressCarryingEvent)event).getAddress(),																													  ((AddressCarryingEvent)event).coreId); 
													simElement.getPort().put(addressEvent);
				return addressEvent;
			}
		}
		/**
		 * 
		 * PROCESS DIRECTORY WRITE HIT
		 * Change cache line state to modified
		 * Directory state:
		 * invalid -> modified 
		 * modified -> modified,
		 * shared -> modified , invalidate,writeback others
		 * 
		 * */
		private void writeHitUpdateDirectory(int requestingCore, long dirAddress, Event event, long address){
			CentralizedDirectoryCache centralizedDirectory = MemorySystem.getDirectoryCache();
			
			long delay = 0;
			if(this.coherence==CoherenceType.Directory) {
				if(SystemConfig.interconnect == Interconnect.Bus)
				{
					delay += CentralizedDirectoryCache.getNetworkDelay() + centralizedDirectory.getLatencyDelay();
				}
			}
			if(SystemConfig.interconnect == Interconnect.Bus)
			{
				centralizedDirectory.getPort().put(
							new AddressCarryingEvent(
									event.getEventQ(),
									delay,
									this,
									centralizedDirectory,
									RequestType.WriteHitDirectoryUpdate,
									address,
									(event).coreId));
			}
			
			else if(SystemConfig.interconnect == Interconnect.Noc)
			{
				Vector<Integer> destinationId = getDirectoryId(address);
				
				AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(event.getEventQ(),
						 0,ArchitecturalComponent.getCores()[event.coreId], 
						 ArchitecturalComponent.getCores()[event.coreId].getRouter(),
						 RequestType.WriteHitDirectoryUpdate,
						 address,event.coreId,
						 ArchitecturalComponent.getCores()[event.coreId].getId(),destinationId);
				ArchitecturalComponent.getCores()[event.coreId].getRouter().
				getPort().put(eventToBeSent);
			}
		}

		/**
		 * PROCESS DIRECTORY READ MISS
		 *Cache block state -> 
		 *invalid -> shared
		 *modified -> shared 
		 *writeback to memory!
		 *
		 * Directory state:
		 *invalid -> shared
		 *modified -> shared
		 * */
		
		private void readMissUpdateDirectory(int requestingCore,long dirAddress, Event event, long address) {
			
			CentralizedDirectoryCache centralizedDirectory = MemorySystem.getDirectoryCache();
			
			long delay = 0;
			if(this.coherence==CoherenceType.Directory) {
				if(SystemConfig.interconnect == Interconnect.Bus)
					delay += CentralizedDirectoryCache.getNetworkDelay() + centralizedDirectory.getLatencyDelay();
			}
			if(SystemConfig.interconnect == Interconnect.Bus)
			{
				centralizedDirectory.getPort().put(
							new AddressCarryingEvent(
									event.getEventQ(),
									delay,
									this,
									centralizedDirectory,
									RequestType.ReadMissDirectoryUpdate,
									address,
									(event).coreId));
			}
			else if(SystemConfig.interconnect == Interconnect.Noc)
			{
				Vector<Integer> destinationId = getDirectoryId(address);
				AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(event.getEventQ(),
						 0,ArchitecturalComponent.getCores()[event.coreId], 
						 ArchitecturalComponent.getCores()[event.coreId].getRouter(),
						 RequestType.ReadMissDirectoryUpdate,
						 address,event.coreId,
						 ArchitecturalComponent.getCores()[event.coreId].getId(),destinationId);
				ArchitecturalComponent.getCores()[event.coreId].getRouter().
				getPort().put(eventToBeSent);
			}
		}

		/**
		 * 
		 * PROCESS DIRECTORY WRITE MISS
		 *cache state modified
		 *directory state:
		 *invalid -> modified
		 *modified -> modified , invalidate, update sharers
		 *shared -> modified, invalidate , update sharers
		 * 
		 * */
		private void writeMissUpdateDirectory(int requestingCore, long dirAddress, Event event, long address) {
			CentralizedDirectoryCache centralizedDirectory = MemorySystem.getDirectoryCache();
			long delay = 0;
			if(this.coherence==CoherenceType.Directory) {
				if(SystemConfig.interconnect == Interconnect.Bus)
					delay += CentralizedDirectoryCache.getNetworkDelay() + centralizedDirectory.getLatencyDelay();
			}
			if(SystemConfig.interconnect == Interconnect.Bus)
			{
				centralizedDirectory.getPort().put(
								new AddressCarryingEvent(
										event.getEventQ(),
										delay,
										this,
										centralizedDirectory,
										RequestType.WriteMissDirectoryUpdate,
										address,
										(event).coreId));
			}
			else if(SystemConfig.interconnect == Interconnect.Noc)
			{
				Vector<Integer> destinationId = getDirectoryId(address);
				
				AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(event.getEventQ(),
						 0,ArchitecturalComponent.getCores()[event.coreId], 
						 ArchitecturalComponent.getCores()[event.coreId].getRouter(),
						 RequestType.WriteMissDirectoryUpdate,
						 address,event.coreId,
						 ArchitecturalComponent.getCores()[event.coreId].getId(),destinationId);
				ArchitecturalComponent.getCores()[event.coreId].getRouter().
				getPort().put(eventToBeSent);
			}
		}
		
		private void memResponseUpdateDirectory( int requestingCore, long dirAddress,Event event, long address )
		{
			CentralizedDirectoryCache centralizedDirectory = MemorySystem.getDirectoryCache();
			long delay = 0;			
			if(this.coherence==CoherenceType.Directory) {
				if(SystemConfig.interconnect == Interconnect.Bus)
				{
					delay += CentralizedDirectoryCache.getNetworkDelay() + centralizedDirectory.getLatency();
				}
			}
			
			if(SystemConfig.interconnect == Interconnect.Bus)
			{
				centralizedDirectory.getPort().put(
							new AddressCarryingEvent(
									event.getEventQ(),
									delay,
									this,
									centralizedDirectory,
									RequestType.MemResponseDirectoryUpdate,
									address,
									(event).coreId));
			}
			else if(SystemConfig.interconnect == Interconnect.Noc)
			{
				Vector<Integer> destinationId = getDirectoryId(address);
				
				AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(event.getEventQ(),
						 0,ArchitecturalComponent.getCores()[event.coreId], 
						 ArchitecturalComponent.getCores()[event.coreId].getRouter(),
						 RequestType.MemResponseDirectoryUpdate,
						 address,event.coreId,
						 ArchitecturalComponent.getCores()[event.coreId].getId(),destinationId);
				ArchitecturalComponent.getCores()[event.coreId].getRouter().
				getPort().put(eventToBeSent);
			}
			
		}
		/**
		 * UPDATE DIRECTORY FOR EVICTION
		 * Update directory for evictedLine
		 * If modified, writeback, else just update sharers
		 * */
		private void evictionUpdateDirectory(int requestingCore, Event event, long address) {
			
			CentralizedDirectoryCache centralizedDirectory = MemorySystem.getDirectoryCache();
			long delay = 0;			
			if(this.coherence==CoherenceType.Directory) {
				if(SystemConfig.interconnect == Interconnect.Bus)
				{
					delay += CentralizedDirectoryCache.getNetworkDelay() + centralizedDirectory.getLatency();
				}
			}
			if(SystemConfig.interconnect == Interconnect.Bus)
			{
				AddressCarryingEvent addrEvent = new AddressCarryingEvent(
					event.getEventQ(),
					delay,
					this,
					centralizedDirectory,
					RequestType.EvictionDirectoryUpdate,
					address,
					(event).coreId);
				centralizedDirectory.getPort().put(addrEvent);
			}
			else if(SystemConfig.interconnect == Interconnect.Noc)
			{
				Vector<Integer> destinationId = getDirectoryId(address);
				
				AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(event.getEventQ(),
						 0,ArchitecturalComponent.getCores()[event.coreId], 
						 ArchitecturalComponent.getCores()[event.coreId].getRouter(),
						 RequestType.EvictionDirectoryUpdate,
						 address,event.coreId,
						 ArchitecturalComponent.getCores()[event.coreId].getId(),destinationId);
				ArchitecturalComponent.getCores()[event.coreId].getRouter().
				getPort().put(eventToBeSent);
			}
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

		protected CacheLine read(long addr)
		{
			CacheLine cl = access(addr);
			if(cl != null) {
				mark(cl);
			}
			return cl;
		}
		
		protected CacheLine write(long addr, Event event)
		{
			CacheLine cl = access(addr);
			if(cl != null) { 
				mark(cl);
				
				if(cl.getState()!=MESI.MODIFIED) {
					
					cl.setState(MESI.MODIFIED);
					
					manageAJustModifiedLine((AddressCarryingEvent)event);
				}
			}
			return cl;
		}
		
		private void manageAJustModifiedLine(AddressCarryingEvent event) {
			
			long addr = event.getAddress(); 
					
			// Send request to lower cache.
			if(this.coherence==CoherenceType.None && this.isLastLevel==false) {
				AddressCarryingEvent newEvent  = (AddressCarryingEvent)event.clone();
				newEvent.setAddress(addr);
				sendWriteRequest(newEvent);
			} else if (this.coherence==CoherenceType.None && this.isLastLevel==true) {
				AddressCarryingEvent newEvent  = (AddressCarryingEvent)event.clone();
				newEvent.setAddress(addr);
				sendWriteRequestToMainMemory(newEvent);
			} else if(this.coherence == CoherenceType.Directory) {
				// If I have coherence, I should send this request to Directory
				writeHitUpdateDirectory(event.coreId,( addr>>> blockSizeBits ), event.clone(), addr);
			} else {
				misc.Error.showErrorAndExit("Invalid cache setup !!");
			}			
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
	
		public CacheLine processRequest(RequestType requestType, long addr, Event event)
		{
			CacheLine ll = null;
			if(requestType == RequestType.Cache_Read )  {
				ll = this.read(addr);
			} else if (requestType == RequestType.Cache_Write) {
				ll = this.write(addr, event);
			}
			return ll;
		}
		
		public void populateConnectedMSHR()
		{
			//if mode3
			//((Mode3MSHR)(missStatusHoldingRegister)).populateConnectedMSHR(this.prevLevel);
		}
		
		public void handleLowerMshrFull( AddressCarryingEvent event)
		{
			AddressCarryingEvent eventToBeSent = (AddressCarryingEvent)event.clone();
			eventToBeSent.actualProcessingElement = eventToBeSent
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
				
				((AddressCarryingEvent)event).setProcessingElement(((AddressCarryingEvent)event).actualProcessingElement);
				((AddressCarryingEvent)event).actualProcessingElement = null;
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
		
		public Vector<Integer> getDirectoryId(long addr)
		{
			Vector<Integer> destinationBankId = new Vector<Integer>();
			int bankNumber= SystemConfig.nocConfig.nocElements.l1Directories.get(0).getDirectoryNumber(addr);
			destinationBankId = SystemConfig.nocConfig.nocElements.l1Directories.get(bankNumber).getId();
			return destinationBankId;
		}
}