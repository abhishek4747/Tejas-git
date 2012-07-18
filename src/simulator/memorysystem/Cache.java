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

import memorysystem.directory.CentralizedDirectory;
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
		protected CacheType levelFromTop; 
		protected boolean isLastLevel; //Tells whether there are any more levels of cache
		protected CacheConfig.WritePolicy writePolicy; //WRITE_BACK or WRITE_THROUGH
		
		protected String nextLevelName; //Name of the next level cache according to the configuration file
		protected ArrayList<Cache> prevLevel = new ArrayList<Cache>(); //Points towards the previous level in the cache hierarchy
		protected Cache nextLevel; //Points towards the next level in the cache hierarchy
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
			long SetMask = 1 << (numSetsBits) - 1;
			int startIdx = (int) ((addr >>> blockSizeBits) & (SetMask));
			return startIdx;
		}
		private int getNextIdx(int startIdx,int idx) {
			int index = startIdx + idx << numSetsBits;
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
			
			long blockAddr = addr >>> blockSizeBits;
			
			if (!/*NOT*/missStatusHoldingRegister.containsKey(blockAddr))
			{
				entryAlreadyThere = 0;
				missStatusHoldingRegister.put(blockAddr, new OMREntry(new ArrayList<Event>(), false, null));
				missStatusHoldingRegister.get(blockAddr).outStandingEvents.add(event);
			}
			else{
				if(missStatusHoldingRegister.get(blockAddr).outStandingEvents.isEmpty())
					entryAlreadyThere = 0;
				else
					entryAlreadyThere = 1;
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
			}else if (event.getRequestType() == RequestType.Mem_Response)
				this.handleMemResponse(eventQ, event);
			else if (event.getRequestType() == RequestType.MESI_Invalidate)
				this.handleInvalidate(event);
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
				
/*				if(requestingElement!=null){
					if(requestingElement.getClass() == MemUnitIn.class)
					{
						((MemUnitIn)requestingElement).getMissStatusHoldingRegister().remove(address);
					}
					else if(requestingElement.getClass() == LSQ.class){
						((LSQ)requestingElement).getMissStatusHoldingRegister().remove(address);
					}
				}
				*/
				//Schedule the requesting element to receive the block TODO (for LSQ)
				if (requestType == RequestType.Cache_Read  || requestType == RequestType.Cache_Read_from_iCache)
				{
					//Just return the read block
					putEventToPort(event, requestingElement, RequestType.Mem_Response, true,false);	
				}
				
				else if (requestType == RequestType.Cache_Write)
				{
						if (this.isLastLevel)
						{
							putEventToPort(event, MemorySystem.mainMemory, RequestType.Main_Mem_Write, true,true);		
						}
						else if((this.nextLevel.coherence == CoherenceType.None) 
								&& (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH))
						{
							putEventToPort(event, this.nextLevel, RequestType.Cache_Write, true,true);
						}
				}
			}
			
			//IF MISS
			else
			{	
				int alreadyRequested = this.addOutstandingRequest(event, address);
				if (alreadyRequested == 0)
				{
					/*if(requestingElement != null){
						if(requestingElement.getClass() == MemUnitIn.class)
						{
							((MemUnitIn)requestingElement).getMissStatusHoldingRegister().remove(address);
						}
						else if(requestingElement.getClass() == LSQ.class)
						{
							((LSQ)requestingElement).getMissStatusHoldingRegister().remove(address);
						}
					}*/
										// access the next level
						if (this.isLastLevel)
						{
							putEventToPort(event, MemorySystem.mainMemory, RequestType.Main_Mem_Read,false,true);
//							missStatusHoldingRegister.get((address >> blockSizeBits)).eventToForward = addressEvent;
							return;
						}
						else
						{
							putEventToPort(event, this.nextLevel, RequestType.Cache_Read, false,true);
//							missStatusHoldingRegister.get((address >> blockSizeBits)).eventToForward = addressEvent;
						}
					}
				
				else if(alreadyRequested == 1 && requestingElement!=null)
				{
						/*if(requestingElement.getClass() == MemUnitIn.class)
						{
							if(((MemUnitIn)requestingElement).getMissStatusHoldingRegister().containsKey(address))
								((MemUnitIn)requestingElement).getMissStatusHoldingRegister().remove(address);
							
					
						}
						else if(requestingElement.getClass() == LSQ.class){
							if(((LSQ)requestingElement).getMissStatusHoldingRegister().containsKey(address))
								((LSQ)requestingElement).getMissStatusHoldingRegister().remove(address);
						}*/
				}
				else if(alreadyRequested == 2 && requestingElement != null)
				{
/*					if(requestingElement.getClass() == Cache.class)
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
	*/			}
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
			
			CacheLine evictedLine = this.fill(addr, stateToSet);
			if (evictedLine != null && evictedLine.getState() == MESI.MODIFIED) //This does not ensure inclusiveness
			{
				if (this.isLastLevel) {
					putEventToPort(event, MemorySystem.mainMemory, RequestType.Main_Mem_Write, false,true);
				}else {
						putEventToPort(event,this.nextLevel, RequestType.Cache_Write, false,true);
				}
			}
			long blockAddr = addr >>> this.blockSizeBits;
			if (!this.missStatusHoldingRegister.containsKey(blockAddr))
			{
				System.err.println("Memory System Error : An outstanding request not found in the requesting element" + event.getRequestType() + event.getProcessingElement().getClass() + "  " + ((Cache)event.getProcessingElement()).levelFromTop);
				System.exit(1);
			}
			Event eventPoppedOut;
			ArrayList<Event> outstandingRequestList = this.missStatusHoldingRegister.remove(blockAddr).outStandingEvents;
			while (!/*NOT*/outstandingRequestList.isEmpty())
			{	
				eventPoppedOut = outstandingRequestList.remove(0);		
				if (eventPoppedOut.getRequestType() == RequestType.Cache_Read  || eventPoppedOut.getRequestType() == RequestType.Cache_Read_from_iCache)
				{
						putEventToPort(eventPoppedOut,eventPoppedOut.getRequestingElement(), RequestType.Mem_Response, true,false);	
				}
				
				else if (eventPoppedOut.getRequestType() == RequestType.Cache_Write)
				{
					if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
					{
						long address;
						address = ((AddressCarryingEvent)(event)).getAddress();
						
						if (this.isLastLevel)
						{
							putEventToPort(eventPoppedOut,eventPoppedOut.getRequestingElement(), RequestType.Main_Mem_Write, true,true);	
						}
						else if (this.nextLevel.coherence != CoherenceType.Snoopy)
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
					System.err.println("Cache Error : A request was of type other than Cache_Read or Cache_Write. The encountered request type was : " + eventPoppedOut.getRequestType());
					System.err.println("Event details \n"+"req element"+eventPoppedOut.getRequestingElement()
										+"processing element"+eventPoppedOut.getProcessingElement());
//					System.exit(1);
				}
				
				//Remove the processed entry from the outstanding request list
//				outstandingRequestList.remove(0);
			}
			/*Vector<Integer> indexToRemove = new Vector<Integer>();
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
			*/
			
			/*}
			else
			{
				System.err.println("Memory System Error : An outstanding request not found in the requesting element" + event.getRequestType() + event.getProcessingElement().getClass() + "  " + ((Cache)event.getProcessingElement()).levelFromTop);
				System.exit(1);
			}*/
		}
		
		private void handleInvalidate(Event event)
		{
			CacheLine cl = this.access(((AddressCarryingEvent)event).getAddress());
			if (cl != null)
				cl.setState(MESI.INVALID);
		}
		
		private void  putEventToPort(Event event, SimulationElement simElement, RequestType requestType, boolean flag, boolean time  )
		{
			long eventTime = 0;
			if(time) {
				eventTime = simElement.getLatency();
			}
			else {
				eventTime = 0;
			}
			if(flag){
				simElement.getPort().put(
						event.update(
								event.getEventQ(),
								eventTime,
								this,
								simElement,
								requestType));
			} else {
				simElement.getPort().put(
						new AddressCarryingEvent(
								event.getEventQ(),
								eventTime,
								this,
								simElement,
								requestType,
								((AddressCarryingEvent)event).coreId));
			}
		}
}
