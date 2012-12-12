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
		
		public int noOfRequests;
		public int noOfAccesses;public int noOfResponsesReceived;public int noOfResponsesSent;public int noOfWritesForwarded;
		public int noOfWritesReceived;
		public int hits;
		public int misses;
		public int evictions;
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
				missStatusHoldingRegister = new Mode1MSHR(500);
			}
			else
			{
				missStatusHoldingRegister = new Mode1MSHR(40000);
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
		
		
		
		public void handleEvent(EventQueue eventQ, Event event)
		{
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
					|| event.getRequestType() == RequestType.Send_Mem_Response_Invalidate 
					||  event.getRequestType() == RequestType.Send_Mem_Response_On_WriteHit ){
				this.handleAccessWithDirectoryUpdates(eventQ, (AddressCarryingEvent) event);
			}
			else if (event.getRequestType() == RequestType.Send_Mem_Response_On_WriteHit)
			{
				AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
				memResponseUpdateDirectory(addrEvent.coreId, addrEvent.getAddress() >>> blockSizeBits, event, addrEvent.getAddress() );
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
		{noOfAccesses++;
		if(event.getRequestType() == RequestType.Cache_Write)
		{
			noOfWritesReceived++;
		}
			//update counters
			if(this.isLastLevel){
				Counters.incrementDcache2Access(1);
			}
			else{
				this.containingMemSys.getCore().powerCounters.incrementDcacheAccess(1);
			}
			
			RequestType requestType = event.getRequestType();
			long address = event.getAddress();
			
			CacheLine cl = this.processRequest(requestType, address);
			
			//IF HIT
			if (cl != null || missStatusHoldingRegister.containsWriteOfEvictedLine(address) )
			{
				
				if(this.coherence == CoherenceType.Directory 
						&& event.getRequestType() == RequestType.Cache_Write)
				{
					writeHitUpdateDirectory(event.coreId,( address>>> blockSizeBits ), event, address);
				}
				/*if(this.levelFromTop == CacheType.Lower){
					System.out.println(event.getEventTime()+"  cache hit for address "+ event.getAddress() + "with tag = "+ computeTag(event.getAddress()));
				}*/
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

		private void handleAccessWithDirectoryUpdates(EventQueue eventQ,
				AddressCarryingEvent event) {
			//Writeback
			if(this.levelFromTop != CacheType.L1)
			{
				System.err.println(" other than L1 handling AccessWith Directory "+ " coreID =  "+  event.coreId + "  " + this.levelFromTop  + " " + event.getRequestType());
				System.exit(1);
			}
			
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
			if(RequestType.Send_Mem_Response_On_WriteHit == event.getRequestType())
			{
				handleInvalidate(event);
				memResponseUpdateDirectory(event.coreId,event.getAddress() >>> blockSizeBits , event, event.getAddress());
				return ;
			}
			else if(RequestType.Send_Mem_Response_Invalidate == event.getRequestType())
			{
				handleInvalidate(event);
			}

			sendMemResponse(event);
		}
		protected void handleMemResponse(EventQueue eventQ, Event event)
		{noOfResponsesReceived++;
			/*Response for a read/write miss. Thus incrementing counters here as well*/
			if(this.isLastLevel){
				Counters.incrementDcache2Access(1);
			}
			else{
				this.containingMemSys.getCore().powerCounters.incrementDcacheAccess(1);
			}
			
			this.fillAndSatisfyRequests(eventQ, event, MESI.EXCLUSIVE);
		}
		
		private void handleInvalidate(Event event)
		{
			CacheLine cl = this.access(((AddressCarryingEvent)event).getAddress());
			if (cl != null)
				cl.setState(MESI.INVALID);
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
		
		
		
		private void sendResponseToWaitingEvent(ArrayList<Event> outstandingRequestList)
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
						ArchitecturalComponent.getCores()[eventPoppedOut.coreId].getExecEngine().getCoreMemorySystem().L1MissStatusHoldingRegister.removeEventIfAvailable(eventPoppedOut);
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
			
			ArrayList<Event> eventsToBeServed = missStatusHoldingRegister.removeRequests((AddressCarryingEvent)event);
			
			misses += eventsToBeServed.size();			
			noOfRequests += eventsToBeServed.size();
			//System.out.println(this.levelFromTop + "    hits : " + hits + "\tmisses : " + misses + "\trequests : " + noOfRequests);
			CacheLine evictedLine = this.fill(addr, stateToSet);
			if (evictedLine != null 
			    && evictedLine.getState() == MESI.MODIFIED 
			    && this.writePolicy != CacheConfig.WritePolicy.WRITE_THROUGH) //This does not ensure inclusiveness
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
																					   evictedLine.getTag() << blockSizeBits,
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
			ArrayList<Event> eventsToBeServed = missStatusHoldingRegister.removeRequests(event);
			hits += eventsToBeServed.size();
			noOfRequests += eventsToBeServed.size();
			//System.out.println(this.levelFromTop + "    hits : " + hits + "\tmisses : " + misses + "\trequests : " + noOfRequests);
			sendResponseToWaitingEvent(eventsToBeServed);
		}
		
		public void sendMemResponse(AddressCarryingEvent eventToRespondTo)
		{noOfResponsesSent++;
			eventToRespondTo.getRequestingElement().getPort().put(
										eventToRespondTo.update(
												eventToRespondTo.getEventQ(),
												0,
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
			centralizedDirectory.getPort().put(
							new AddressCarryingEvent(
									event.getEventQ(),
									centralizedDirectory.getLatencyDelay(),
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
			centralizedDirectory.getPort().put(
							new AddressCarryingEvent(
									event.getEventQ(),
									centralizedDirectory.getLatencyDelay(),
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
			centralizedDirectory.getPort().put(
							new AddressCarryingEvent(
									event.getEventQ(),
									centralizedDirectory.getLatencyDelay(),
									this,
									centralizedDirectory,
									RequestType.WriteMissDirectoryUpdate,
									address,
									(event).coreId));

		}
		
		private void memResponseUpdateDirectory( int requestingCore, long dirAddress,Event event, long address )
		{
			CentralizedDirectoryCache centralizedDirectory = MemorySystem.getDirectoryCache();
			centralizedDirectory.getPort().put(
							new AddressCarryingEvent(
									event.getEventQ(),
									centralizedDirectory.getLatencyDelay(),
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
			AddressCarryingEvent addrEvent = new AddressCarryingEvent(
					event.getEventQ(),
					centralizedDirectory.getLatencyDelay(),
					this,
					centralizedDirectory,
					RequestType.EvictionDirectoryUpdate,
					address,
					(event).coreId);
			centralizedDirectory.getPort().put(addrEvent);
			
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
		private int getNextIdx(int startIdx,int idx) {
			int index = startIdx +( idx << numSetsBits);
			return index;
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
		
		protected CacheLine write(long addr)
		{
			CacheLine cl = access(addr);
			if(cl != null) { 
				mark(cl);
				cl.setState(MESI.MODIFIED);
			}
			return cl;
		}
		
		protected CacheLine fill(long addr, MESI stateToSet) //Returns a copy of the evicted line
		{
			CacheLine evictedLine = null;
    		/* compute startIdx and the tag */
			int startIdx = getStartIdx(addr);
			long tag = computeTag(addr); 
			
			/* find any invalid lines -- no eviction */
			CacheLine fillLine = null;
			boolean evicted = false;

			for (int idx = 0; idx < assoc; idx++) 
			{
				int nextIdx = getNextIdx(startIdx, idx);
				CacheLine ll = this.lines[nextIdx];
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
					CacheLine ll = this.lines[index];
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
	
		public CacheLine processRequest(RequestType requestType, long addr)
		{
			CacheLine ll = null;
			if(requestType == RequestType.Cache_Read )  {
				ll = this.read(addr);
			} else if (requestType == RequestType.Cache_Write) {
				ll = this.write(addr);
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