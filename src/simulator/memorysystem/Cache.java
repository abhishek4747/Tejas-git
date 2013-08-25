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

import java.util.*;

import power.Counters;
import main.ArchitecturalComponent;
import memorysystem.directory.CentralizedDirectoryCache;
import memorysystem.directory.DirectoryEntry;
import memorysystem.nuca.NucaCache.NucaType;
import memorysystem.nuca.NucaCacheBank;
import config.CacheConfig;
import config.CacheConfig.WritePolicy;
import config.SimulationConfig;
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
		
		public CacheType levelFromTop; 
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
		
		public Cache(CacheConfig cacheParameters, CoreMemorySystem containingMemSys)
		{
			super(cacheParameters.portType,
					cacheParameters.getAccessPorts(), 
					cacheParameters.getPortOccupancy(),
					cacheParameters.getLatency(),
					cacheParameters.operatingFreq);
			
			this.containingMemSys = containingMemSys;
			
			// set the parameters
			this.blockSize = cacheParameters.getBlockSize();
			this.assoc = cacheParameters.getAssoc();
			this.size = cacheParameters.getSize(); // in kilobytes
			this.blockSizeBits = Util.logbase2(blockSize);
			this.assocBits = Util.logbase2(assoc);
			this.numLines = getNumLines();
			this.numLinesBits = Util.logbase2(numLines);
			this.numSetsBits = numLinesBits - assocBits;
	
			this.writePolicy = cacheParameters.getWritePolicy();
			this.levelFromTop = cacheParameters.getLevelFromTop();
			this.isLastLevel = cacheParameters.isLastLevel();
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
			
			//missStatusHoldingRegister = new Mode3MSHR(blockSizeBits, cacheParameters.mshrSize);
			if(this.levelFromTop == CacheType.L1 || this.levelFromTop == CacheType.iCache)
			{
				missStatusHoldingRegister = new Mode3MSHR(blockSizeBits, 500, this.containingMemSys.core.eventQueue);
			}
			else
			{
				if(SimulationConfig.nucaType == NucaType.NONE) 
				{
					missStatusHoldingRegister = new Mode3MSHR(blockSizeBits, 40000, null);
				}
			}
			this.nucaType = NucaType.NONE;
		}
		
		public Cache(
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
			this.size = size; // in kilobytes
			this.blockSizeBits = Util.logbase2(blockSize);
			this.assocBits = Util.logbase2(assoc);
			this.numLines = getNumLines();
			this.numLinesBits = Util.logbase2(numLines);
			this.numSetsBits = numLinesBits - assocBits;
	
			this.writePolicy = writePolicy;
			this.levelFromTop = CacheType.L1;
			this.isLastLevel = true;
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
			missStatusHoldingRegister = new Mode1MSHR(10000);
			
		}
		
		
		private boolean printCacheDebugMessages = false;
		public void handleEvent(EventQueue eventQ, Event event)
		{
			// Sanity check for iCache
			if(this.levelFromTop==CacheType.iCache && event.getRequestType()==RequestType.Cache_Read && ((AddressCarryingEvent)event).getAddress()==-1) {
				misc.Error.showErrorAndExit("iCache is getting request for invalid ip : -1");
			}
			
			if(printCacheDebugMessages==true) {
				if(event.getClass()==AddressCarryingEvent.class &&
					((AddressCarryingEvent)event).getAddress()>>blockSizeBits==48037994l &&
					this.levelFromTop==CacheType.L1)
				{
					System.out.println("CACHE : globalTime = " + GlobalClock.getCurrentTime() + 
						"\teventTime = " + event.getEventTime() + "\t" + event.getRequestType() +
						"\trequestingElelement = " + event.getRequestingElement() +
						"\t" + this);
				}
			}
			
			if(this.levelFromTop == CacheType.L1 || this.levelFromTop == CacheType.iCache)
			{
				/*if(event.coreId != this.containingMemSys.coreID)
				{
					System.out.println("this print is from : " + this.levelFromTop + " of " + this.containingMemSys.coreID);
					event.dump();
					ArchitecturalComponent.dumpOutStandingLoads();
					ArchitecturalComponent.dumpAllEventQueues();
					ArchitecturalComponent.dumpAllMSHRs();
					misc.Error.showErrorAndExit("coreIDs mismatch!!");					
				}*/
			}
			
			if (event.getRequestType() == RequestType.Cache_Read
					|| event.getRequestType() == RequestType.Cache_Write)
			{
				this.handleAccess(eventQ, (AddressCarryingEvent) event);
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
			
			else if (event.getRequestType() == RequestType.MSHR_Full)
			{
				// Reset the requestType to actualRequestType
				event.setRequestType(((AddressCarryingEvent)event).actualRequestType);
				((AddressCarryingEvent)event).actualRequestType = null;
				Cache processingCache = (Cache)event.getProcessingElement();
				if (processingCache.addEvent((AddressCarryingEvent)event) == false)
				{
					missStatusHoldingRegister.handleLowerMshrFull((AddressCarryingEvent)event);
				}
			}
		}
		
		public void handleAccess(EventQueue eventQ, AddressCarryingEvent event)
		{
			noOfAccesses++;
		
			if(event.getRequestType() == RequestType.Cache_Write)
			{
				noOfWritesReceived++;
			}
			
			//update counters
			if(this.isLastLevel){
				Counters.incrementDcache2Access(1);
			} else {
				this.containingMemSys.getCore().powerCounters.incrementDcacheAccess(1);
			}
			
			RequestType requestType = event.getRequestType();
			long address = event.getAddress();
			
			CacheLine cl = this.processRequest(requestType, address, event);
			
//			if(this.levelFromTop==CacheType.L1) {
//				System.out.println("numAccess = " + noOfAccesses + " addr = " + address + 
//						" tag+set = " + (address>>>blockSizeBits));
//			}
			
			//IF HIT
			if (cl != null || missStatusHoldingRegister.containsWriteOfEvictedLine(address) )
			{
				
				/*if(this.levelFromTop == CacheType.Lower){
					System.out.println(event.getEventTime()+"  cache hit for address "+ event.getAddress() + "with tag = "+ computeTag(event.getAddress()));
				}*/
			//	int setNumber = this.getStartIdx(event.getAddress());
				//System.out.println(setNumber+" setNumber "+ this.getId() + " " + event.coreId+" " + event.getAddress());
				//setAccessFreq[setNumber][event.coreId]++;
				processBlockAvailable(event);				
			}
			
			//IF MISS
			else
			{	
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
	
					sendReadRequest(event);
				}
			}
		}

		private void handleAccessWithDirectoryUpdates(EventQueue eventQ, AddressCarryingEvent event) 
		{
			if(event.getRequestType() == RequestType.Cache_Read_Writeback ) 
			{
				if (this.isLastLevel)
				{
					putEventToPort(event, MemorySystem.mainMemory, RequestType.Main_Mem_Write, true, true);
				}
				else
				{
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
			
			/*Response for a read/write miss. Thus incrementing counters here as well*/
			if(this.isLastLevel){
				Counters.incrementDcache2Access(1);
			}
			else{
				this.containingMemSys.getCore().powerCounters.incrementDcacheAccess(1);
			}
			
			this.fillAndSatisfyRequests(eventQ, event, MESI.EXCLUSIVE);
		}
		
		public void handleInvalidate(Event event)
		{
			CacheLine cl = this.access(((AddressCarryingEvent)event).getAddress());
			if (cl != null) {
				cl.setState(MESI.INVALID);
			}
			
			invalidatePreviousLevelCaches((AddressCarryingEvent)event);
		}
		
		private void invalidatePreviousLevelCaches(AddressCarryingEvent event)
		{
			// If I am invalidating a cache entry, I must inform all the previous level caches 
			// about the same
			if(prevLevel==null) {
				return;
			}
			
			for(int i=0; i<prevLevel.size(); i++) {
				Cache c = prevLevel.get(i);
				c.getPort().put(
					new AddressCarryingEvent(
						c.containingMemSys.getCore().getEventQueue(),
						c.getLatency(),
						this, 
						c,
						RequestType.MESI_Invalidate, 
						((AddressCarryingEvent)event).getAddress(),
						c.containingMemSys.getCore().getCore_number()));
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
			
			boolean isAddedinLowerMshr = this.nextLevel.addEvent(receivedEvent);
			if(!isAddedinLowerMshr)
			{
				missStatusHoldingRegister.handleLowerMshrFull(receivedEvent);
			}
		}
		
		/*
		 * forward memory request to next level
		 * handle related lower level mshr scenarios
		 */
		public void sendReadRequestToLowerCache(AddressCarryingEvent receivedEvent)
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
									RequestType.Cache_Read);
			
			boolean isAddedinLowerMshr = this.nextLevel.addEvent(receivedEvent);
			if(!isAddedinLowerMshr)
			{
				missStatusHoldingRegister.handleLowerMshrFull(receivedEvent);
			}
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
			
			receivedEvent.update(receivedEvent.getEventQ(),
					MemorySystem.mainMemory.getLatency(),
					this,
					MemorySystem.mainMemory,
					RequestType.Main_Mem_Write);

			MemorySystem.mainMemory.getPort().put(receivedEvent);
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
			
			receivedEvent.update(receivedEvent.getEventQ(),
					MemorySystem.mainMemory.getLatency(),
					this,
					MemorySystem.mainMemory,
					RequestType.Main_Mem_Read);

			MemorySystem.mainMemory.getPort().put(receivedEvent);
		}
		
		
		
		private void sendResponseToWaitingEvent(ArrayList<AddressCarryingEvent> outstandingRequestList)
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
					if(this.levelFromTop == CacheType.L1)
					{
						ArchitecturalComponent.getCores()[eventPoppedOut.coreId].getExecEngine().getCoreMemorySystem().L1MissStatusHoldingRegister.removeRequestsByRequestTypeAndAddressIfAvailable(eventPoppedOut);
						/*ArchitecturalComponent.getCores()[eventPoppedOut.coreId].getExecEngine().getCoreMemorySystem().L1MissStatusHoldingRegister.removeRequests(eventPoppedOut);*/
					}
					/*if(this.levelFromTop == CacheType.Lower)
						System.out.println(eventPoppedOut.getEventTime()+" write removed from mshr "+ eventPoppedOut.getAddress() + "tag "+ computeTag(eventPoppedOut.getAddress()));	*/
					if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
						{
								if (this.isLastLevel)
								{
									putEventToPort(eventPoppedOut,eventPoppedOut.getRequestingElement(), RequestType.Main_Mem_Write, true,true);
									//putEventToPort(eventPoppedOut,MemorySystem.mainMemory, RequestType.Main_Mem_Write, false,true);
								}
								else if (this.coherence == CoherenceType.None)
								{
										//putEventToPort(eventPoppedOut,this.nextLevel, RequestType.Cache_Write, true,true);
									numberOfWrites++;
									sampleWriteEvent = (AddressCarryingEvent) eventPoppedOut.clone();
								}
						}
						else
						{
								CacheLine cl = this.access(eventPoppedOut.getAddress());
								if (cl != null)
								{
										cl.setState(MESI.MODIFIED);
								}
								else
								{
									numberOfWrites++;
									sampleWriteEvent = (AddressCarryingEvent) eventPoppedOut.clone();
								}
						}
				}
			}
			
			if(numberOfWrites > 0)
			{
				//for all writes to the same block at this level,
				//one write is sent to the next level
				propogateWrite(sampleWriteEvent);
			}
		}
		
		
		
		
		
		/*
		 * called by higher level cache
		 *returned value signifies whether the event will be saved in mshr or not
		 * */
		public boolean addEvent(AddressCarryingEvent addressEvent)
		{
				
			if(missStatusHoldingRegister.isFull())
			{
				return false;
			}
			
			long address = addressEvent.getAddress();
			//System.out.println(address + " in l1 setNumber : " +this.getStartIdx(address));
			boolean entryCreated = missStatusHoldingRegister.addOutstandingRequest(addressEvent);
			if(entryCreated)
			{
				this.getPort().put(addressEvent);
			}
			else
			{
				//if mode3mshr
				
				AddressCarryingEvent eventToForward = ((Mode3MSHR)missStatusHoldingRegister).getMshrEntry(addressEvent.getAddress()).eventToForward; 
				/*
				 * it is possible that an onrEntry corresponding to the line exists, and has a write as eventToForward
				 * in this situation, we are not expecting any memResponse for the line from below
				 * therefore, we have to schedule a handleAccess
				 */
				if(eventToForward != null &&
						eventToForward.getRequestType() == RequestType.Cache_Write)
				{
					//handleAccess(addressEvent.getEventQ(), addressEvent);
					/*
					 * directly calling handle access does not include hit-time
					 * instead, we schedule an event at time cur + hit-time
					 */
					this.getPort().put(addressEvent);
				}
			}
			
			return true;
		}
		
		protected void fillAndSatisfyRequests(EventQueue eventQ, Event event, MESI stateToSet)
		{		
			long addr = ((AddressCarryingEvent)(event)).getAddress();
			
			ArrayList<AddressCarryingEvent> eventsToBeServed = missStatusHoldingRegister.removeRequestsByAddress((AddressCarryingEvent)event);
			
			misses += eventsToBeServed.size();			
			noOfRequests += eventsToBeServed.size();
			//System.out.println(this.levelFromTop + "    hits : " + hits + "\tmisses : " + misses + "\trequests : " + noOfRequests);
			CacheLine evictedLine = this.fill(addr, stateToSet);
			
			//This does not ensure inclusiveness
			if (
				evictedLine != null && 
				evictedLine.getState() != MESI.INVALID && 
				// if the line is modified, the cache write policy must NOT be WRITE_THROUGH
				((evictedLine.getState()!=MESI.MODIFIED) || (evictedLine.getState()==MESI.MODIFIED && this.writePolicy!=WritePolicy.WRITE_THROUGH))
			)
			{
				//Update directory in case of eviction
					if(this.coherence==CoherenceType.Directory)
						
					{
							int requestingCore = containingMemSys.getCore().getCore_number();
							long address= evictedLine.getAddress();	//Generating an address of this cache line
							evictionUpdateDirectory(requestingCore,evictedLine.getTag(),event,address);
					}
					else if (this.isLastLevel)
					{
							putEventToPort(event, MemorySystem.mainMemory, RequestType.Main_Mem_Write, false,true);
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
			
			boolean isAdded = this.nextLevel.addEvent(eventToForward);
			if( !isAdded )
			{
				boolean entryCreated =  missStatusHoldingRegister.addOutstandingRequest( eventToForward );
				if (entryCreated)
				{
					missStatusHoldingRegister.handleLowerMshrFull( eventToForward );
				}
			}
			else
			{
				noOfWritesForwarded++;
			}
		}
		
		private void processBlockAvailable(AddressCarryingEvent event)
		{
			ArrayList<AddressCarryingEvent> eventsToBeServed = missStatusHoldingRegister.removeRequestsByAddress(event);
			hits += eventsToBeServed.size();
			noOfRequests += eventsToBeServed.size();
			//System.out.println(this.levelFromTop + "    hits : " + hits + "\tmisses : " + misses + "\trequests : " + noOfRequests);
			sendResponseToWaitingEvent(eventsToBeServed);
		}
		
		public void sendMemResponse(AddressCarryingEvent eventToRespondTo)
		{
			long delay = eventToRespondTo.getRequestingElement().getLatency();
			
			// ------- Add the network delay when coherent caches are communicating with each other -----------
			// L1 data cache
			if(eventToRespondTo.getRequestingElement().getClass()==Cache.class &&
				((Cache)eventToRespondTo.getRequestingElement()).coherence==CoherenceType.Directory)
			{
				delay += CentralizedDirectoryCache.getNetworkDelay(); 
			}
			
			// Instruction cache connected directly to the lower cache
			// Here, coherence is not there but network delay must be added
			// We are checking for connection to lower cache because if tomorrow we have a private L2 cache, this
			// network delay must not be added.
			if(eventToRespondTo.getRequestingElement().getClass()==Cache.class &&
				((Cache)eventToRespondTo.getRequestingElement()).levelFromTop==CacheType.iCache &&
				((Cache)eventToRespondTo.getRequestingElement()).nextLevel.levelFromTop==CacheType.Lower)
			{
				delay += CentralizedDirectoryCache.getNetworkDelay(); 
			}

						
			noOfResponsesSent++;
			eventToRespondTo.getRequestingElement().getPort().put(
										eventToRespondTo.update(
												eventToRespondTo.getEventQ(),
												delay,
												eventToRespondTo.getProcessingElement(),
												eventToRespondTo.getRequestingElement(),
												RequestType.Mem_Response));
		}
		
		public void sendMemResponseDirectory(AddressCarryingEvent eventToRespondTo)
		{
			eventToRespondTo.getRequestingElement().getPort().put(
										eventToRespondTo.update(
												eventToRespondTo.getEventQ(),
												MemorySystem.getDirectoryCache().getNetworkDelay(),
												eventToRespondTo.getProcessingElement(),
												eventToRespondTo.getRequestingElement(),
												RequestType.Mem_Response));
		}
		
		private AddressCarryingEvent  putEventToPort(Event event, SimulationElement simElement, RequestType requestType, boolean flag, boolean time  )
		{
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
			} else {
				AddressCarryingEvent addressEvent = 	new AddressCarryingEvent( 	event.getEventQ(),
																																									    eventTime,
																																									   this,
																																									   simElement,
																																									  requestType,
																																									  ((AddressCarryingEvent)event).getAddress(),
																																									  ((AddressCarryingEvent)event).coreId); 
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
				delay += CentralizedDirectoryCache.getNetworkDelay() + centralizedDirectory.getLatencyDelay();
			}
			
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
				delay += CentralizedDirectoryCache.getNetworkDelay() + centralizedDirectory.getLatencyDelay();
			}
			
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
				delay += CentralizedDirectoryCache.getNetworkDelay() + centralizedDirectory.getLatencyDelay();
			}
			
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
		
		private void memResponseUpdateDirectory( int requestingCore, long dirAddress,Event event, long address )
		{
			CentralizedDirectoryCache centralizedDirectory = MemorySystem.getDirectoryCache();
			long delay = 0;			
			if(this.coherence==CoherenceType.Directory) {
				delay += CentralizedDirectoryCache.getNetworkDelay() + centralizedDirectory.getLatency();
			}
			
			
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
		/**
		 * UPDATE DIRECTORY FOR EVICTION
		 * Update directory for evictedLine
		 * If modified, writeback, else just update sharers
		 * */
		private void evictionUpdateDirectory(int requestingCore, long dirAddress,Event event, long address) {
			if(debug && this.levelFromTop == CacheType.L1)System.out.println("tag of line evicted " + (address >>>  this.blockSizeBits)+ " coreID  " + event.coreId );
			CentralizedDirectoryCache centralizedDirectory = MemorySystem.getDirectoryCache();
			long delay = 0;			
			if(this.coherence==CoherenceType.Directory) {
				delay += CentralizedDirectoryCache.getNetworkDelay() + centralizedDirectory.getLatency();
			}
			
			AddressCarryingEvent addrEvent = new AddressCarryingEvent(
					event.getEventQ(),
					delay,
					this,
					centralizedDirectory,
					RequestType.EvictionDirectoryUpdate,
					address,
					(event).coreId);
			centralizedDirectory.getPort().put(addrEvent);
			
			invalidatePreviousLevelCaches((AddressCarryingEvent)event);			
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
			long totSize = size * 1024;
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
					
					// Send request to lower cache.
					if(this.coherence==CoherenceType.None && this.isLastLevel==false) {
						AddressCarryingEvent newEvent  = (AddressCarryingEvent)event.clone();
						newEvent.setAddress(addr);
						sendWriteRequest(newEvent);
					}
					
					// If I have coherence, I should send this request to Directory 
					if(this.coherence == CoherenceType.Directory) {
						writeHitUpdateDirectory(event.coreId,( addr>>> blockSizeBits ), event.clone(), addr);
					}
				}
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
		
		
		//getters and setters
		public MissStatusHoldingRegister getMissStatusHoldingRegister() {
			return missStatusHoldingRegister;
		}
		
		public String toString()
		{
			StringBuilder s = new StringBuilder();
			s.append(this.levelFromTop + " : ");
			if(this.levelFromTop == CacheType.L1 || this.levelFromTop == CacheType.iCache)
			{
				s.append(this.containingMemSys.coreID);
			}
			return s.toString();
		}
}