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
import memorysystem.directory.CentralizedDirectoryCache;
import memorysystem.snoopyCoherence.BusController;
import config.CacheConfig;
import emulatorinterface.Newmain;
import misc.Util;
import generic.*;

public class Cache extends SimulationElement
{
		public static enum CacheType{
			L1,
			iCache,
			Lower
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
		protected int assoc;
		protected int assocBits; // in bits
		protected int size; // MegaBytes
		protected int numLines;
		protected int numLinesBits;
		protected int numSetsBits;
		protected double timestamp;
		protected int numLinesMask;
		protected Vector<Long> evictedLines = new Vector<Long>();
		
		public CoherenceType coherence = CoherenceType.None;
		public int numberOfBuses = 1;
		public BusController busController = null;
		
		public CacheType levelFromTop; 
		public boolean isLastLevel; //Tells whether there are any more levels of cache
		protected CacheConfig.WritePolicy writePolicy; //WRITE_BACK or WRITE_THROUGH
		
		public String nextLevelName; //Name of the next level cache according to the configuration file
		public ArrayList<Cache> prevLevel = new ArrayList<Cache>(); //Points towards the previous level in the cache hierarchy
		public Cache nextLevel; //Points towards the next level in the cache hierarchy
		protected CacheLine lines[];
		
		public MissStatusHoldingRegister missStatusHoldingRegister;
		public ArrayList<MissStatusHoldingRegister> connectedMSHR;
		
		public int noOfRequests;
		public int hits;
		public int misses;
		public int evictions;
		
		public static final long NOT_EVICTED = -1;
		
		public static boolean debugMode =false;
	
		
		
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
			if (this.coherence == CoherenceType.Snoopy)
				busController = new BusController(prevLevel, this, numberOfBuses, this, cacheParameters.getBusOccupancy());
			
			
			this.timestamp = 0;
			this.numLinesMask = numLines - 1;
			this.noOfRequests = 0;
			this.hits = 0;
			this.misses = 0;
			this.evictions = 0;
			// make the cache
			makeCache();
			
			missStatusHoldingRegister = new MissStatusHoldingRegister(blockSizeBits, cacheParameters.mshrSize);			
			connectedMSHR = new ArrayList<MissStatusHoldingRegister>();
		}
		
		
		
		public void handleEvent(EventQueue eventQ, Event event)
		{
			AddressCarryingEvent addressCarryingEvent = (AddressCarryingEvent) event;
			if (event.getRequestType() == RequestType.Cache_Read
					|| event.getRequestType() == RequestType.Cache_Write 
					|| event.getRequestType() == RequestType.Cache_Write_Requiring_Response ){
//				if(this.isLastLevel && event.getRequestType()==RequestType.Cache_Read_from_iCache)
					//this.prevLevel.get(0).containingMemSys.getCore().getExecutionEngineIn().l2accesses++;
				this.handleAccess(eventQ, addressCarryingEvent);
			}
			else if (event.getRequestType() == RequestType.Cache_Read_Writeback
					|| event.getRequestType() == RequestType.Cache_Read_Writeback_Invalidate){
				this.handleAccessWithDirectoryUpdates(eventQ, addressCarryingEvent);
			}
			else if (event.getRequestType() == RequestType.Mem_Response)
				this.handleMemResponse(eventQ, event);
			else if (event.getRequestType() == RequestType.MESI_Invalidate)
				this.handleInvalidate(addressCarryingEvent);
		}
		
		protected void handleAccess(EventQueue eventQ, AddressCarryingEvent event)
		{
			RequestType requestType = event.getRequestType();
			long address = event.getAddress();
			if(debugMode) System.out.println(levelFromTop + " handling : " + requestType + " : " + address + " : " + event.coreId);
			CacheLine cl = this.processRequest(requestType, address);
			if(this.isLastLevel){
				Counters.incrementDcache2Access(1);
			}
			else{
				this.containingMemSys.getCore().powerCounters.incrementDcacheAccess(1);
			}
			//IF HIT
			if (cl != null || missStatusHoldingRegister.containsWriteOfEvictedLine(address) )
			{
				if(debugMode) System.out.println("remove : " + this.levelFromTop + " : " + address + " : " + (address >>> blockSizeBits) + "  :  " + event.coreId);
				processBlockAvailable(address);				
			}
			
			//IF MISS
			else
			{	
				if(this.isLastLevel)
				{
					AddressCarryingEvent eventToForward = new AddressCarryingEvent(event.getEventQ(),
																				    MemorySystem.mainMemory.getLatency(), 
																				    this, 
																				    MemorySystem.mainMemory, 
																				    RequestType.Main_Mem_Read, 
																				    address,
																				    event.coreId);
					MemorySystem.mainMemory.getPort().put(eventToForward);
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
			if (this.isLastLevel)
				putEventToPort(event, MemorySystem.mainMemory, RequestType.Main_Mem_Write, true, true);
			else
				putEventToPort(event,this.nextLevel, RequestType.Cache_Write, true, true);

			event.update(event.getEventQ(),	event.getEventTime(),event.getRequestingElement(),event.getProcessingElement(),	RequestType.Cache_Read);
			handleAccess(eventQ,event);

			//invalidate self
			handleInvalidate(event);			
		}
		
		protected void handleMemResponse(EventQueue eventQ, Event event)
		{
			/*Response for a read/write miss. Thus incrementing counters here as well*/
			if(this.isLastLevel){
				Counters.incrementDcache2Access(1);
			}
			else{
				this.containingMemSys.getCore().powerCounters.incrementDcacheAccess(1);
			}
			if(debugMode)
			{
				if(event.getRequestingElement() == null)
				{
					System.out.println("response came from main memory : " + ((AddressCarryingEvent)event).getAddress());
				}
			}
			this.fillAndSatisfyRequests(eventQ, event, MESI.EXCLUSIVE);
		}
		
		private void handleInvalidate(Event event)
		{
			CacheLine cl = this.access(((AddressCarryingEvent)event).getAddress());
			if (cl != null)
				cl.setState(MESI.INVALID);
		}
		
		
		

		
		private void sendResponseToWaitingEvent(ArrayList<Event> outstandingRequestList)
		{
			int numberOfWrites = 0;
			int numofWrite_Req_Res = 0;
			AddressCarryingEvent sampleWriteEvent = null;
			AddressCarryingEvent sampleWriteEvent1 = null;
			while (!outstandingRequestList.isEmpty())
			{	
				AddressCarryingEvent eventPoppedOut = (AddressCarryingEvent) outstandingRequestList.remove(0); 
				if (eventPoppedOut.getRequestType() == RequestType.Cache_Read 
						|| eventPoppedOut.getRequestType() == RequestType.Cache_Write_Requiring_Response)
				{
					eventPoppedOut.getRequestingElement().getPort().put(
							eventPoppedOut.update(
									eventPoppedOut.getEventQ(),
									0,
									eventPoppedOut.getProcessingElement(),
									eventPoppedOut.getRequestingElement(),
									RequestType.Mem_Response));
				} /*else if (eventPoppedOut.getRequestType() == RequestType.Cache_Write_Requiring_Response)
				{
					numofWrite_Req_Res++;
					sampleWriteEvent = eventPoppedOut;
				}*/
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
										//putEventToPort(eventPoppedOut,this.nextLevel, RequestType.Cache_Write, true,true);
									numberOfWrites++;
									sampleWriteEvent = eventPoppedOut;
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
									sampleWriteEvent = eventPoppedOut;
								}
									
						}
				}
				else
				{
					System.err.println("error request other than cache_read or cache_write came " + eventPoppedOut.getRequestType());
				}
			}
			
			/*if(numofWrite_Req_Res > 0 )
			{
				sampleWriteEvent1 = (AddressCarryingEvent) sampleWriteEvent.clone();
				sampleWriteEvent1.getRequestingElement().getPort().put(
						sampleWriteEvent1.update(
								sampleWriteEvent1.getEventQ(),
								0,
								sampleWriteEvent1.getProcessingElement(),
								sampleWriteEvent1.getRequestingElement(),
								RequestType.Mem_Response));
				
			}*/
			if(numberOfWrites > 0)
			{
				//for all writes to the same block at this level,
				//one write is sent to the next level
				propogateWrite(sampleWriteEvent);
			}
		}
		
		public void pullFromUpperMshrs()
		{
			if(missStatusHoldingRegister.isFull())
			{
				return;
			}
			
			Random rand = new Random();
			int startIdx = rand.nextInt();
			if(startIdx < 0 )
			{
				startIdx = -startIdx;
			}
			startIdx = startIdx%connectedMSHR.size();
			for(int i=0, j= startIdx  ; i < connectedMSHR.size();i++,j=(j+1)%connectedMSHR.size())
			{
				MissStatusHoldingRegister tempMshr = connectedMSHR.get(j);
				
				if(tempMshr.getNumberOfEntriesReadyToProceed() == 0)
				{
					return;
				}
				
				if(debugMode) System.out.println(this.levelFromTop + " pulling from above " + j);
				ArrayList<OMREntry> eventToProceed = tempMshr.getElementsReadyToProceed();
				for(int k = 0;k< eventToProceed.size();k++)
				{
					if(missStatusHoldingRegister.isFull())
					{
						break;
					}
					OMREntry omrEntry = eventToProceed.get(k);
//					SimulationElement requestingElement = omrEntry.eventToForward.getRequestingElement();
					omrEntry.readyToProceed = false;
					//if (omrEntry.eventToForward.get)
					AddressCarryingEvent clone = (AddressCarryingEvent) omrEntry.eventToForward.clone();
					boolean entryCreated = missStatusHoldingRegister.addOutstandingRequest(clone);//####
					//tempMshr.decrementNumberOfEntriesReadyToProceed();
					
					if(debugMode) System.out.println("pulled " + omrEntry.eventToForward.getRequestType() + "," + omrEntry.eventToForward.getAddress());
					
					if(omrEntry.eventToForward.getRequestType() == RequestType.Cache_Write)
					{
						tempMshr.removeStartingWrites(omrEntry.eventToForward.getAddress());
						if(debugMode)
						{
							if(omrEntry.eventToForward.getRequestingElement().getClass() == Cache.class)
							{
								for(int i1 = 0; i1 < omrEntry.outStandingEvents.size(); i1++)
								{
									if(omrEntry.outStandingEvents.get(i1).getRequestType() == RequestType.Cache_Read)
									{
										System.err.println("read found in omrentry of type write");
										System.exit(1);
									}
								}
							}
						}
					}
					tempMshr.decrementNumberOfEntriesReadyToProceed();
					
					if(entryCreated)
					{
						if(debugMode) System.out.println("add from pull: " + this.levelFromTop + " : " + omrEntry.eventToForward.getAddress() + " : " + (omrEntry.eventToForward.getAddress() >>> blockSizeBits) + "  :  " +  omrEntry.eventToForward.coreId );
						handleAccess(omrEntry.eventToForward.getEventQ() , clone);
					}
					else
					{
						AddressCarryingEvent eventToForward = missStatusHoldingRegister.getMshrEntry(clone.getAddress()).eventToForward; 
						if(eventToForward != null &&
								eventToForward.getRequestType() == RequestType.Cache_Write)
						{
							handleAccess(clone.getEventQ(), clone);
						}
					}
				}
				if(missStatusHoldingRegister.isFull())
				{
					break;
				}
			}
			if(debugMode) System.out.println(levelFromTop + " pulling complete");
		}
		
		/*
		 * forward memory request to next level
		 * handle related lower level mshr scenarios
		 */
		private void sendReadRequest(AddressCarryingEvent receivedEvent )
		{
			AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(receivedEvent.getEventQ(), 
																		  this.nextLevel.getLatency(), 
																		  this, 
																		  this.nextLevel, 
																		  RequestType.Cache_Read, 
																		  receivedEvent.getAddress(),
																		  receivedEvent.coreId);
			boolean isAddedinLowerMshr = this.nextLevel.addEvent(eventToBeSent);
			if(!isAddedinLowerMshr)
			{
				missStatusHoldingRegister.handleLowerMshrFull((AddressCarryingEvent) eventToBeSent.clone());
			}
			else
			{
				if(debugMode) System.out.println("issued : " + levelFromTop + " : " + eventToBeSent.getAddress() + " : " + GlobalClock.getCurrentTime() +
						this.levelFromTop + "," + this.nextLevel.levelFromTop );
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
			
			AddressCarryingEvent clone = (AddressCarryingEvent) addressEvent.clone();
			boolean entryCreated = missStatusHoldingRegister.addOutstandingRequest(clone);
			if(entryCreated)
			{
				if(debugMode) System.out.println("add from add event: " + this.levelFromTop + " : " + addressEvent.getAddress() + " : " + (addressEvent.getAddress() >>> blockSizeBits ) + "  :  " +  addressEvent.coreId );
				this.getPort().put(clone);
			}
			else
			{
				AddressCarryingEvent eventToForward = missStatusHoldingRegister.getMshrEntry(clone.getAddress()).eventToForward; 
				if(eventToForward != null &&
						eventToForward.getRequestType() == RequestType.Cache_Write)
				{
					handleAccess(clone.getEventQ(), clone);
				}
			}
			return true;
		}
		
		protected void fillAndSatisfyRequests(EventQueue eventQ, Event event, MESI stateToSet)
		{		
			long addr = ((AddressCarryingEvent)(event)).getAddress();
			
			if(debugMode) System.out.println("remove : " + this.levelFromTop + " : " + addr + " : " + (addr >>> blockSizeBits) );
			
			
			ArrayList<Event> eventsToBeServed = missStatusHoldingRegister.removeRequests(addr);
			
			CacheLine evictedLine = this.fill(addr, stateToSet);
			if (evictedLine != null 
			    && evictedLine.getState() == MESI.MODIFIED 
			    && this.writePolicy != CacheConfig.WritePolicy.WRITE_THROUGH) //This does not ensure inclusiveness
			{
				//Update directory in case of eviction
					if(this.coherence==CoherenceType.Directory)
					{
							int requestingCore = containingMemSys.getCore().getCore_number();
							long address=evictedLine.getTag() << this.blockSizeBits;	//Generating an address of this cache line
							evictionUpdateDirectory(requestingCore,evictedLine.getTag(),event,address);
					}
					if (this.isLastLevel)
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
			boolean pullFromTop = true;
			if (missStatusHoldingRegister.isFull()) 
			{
				pullFromTop = true;
			}
			
			sendResponseToWaitingEvent(eventsToBeServed);
			
			if(pullFromTop)
			{
				//pullFromUpperMshrs();
			}
			
			if(this.isLastLevel)
			{
				if(debugMode) System.out.println("L2");
			}
			else
			{
				if(debugMode) System.out.println("L1 | icache" );
			}
		}
		
		private void propogateWrite(AddressCarryingEvent event)
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
				if(missStatusHoldingRegister.isFull())
				{
					Newmain.dumpAllMSHRs();	
					Newmain.dumpAllEventQueues();
					System.err.println( levelFromTop + " entry not created for write so cannot propogate!!!! " +  event.getAddress() + "  :  " + (event.getAddress() >>> blockSizeBits) + " : " + event.coreId);
					//System.exit(1);
				}
				else
				{
					boolean entryCreated =  missStatusHoldingRegister.addOutstandingRequest( eventToForward );
					if (entryCreated)
					{
						missStatusHoldingRegister.handleLowerMshrFull( (AddressCarryingEvent) eventToForward.clone() );
					}
				}
			/*	else
				{
					System.err.println("write already present but mshr not full");
				}*/
			}
		}
		
		private void processBlockAvailable(long address)
		{
			boolean pullFromTop = true;
			if (missStatusHoldingRegister.isFull()) 
			{
				pullFromTop = true;
			}
			
			ArrayList<Event> eventsToBeServed = missStatusHoldingRegister.removeRequests(address); 
			sendResponseToWaitingEvent(eventsToBeServed);
			
			if(pullFromTop)
			{
				//pullFromUpperMshrs();
			}
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
		/**
		 * UPDATE DIRECTORY FOR EVICTION
		 * Update directory for evictedLine
		 * If modified, writeback, else just update sharers
		 * */
		private void evictionUpdateDirectory(int requestingCore, long dirAddress,Event event, long address) {
			CentralizedDirectoryCache centralizedDirectory = MemorySystem.getDirectoryCache();
			centralizedDirectory.getPort().put(
							new AddressCarryingEvent(
									event.getEventQ(),
									centralizedDirectory.getLatencyDelay(),
									this,
									centralizedDirectory,
									RequestType.EvictionDirectoryUpdate,
									address,
									(event).coreId));
		}
		
		private long computeTag(long addr) {
			long tag = addr >>> (numSetsBits + blockSizeBits);
			return tag;
		}
		private int getStartIdx(long addr) {
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
			if(debugMode) System.out.println("address\t"  + addr+"  starting line\t" + startIdx +  " number of cache lines\t" + numLines  + " size\t"  + size + " tag\t" + tag)  ;

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
				evictedLine = fillLine.copy();
				this.evictions++;
			}

			/* This is the new fill line */
			fillLine.setState(stateToSet);
			mark(fillLine, tag);
			return evictedLine;
		}
	
		public CacheLine processRequest(RequestType requestType, long addr)
		{
			noOfRequests++;
			CacheLine ll = null;
			if(requestType == RequestType.Cache_Read )  {
				ll = this.read(addr);
			} else if (requestType == RequestType.Cache_Write) {
				ll = this.write(addr);
			}
			if(ll == null)
			{
				this.misses++;
			} 
			else 
			{
				this.hits++;				
			}
			return ll;
		}
		
		public void populateConnectedMSHR()
		{
			for(int i = 0; i < prevLevel.size(); i++)
			{
				connectedMSHR.add(i, prevLevel.get(i).getMissStatusHoldingRegister());
			}
		}
		
		
		//getters and setters
		public MissStatusHoldingRegister getMissStatusHoldingRegister() {
			return missStatusHoldingRegister;
		}
}