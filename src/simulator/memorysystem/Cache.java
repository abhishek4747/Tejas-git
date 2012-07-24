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
import pipeline.inorder.FetchUnitIn;
import pipeline.inorder.MemUnitIn;

import net.NOC.TOPOLOGY;

import memorysystem.directory.CentralizedDirectoryCache;
import memorysystem.directory.DirectoryEntry;
import memorysystem.directory.DirectoryState;
import memorysystem.snoopyCoherence.BusController;

import config.CacheConfig;
import config.SystemConfig;
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
		
		
//		protected boolean enforcesCoherence = false; //The cache which is shared between the coherent cache level
//		protected boolean isCoherent = false; //Tells whether the level is coherent or not
		
		public CoherenceType coherence = CoherenceType.None;
		public int numberOfBuses = 1;
		public BusController busController = null;
		
//		protected boolean isFirstLevel = false;
		public CacheType levelFromTop; 
		public boolean isLastLevel; //Tells whether there are any more levels of cache
		protected CacheConfig.WritePolicy writePolicy; //WRITE_BACK or WRITE_THROUGH
		
		public String nextLevelName; //Name of the next level cache according to the configuration file
		public ArrayList<Cache> prevLevel = new ArrayList<Cache>(); //Points towards the previous level in the cache hierarchy
		public Cache nextLevel; //Points towards the next level in the cache hierarchy
        protected final int MSHRSize;
		protected CacheLine lines[];
		
//		protected Hashtable<Long, ArrayList<CacheMissStatusHoldingRegisterEntry>> missStatusHoldingRegister
//						= new Hashtable<Long, ArrayList<CacheMissStatusHoldingRegisterEntry>>();
		public Hashtable<Long, OMREntry> missStatusHoldingRegister
								= new Hashtable<Long, OMREntry>();
		public ArrayList<Hashtable<Long,OMREntry>> connectedMSHR = 
						new ArrayList<Hashtable<Long,OMREntry>>();
		
		public int noOfRequests;
		public int hits;
		public int misses;
		public int evictions;
		
		public static final long NOT_EVICTED = -1;
	
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
			//System.out.println("address\t"  + addr+"  starting line\t" + startIdx +  " number of cache lines\t" + numLines  + " size\t"  + size + " tag\t" + tag)  ;

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
			this.MSHRSize = cacheParameters.mshrSize;
			// make the cache
			makeCache();
			
		/*	System.out.println("cache size " + size  + "block size " + blockSize  + " number of lines  " + numLines + "cache name " + nextLevelName) ;
			System.exit(1);
		*/}

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
			if(requestType == RequestType.Cache_Read  || requestType == RequestType.Cache_Read_from_iCache )  {
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
		
		/**
		 * Used when a new request is made to a cache and there is a miss.
		 * This adds the request to the outstanding requests buffer of the cache
		 * @param blockAddr : Memory Address requested
		 * @param requestType : MEM_READ or MEM_WRITE
		 * @param requestingElement : Which element made the request. Helpful in backtracking and filling the stack
		 */
		public int addOutstandingRequest(Event event, long addr)
		{
			int entryAlreadyThere = 0;
			Enumeration<OMREntry> tempEntry = missStatusHoldingRegister.elements();
			while(tempEntry.hasMoreElements())
			{
				OMREntry omrEntry = tempEntry.nextElement();
				for(int i=0; i<omrEntry.outStandingEvents.size();i++)
				{
					if(omrEntry.outStandingEvents.get(i).getRequestType() == RequestType.Mem_Response)
					{
						System.err.println("mem response into mshr ");
						System.exit(1);
					}
				}
			}
			long blockAddr = addr >>> blockSizeBits;
			
			if (!/*NOT*/missStatusHoldingRegister.containsKey(blockAddr))
			{
				if(missStatusHoldingRegister.size() > MSHRSize) {
					System.out.println("mshr full");
					return 2;
				}
				entryAlreadyThere = 0;
				missStatusHoldingRegister.put(blockAddr, new OMREntry(new ArrayList<Event>(), false, null));
				missStatusHoldingRegister.get(blockAddr).outStandingEvents.add(event);
			}
			else{
				if(missStatusHoldingRegister.get(blockAddr).outStandingEvents.isEmpty())
					entryAlreadyThere = 0;
				else if(missStatusHoldingRegister.get(blockAddr).outStandingEvents.size() < MSHRSize)
					entryAlreadyThere = 1;
				else {
					System.out.println("mshr full");
					return 2;
				}
				missStatusHoldingRegister.get(blockAddr).outStandingEvents.add(event);
			}
			
			if(event.getRequestType()==RequestType.Mem_Response)
				System.err.println("Adding a mem response event to mshr!");
			return entryAlreadyThere;
		}
		
		public void handleEvent(EventQueue eventQ, Event event)
		{
			if (event.getRequestType() == RequestType.Cache_Read
					|| event.getRequestType() == RequestType.Cache_Write
					||event.getRequestType() == RequestType.Cache_Read_from_iCache){
//				if(this.isLastLevel && event.getRequestType()==RequestType.Cache_Read_from_iCache)
					//this.prevLevel.get(0).containingMemSys.getCore().getExecutionEngineIn().l2accesses++;
				this.handleAccess(eventQ, event);
			}
			else if (event.getRequestType() == RequestType.Cache_Read_Writeback
					|| event.getRequestType() == RequestType.Cache_Read_Writeback_Invalidate){
				this.handleAccessWithDirectoryUpdates(eventQ, event);
			}
			else if (event.getRequestType() == RequestType.Mem_Response)
				this.handleMemResponse(eventQ, event);
			else if (event.getRequestType() == RequestType.MESI_Invalidate)
				this.handleInvalidate(event);
		}

		private void handleAccessWithDirectoryUpdates(EventQueue eventQ,
				Event event) {
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


		
		protected void handleAccess(EventQueue eventQ, Event event)
		{
			SimulationElement requestingElement = event.getRequestingElement();
			RequestType requestType = event.getRequestType();
			long address = ((AddressCarryingEvent)(event)).getAddress();
			//Process the access
			CacheLine cl = this.processRequest(requestType, address);
			if(this.isLastLevel){
				Counters.incrementDcache2Access(1);
			}
			else{
				this.containingMemSys.getCore().powerCounters.incrementDcacheAccess(1);
			}
			//IF HIT
			if (cl != null)
			{
				
				if(requestingElement!=null){
					if(requestingElement.getClass() == MemUnitIn.class)
					{
						((MemUnitIn)requestingElement).getMissStatusHoldingRegister().remove(address);
					}
					else if(requestingElement.getClass() == LSQ.class){
						((LSQ)requestingElement).getMissStatusHoldingRegister().remove(address);
					}
				}

				//Schedule the requesting element to receive the block TODO (for LSQ)
				if (requestType == RequestType.Cache_Read  || requestType == RequestType.Cache_Read_from_iCache)
				{
					//Just return the read block
					putEventToPort(event, requestingElement, RequestType.Mem_Response, true,false);	
				}
				
				else if (requestType == RequestType.Cache_Write)
				{
						if(this.coherence==CoherenceType.Directory){
							writeHitUpdateDirectory(this.containingMemSys.getCore().getCore_number(), computeTag(address), event, address);
						}
						else{//FIXME This logic seems incorrect. In case of a cache write no need to write main memory if it is not write through!
								if (this.isLastLevel)
							{
								putEventToPort(event, MemorySystem.mainMemory, RequestType.Main_Mem_Write, true,true);		
							}
							else if((this.coherence == CoherenceType.None) 
									&& (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH))
							{
								putEventToPort(event, this.nextLevel, RequestType.Cache_Write, true,true);
							}
						}
				}
			}
			
			//IF MISS
			else
			{	
				int alreadyRequested = this.addOutstandingRequest(event, address);
				if (alreadyRequested == 0)
				{
					if(this.coherence==CoherenceType.Directory){
						if(requestType==RequestType.Cache_Read)
							readMissUpdateDirectory(this.containingMemSys.getCore().getCore_number(), computeTag(address), event, address);
						else if(requestType==RequestType.Cache_Write)
							writeMissUpdateDirectory(this.containingMemSys.getCore().getCore_number(), computeTag(address), event, address);
					}
					if(requestingElement != null){
						if(requestingElement.getClass() == MemUnitIn.class)
						{
							((MemUnitIn)requestingElement).getMissStatusHoldingRegister().remove(address);
						}
						else if(requestingElement.getClass() == LSQ.class)
						{
							((LSQ)requestingElement).getMissStatusHoldingRegister().remove(address);
						}
					}
										// access the next level
						if (this.isLastLevel)
						{
							AddressCarryingEvent addressEvent =   putEventToPort(event, MemorySystem.mainMemory, RequestType.Main_Mem_Read,false,true);
							missStatusHoldingRegister.get((address >> blockSizeBits)).eventToForward = addressEvent;
							return;
						}
						else
						{
							AddressCarryingEvent addressEvent = putEventToPort(event, this.nextLevel, RequestType.Cache_Read, false,true);
							missStatusHoldingRegister.get((address >> blockSizeBits)).eventToForward = addressEvent;
						}
					}
				
				else if(alreadyRequested == 1 && requestingElement!=null)
				{
						if(requestingElement.getClass() == MemUnitIn.class)
						{
							if(((MemUnitIn)requestingElement).getMissStatusHoldingRegister().containsKey(address))
								((MemUnitIn)requestingElement).getMissStatusHoldingRegister().remove(address);
							
					
						}
						else if(requestingElement.getClass() == LSQ.class){
							if(((LSQ)requestingElement).getMissStatusHoldingRegister().containsKey(address))
								((LSQ)requestingElement).getMissStatusHoldingRegister().remove(address);
						}
				}
				else if(alreadyRequested == 2 && requestingElement != null)
				{
					if(requestingElement.getClass() == Cache.class)
					{
						if(!this.connectedMSHR.contains(((Cache)requestingElement).missStatusHoldingRegister))
							this.connectedMSHR.add(((Cache)requestingElement).missStatusHoldingRegister);
						if(((Cache)requestingElement).missStatusHoldingRegister.containsKey(address >> ((Cache)requestingElement).blockSizeBits) &&
								event.getRequestType() == RequestType.Cache_Read )
						{
							((Cache)requestingElement).missStatusHoldingRegister.get(address >> ((Cache)requestingElement).blockSizeBits).readyToProceed = true;
							//((Cache)requestingElement).missStatusHoldingRegister.get(address >> ((Cache)requestingElement).blockSizeBits).eventToForward = event;
						}
						else if(((AddressCarryingEvent)event).getRequestType() != RequestType.Cache_Write)
						{
							System.out.println("Outstanding Request in Memory System from cache line 610 " + (address >> ((Cache)requestingElement).blockSizeBits) + ((Cache)requestingElement).missStatusHoldingRegister + event.getRequestType());
							System.exit(1);
						}
					}
					else if(requestingElement.getClass() == InstructionCache.class)
					{
						if(!this.connectedMSHR.contains(((InstructionCache)requestingElement).missStatusHoldingRegister))
							this.connectedMSHR.add(((InstructionCache)requestingElement).missStatusHoldingRegister);
						if(((InstructionCache)requestingElement).missStatusHoldingRegister.containsKey(address >> ((InstructionCache)requestingElement).blockSizeBits) &&
								event.getRequestType() == RequestType.Cache_Read_from_iCache )
						{
							((InstructionCache)requestingElement).missStatusHoldingRegister.get(address >> ((InstructionCache)requestingElement).blockSizeBits).readyToProceed = true;
//							((InstructionCache)requestingElement).missStatusHoldingRegister.get(address >> ((InstructionCache)requestingElement).blockSizeBits).eventToForward = event;
						}
						else if(((AddressCarryingEvent)event).getRequestType() != RequestType.Cache_Write)
						{
							System.out.println("Outstanding Request in Memory System from cache line 626");
							System.exit(1);
						}
					}
					else if(requestingElement.getClass()==MemUnitIn.class){
						if(!this.connectedMSHR.contains(((MemUnitIn)requestingElement).getMissStatusHoldingRegister()))
							this.connectedMSHR.add(((MemUnitIn)requestingElement).getMissStatusHoldingRegister());
					}
					else if(requestingElement.getClass()==LSQ.class){
						if(!this.connectedMSHR.contains(((LSQ)requestingElement).getMissStatusHoldingRegister()))
							this.connectedMSHR.add(((LSQ)requestingElement).getMissStatusHoldingRegister());
					}
				}
			}
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
			this.fillAndSatisfyRequests(eventQ, event, MESI.EXCLUSIVE);
		}
		
		protected void fillAndSatisfyRequests(EventQueue eventQ, Event event, MESI stateToSet)
		{		
			long addr = ((AddressCarryingEvent)(event)).getAddress();
			
			Enumeration<OMREntry> tempEntry = missStatusHoldingRegister.elements();
			CacheLine evictedLine = this.fill(addr, stateToSet);
			if (evictedLine != null && evictedLine.getState() == MESI.MODIFIED) //This does not ensure inclusiveness
			{

				//Update directory in case of eviction
					if(this.coherence==CoherenceType.Directory){
							int requestingCore = containingMemSys.getCore().getCore_number();
							long address=evictedLine.getTag() << this.blockSizeBits;	//Generating an address of this cache line
							evictionUpdateDirectory(requestingCore,evictedLine.getTag(),event,address);
					}
					if (this.isLastLevel) {
							putEventToPort(event, MemorySystem.mainMemory, RequestType.Main_Mem_Write, false,true);
					}else {
							putEventToPort(event,this.nextLevel, RequestType.Cache_Write, false,true);
					}
			}
			long blockAddr = addr >>> this.blockSizeBits;
			if (!this.missStatusHoldingRegister.containsKey(blockAddr))
			{
				System.err.println("Memory System Error : An outstanding request not found in the requesting element" +
															event.getRequestType() + event.getProcessingElement().getClass() + "  " + ((Cache)event.getProcessingElement()).levelFromTop);
				System.exit(1);
			}
			//Event eventPoppedOut = null;
			ArrayList<Event> outstandingRequestList = this.missStatusHoldingRegister.remove(blockAddr).outStandingEvents;
			while (!outstandingRequestList.isEmpty())
			{	
					Event eventPoppedOut = outstandingRequestList.remove(0); 
					
					if (eventPoppedOut.getRequestType() == RequestType.Cache_Read  
							|| eventPoppedOut.getRequestType() == RequestType.Cache_Read_from_iCache)
					{
						eventPoppedOut.getRequestingElement().getPort().put(
								eventPoppedOut.update(
										eventPoppedOut.getEventQ(),
										1,
										eventPoppedOut.getProcessingElement(),
										eventPoppedOut.getRequestingElement(),
										RequestType.Mem_Response));
						eventPoppedOut = null;
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
											putEventToPort(event,this.nextLevel, RequestType.Cache_Write, true,true);	
									}
							}
							else
							{
									CacheLine cl = this.access(addr);
									if (cl != null)
											cl.setState(MESI.MODIFIED);
							}
					}
					else
					{
						eventPoppedOut.getProcessingElement().getPort().put(
								eventPoppedOut.update(
										eventPoppedOut.getEventQ(),
										1,
										eventPoppedOut.getRequestingElement(),
										eventPoppedOut.getProcessingElement(),
										RequestType.Mem_Response));	
						/*System.out.println("Cache Error : A request was of type other than Cache_Read" +
																	 " or Cache_Write. The encountered request type was : " + eventPoppedOut.getRequestType());
							System.out.println("Event details \n"+"req element"+eventPoppedOut.getRequestingElement()
												+"processing element"+eventPoppedOut.getProcessingElement());
							System.exit(1);*/
					}
			}

			Vector<Integer> indexToRemove = new Vector<Integer>();
			for(int i=0; i < connectedMSHR.size();i++)
			{
				
				Hashtable<Long,OMREntry> tempMissStatusHoldingRegister = connectedMSHR.get(i);
				int readyToProceedCount =0;
				int instructionProceeded =0;
				Enumeration<OMREntry> omrIte = tempMissStatusHoldingRegister.elements();
				Enumeration<Long> omrKeys = tempMissStatusHoldingRegister.keys();
				while(omrIte.hasMoreElements())
				{
					OMREntry omrEntry = omrIte.nextElement();
					Long key = omrKeys.nextElement();
					if(omrEntry.readyToProceed)
					{
						readyToProceedCount++;
						if(omrEntry.eventToForward == null) {
							System.err.println("event to forward null ");
							System.exit(1);
						} else if(omrEntry.eventToForward.getRequestingElement() == null) {
							System.err.println(" requesting element null ");
							System.exit(1);
							
						}
						SimulationElement requestingElement = omrEntry.eventToForward.getRequestingElement();
						if(requestingElement != null){
							if(requestingElement.getClass() != MemUnitIn.class && requestingElement.getClass() != LSQ.class)
							{
								omrEntry.readyToProceed = false;
							}
						}
							handleAccess(eventQ, omrEntry.eventToForward);
						if(!omrEntry.readyToProceed)
						{
							instructionProceeded++;
						}
					}
					if(missStatusHoldingRegister.size() >= MSHRSize)
					{
						break;
					}
				}
				if(readyToProceedCount == instructionProceeded && readyToProceedCount>0)
				{
					indexToRemove.add(i);
				}
				if(missStatusHoldingRegister.size() >= MSHRSize)
				{
					break;
				}
			}
			for(int i=0;i<indexToRemove.size();i++)
			{
				this.connectedMSHR.remove(indexToRemove.get(i));
			}
		}
		
		private void handleInvalidate(Event event)
		{
			CacheLine cl = this.access(((AddressCarryingEvent)event).getAddress());
			if (cl != null)
				cl.setState(MESI.INVALID);
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


}