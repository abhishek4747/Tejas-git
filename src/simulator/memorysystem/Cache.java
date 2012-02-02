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
		protected int blockSizeBits; // in bits
		protected int assoc;
		protected int assocBits; // in bits
		protected int size; // MegaBytes
		protected int numLines;
		protected int numLinesBits;
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

		protected CacheLine lines[];
		
//		protected Hashtable<Long, ArrayList<CacheMissStatusHoldingRegisterEntry>> missStatusHoldingRegister
//						= new Hashtable<Long, ArrayList<CacheMissStatusHoldingRegisterEntry>>();
		protected Hashtable<Long, ArrayList<Event>> missStatusHoldingRegister
						= new Hashtable<Long, ArrayList<Event>>();
		
		public int noOfRequests;
		public int hits;
		public int misses;
		public int evictions;
		
		public static final long NOT_EVICTED = -1;
		
		public CacheLine access(long addr)
		{
			/* remove the block size */
			long tag = addr >>> this.blockSizeBits;

			/* search all the lines that might match */
			
			long laddr = tag >>> this.assocBits;
			laddr = laddr << assocBits; //Replace the associativity bits with zeros.

			/* remove the tag portion */
			laddr = laddr & numLinesMask;

			/* search in a set */
			for(int idx = 0; idx < assoc; idx++) 
			{
				CacheLine ll = this.lines[(int)(laddr + (long)(idx))];
				if(ll.hasTagMatch(tag) && (ll.getState() != MESI.INVALID))
					return  ll;
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
			this.size = cacheParameters.getSize();
			this.blockSizeBits = Util.logbase2(blockSize);
			this.assocBits = Util.logbase2(assoc);
			this.numLines = getNumLines();
			
			this.writePolicy = cacheParameters.getWritePolicy();
			this.levelFromTop = cacheParameters.getLevelFromTop();
			this.isLastLevel = cacheParameters.isLastLevel();
			this.nextLevelName = cacheParameters.getNextLevel();
//			this.enforcesCoherence = cacheParameters.isEnforcesCoherence();
			this.coherence = cacheParameters.getCoherence();
			this.numberOfBuses = cacheParameters.getNumberOfBuses();
			if (this.coherence == CoherenceType.Snoopy)
				busController = new BusController(prevLevel, this, numberOfBuses, this);
			
			this.numLinesBits = Util.logbase2(numLines);
			this.timestamp = 0;
			this.numLinesMask = numLines - 1;
			this.noOfRequests = 0;
			this.hits = 0;
			this.misses = 0;
			this.evictions = 0;
			
			// make the cache
			makeCache();
		}

		private CacheLine read(long addr)
		{
			CacheLine cl = access(addr);
			if(cl != null)
				mark(cl);
			return cl;
		}
		
		private CacheLine write(long addr)
		{
			CacheLine cl = access(addr);
			if(cl != null) 
				mark(cl);
			return cl;
		}
		
		protected CacheLine fill(long addr, MESI stateToSet) //Returns a copy of the evicted line
		{
			CacheLine evictedLine = null;
			
			/* remove the block size */
			long tag = addr >>> this.blockSizeBits;

			/* search all the lines that might match */
			long laddr = tag >>> this.assocBits;
			laddr = laddr << assocBits; // replace the associativity bits with zeros.

			/* remove the tag portion */
			laddr = laddr & numLinesMask;

			/* find any invalid lines -- no eviction */
			CacheLine fillLine = null;
			boolean evicted = false;
			for (int idx = 0; idx < assoc; idx++) 
			{
				CacheLine ll = this.lines[(int)(laddr + (long)(idx))];
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
					CacheLine ll = this.lines[(int)(laddr + (long)(idx))];
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
				
				//if (fillLine.getPid() != request.getThreadID()) //TODO I didn't understand the logic
				//{
					/* increase eviction count */
					this.evictions++;

					/* log the line */
					evictedLines.addElement(fillLine.getTag());
				//}
			}

			/* This is the new fill line */
			fillLine.setState(stateToSet);
			//fillLine.setValid(true);
			mark(fillLine, tag);
			return evictedLine;
		}
	
		public CacheLine processRequest(RequestType requestType, long addr)
		{
			noOfRequests++;
			//boolean isHit;
			/* access the Cache */
			CacheLine ll = null;
			if(requestType == RequestType.Cache_Read)
				ll = this.read(addr);
			else if (requestType == RequestType.Cache_Write)
				ll = this.write(addr);
			
			if(ll == null)
			{
				/* Miss */
//				if (!(request.isWriteThrough()))//TODO For testing purposes only
				this.misses++;
			} 
			else 
			{
				/* Hit */
				/* do nothing */
//				if (!(request.isWriteThrough()))//TODO For testing purposes only
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
		public boolean addOutstandingRequest(Event event, long addr)
		{
			boolean entryAlreadyThere;
			
			long blockAddr = addr >>> blockSizeBits;
			
			if (!/*NOT*/missStatusHoldingRegister.containsKey(blockAddr))
			{
				entryAlreadyThere = false;
//				missStatusHoldingRegister.put(blockAddr, new ArrayList<CacheMissStatusHoldingRegisterEntry>());
				missStatusHoldingRegister.put(blockAddr, new ArrayList<Event>());
			}
			else if (missStatusHoldingRegister.get(blockAddr).isEmpty())
				entryAlreadyThere = false;
			else
				entryAlreadyThere = true;
			
//			missStatusHoldingRegister.get(blockAddr).add(new CacheMissStatusHoldingRegisterEntry(requestType,
//																							requestingElement,
//																							addr,
//																							lsqEntry));
			missStatusHoldingRegister.get(blockAddr).add(event);
			
			return entryAlreadyThere;
		}
		
		public void handleEvent(EventQueue eventQ, Event event)
		{
			if (event.getRequestType() == RequestType.Cache_Read
					|| event.getRequestType() == RequestType.Cache_Write
					||event.getRequestType() == RequestType.Cache_Read_from_iCache)
				this.handleAccess(eventQ, event);
			else if (event.getRequestType() == RequestType.Mem_Response)
				this.handleMemResponse(eventQ, event);
			else if (event.getRequestType() == RequestType.Request_for_copy)
				this.handleRequestForCopy(eventQ, event);
			else if (event.getRequestType() == RequestType.Request_for_modified_copy)
				this.handleRequestForModifiedCopy(eventQ, event);
			else if (event.getRequestType() == RequestType.Reply_with_shared_copy)
				this.handleReplyWithSharedCopy(eventQ, event);
			else if (event.getRequestType() == RequestType.Write_Modified_to_sharedmem)
				this.handleWriteModifiedToSharedMem(eventQ, event);
			else if (event.getRequestType() == RequestType.MESI_Invalidate)
				this.handleInvalidate(event);
		}
		
		private void handleAccess(EventQueue eventQ, Event event)
		{
			SimulationElement requestingElement = event.getRequestingElement();
			RequestType requestType = event.getRequestType();
			long address;
			
			if (this.levelFromTop == CacheType.L1 && !MemorySystem.bypassLSQ)
				address = ((LSQEntryContainingEvent)(event)).getLsqEntry().getAddr();
			else
				address = ((AddressCarryingEvent)(event)).getAddress();
			
			//Process the access
			CacheLine cl = this.processRequest(requestType, address);

			//IF HIT
			if (cl != null)
			{
				//Schedule the requesting element to receive the block TODO (for LSQ)
				if (requestType == RequestType.Cache_Read)
				{
					//Just return the read block
					if (this.coherence != CoherenceType.Snoopy)
						requestingElement.getPort().put(
								event.update(
										eventQ,
										requestingElement.getLatencyDelay(),
										this,
										requestingElement,
										RequestType.Mem_Response));
					else
						this.busController.getBusAndPutEvent(
								event.update(
										eventQ,
										requestingElement.getLatencyDelay(),
										this,
										requestingElement,
										RequestType.Mem_Response));
				}
				
				else if (requestType == RequestType.Cache_Read_from_iCache)
				{
					requestingElement.getPort().put(
							event.update(
									eventQ,
									requestingElement.getLatencyDelay(),
									this,
									requestingElement,
									RequestType.Mem_Response));
				}
				
				else if (requestType == RequestType.Cache_Write)
				{
					//Write the data to the cache block (Do Nothing)
					if ((!this.isLastLevel) && this.nextLevel.coherence == CoherenceType.Snoopy)
						this.nextLevel.busController.processWriteHit(eventQ, this, cl, address);
					else if ((!this.isLastLevel) && this.nextLevel.coherence == CoherenceType.Directory)
					{
						/* remove the block size */
						long tag = address >>> this.blockSizeBits;

						/* search all the lines that might match */
						
						long laddr = tag >>> this.assocBits;
						laddr = laddr << assocBits; //Replace the associativity bits with zeros.

						/* remove the tag portion */
						laddr = laddr & numLinesMask;
						int cacheLineNum=(int)(laddr/(long)blockSize);//TODO is this correct ?
																// long to int typecast ? need an array indexed by long ?
						int requestingCore = containingMemSys.getCore().getCore_number();//TODO Is this correct ?
						
						writeHitUpdate(cacheLineNum,requestingCore, eventQ, address, event);
					}//TODO
					else if ((!this.isLastLevel) && this.nextLevel.coherence == CoherenceType.LowerLevelCoherent)
					{}//TODO
					
					//If the cache level is Write-through
					else if (this.isLastLevel || ((this.nextLevel.coherence == CoherenceType.None) 
							&& (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)))
					{
						if (this.isLastLevel)
						{
							if (this.levelFromTop == CacheType.L1 || this.levelFromTop == CacheType.iCache)
								MemorySystem.mainMemory.getPort().put(
										new AddressCarryingEvent(
												eventQ,
												MemorySystem.mainMemory.getLatencyDelay(),
												this,
												MemorySystem.mainMemory,
												RequestType.Main_Mem_Write,
												address));
							else
								MemorySystem.mainMemory.getPort().put(
										event.update(
												eventQ,
												MemorySystem.mainMemory.getLatencyDelay(),
												this,
												MemorySystem.mainMemory,
												RequestType.Main_Mem_Write));
						}
						else
						{
							if (this.levelFromTop == CacheType.L1 || this.levelFromTop == CacheType.iCache)
								this.nextLevel.getPort().put(
										new AddressCarryingEvent(
												eventQ,
												this.nextLevel.getLatencyDelay(),
												this,
												this.nextLevel,
												RequestType.Cache_Write, 
												address));
							else
								this.nextLevel.getPort().put(
									event.update(
											eventQ,
											this.nextLevel.getLatencyDelay(),
											this,
											this.nextLevel,
											RequestType.Cache_Write));
						}
					}						
				}
			}
			
			//IF MISS
			else
			{			
//System.out.println("Encountered a miss!!");
				//Add the request to the outstanding request buffer
				boolean alreadyRequested = this.addOutstandingRequest(event, address);
				
				if (!alreadyRequested)
				{
					if ((!this.isLastLevel) && this.nextLevel.coherence == CoherenceType.Snoopy)
					{
						if (requestType == RequestType.Cache_Read)
							this.nextLevel.busController.processReadMiss(eventQ, this, address);
						else if (requestType == RequestType.Cache_Write)
							this.nextLevel.busController.processWriteMiss(eventQ, this, address);
						else
						{
							System.err.println("Error : This must not be happening");
							System.exit(1);
						}
					}
					else if ((!this.isLastLevel) && this.nextLevel.coherence == CoherenceType.Directory)
					{
//System.out.println("Encountered a miss in directory!!");
						long directoryDelay=0;
						/* remove the block size */
						long tag = address >>> this.blockSizeBits;

						/* search all the lines that might match */
						
						long laddr = tag >>> this.assocBits;
						laddr = laddr << assocBits; //Replace the associativity bits with zeros.

						/* remove the tag portion */
						laddr = laddr & numLinesMask;
						int cacheLineNum=(int)(laddr/(long)blockSize);//TODO is this correct ?
																//TODO long to int typecast ? need an array indexed by long ?
						int containingCore = containingMemSys.getCore().getCore_number();//TODO Is this correct ?
	
						updateDirectory(cacheLineNum, containingCore,requestType, eventQ, address, event);//FIXME reduce number of arguments
						
					}//TODO
					else if ((!this.isLastLevel) && this.nextLevel.coherence == CoherenceType.LowerLevelCoherent)
					{}//TODO
					
					// access the next level
					else 
					{
						if (this.isLastLevel)
						{
							MemorySystem.mainMemory.getPort().put(
									new AddressCarryingEvent(
											eventQ,
											MemorySystem.mainMemory.getLatencyDelay(),
											this, 
											MemorySystem.mainMemory,
											RequestType.Main_Mem_Read,
											address));
							return;
						}
						else
						{
							this.nextLevel.getPort().put(
									new AddressCarryingEvent(
											eventQ,
											this.nextLevel.getLatencyDelay(),
											this, 
											this.nextLevel,
											RequestType.Cache_Read, 
											address));
							return;
						}
					}
				}
			}
		}
		
		protected void handleMemResponse(EventQueue eventQ, Event event)
		{
			this.fillAndSatisfyRequests(eventQ, event, MESI.EXCLUSIVE);
		}
		
		protected void fillAndSatisfyRequests(EventQueue eventQ, Event event, MESI stateToSet)
		{		
			long addr = ((AddressCarryingEvent)(event)).getAddress();
			
			CacheLine evictedLine = this.fill(addr, stateToSet);
			if (evictedLine != null && evictedLine.getState() == MESI.MODIFIED) //This does not ensure inclusiveness
			{
				if (this.isLastLevel)
					MemorySystem.mainMemory.getPort().put(
							new AddressCarryingEvent(
									eventQ,
									MemorySystem.mainMemory.getLatencyDelay(),
									this, 
									MemorySystem.mainMemory,
									RequestType.Main_Mem_Write,
									evictedLine.getTag() << this.blockSizeBits));
				else
					this.nextLevel.getPort().put(
							new AddressCarryingEvent(
									eventQ,
									this.nextLevel.getLatencyDelay(),
									this,
									this.nextLevel,
									RequestType.Cache_Write,
									evictedLine.getTag() << this.blockSizeBits));
			}
			
			long blockAddr = addr >>> this.blockSizeBits;
			if (!/*NOT*/this.missStatusHoldingRegister.containsKey(blockAddr))
			{
				System.err.println("Memory System Error : An outstanding request not found in the requesting element");
				System.exit(1);
			}
			
			ArrayList<Event> outstandingRequestList = this.missStatusHoldingRegister.get(blockAddr);
			
			while (!/*NOT*/outstandingRequestList.isEmpty())
			{				
				if (outstandingRequestList.get(0).getRequestType() == RequestType.Cache_Read)
				{
					//Pass the value to the waiting element
					//FIXME : Check the logic before finalizing
					if (this.levelFromTop != CacheType.L1 || (!MemorySystem.bypassLSQ))
						outstandingRequestList.get(0).getRequestingElement().getPort().put(
								outstandingRequestList.get(0).update(
										eventQ,
										0, //For same cycle response //outstandingRequestList.get(0).getRequestingElement().getLatencyDelay(),
										this,
										outstandingRequestList.get(0).getRequestingElement(),
										RequestType.Mem_Response));
					else if (containingMemSys.getCore().isPipelineInorder)
						//TODO Return the call to Inorder pipeline
						outstandingRequestList.get(0).getRequestingElement().getPort().put(
								new ExecCompleteEvent(
										containingMemSys.getCore().getEventQueue(),
										0,
										null,
										outstandingRequestList.get(0).getRequestingElement(),
										RequestType.EXEC_COMPLETE,
										null));
				}
				
				else if (outstandingRequestList.get(0).getRequestType() == RequestType.Cache_Read_from_iCache)
				{
					outstandingRequestList.get(0).getRequestingElement().getPort().put(
							outstandingRequestList.get(0).update(
									eventQ,
									0, //For same cycle response //outstandingRequestList.get(0).getRequestingElement().getLatencyDelay(),
									this,
									outstandingRequestList.get(0).getRequestingElement(),
									RequestType.Mem_Response));
				}
				
				else if (outstandingRequestList.get(0).getRequestType() == RequestType.Cache_Write)
				{
					//Write the value to the block (Do Nothing)
					//Handle further writes for Write through
					if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
					{
						//Handle in any case (Whether requesting element is LSQ or cache)
						//TODO : handle write-value forwarding (for Write-Through and Coherent caches)
						long address;
						if (this.levelFromTop == CacheType.L1 && !MemorySystem.bypassLSQ)
							address = ((LSQEntryContainingEvent)(event)).getLsqEntry().getAddr();
						else
							address = ((AddressCarryingEvent)(event)).getAddress();
							
						
						if (this.isLastLevel)
						{
							if (this.levelFromTop == CacheType.L1)
								MemorySystem.mainMemory.getPort().put(
										new AddressCarryingEvent(
												eventQ,
												MemorySystem.mainMemory.getLatencyDelay(),
												this,
												MemorySystem.mainMemory,
												RequestType.Main_Mem_Write,
												address));
							else
								MemorySystem.mainMemory.getPort().put(
										event.update(
												eventQ,
												MemorySystem.mainMemory.getLatencyDelay(),
												this,
												MemorySystem.mainMemory,
												RequestType.Main_Mem_Write));
						}
						else if (this.nextLevel.coherence != CoherenceType.Snoopy)
						{
							if (this.levelFromTop == CacheType.L1)
								this.nextLevel.getPort().put(
										new AddressCarryingEvent(
												eventQ,
												this.nextLevel.getLatencyDelay(),
												this,
												this.nextLevel,
												RequestType.Cache_Write,
												address));
							else
								this.nextLevel.getPort().put(
										event.update(
												eventQ,
												this.nextLevel.getLatencyDelay(),
												this,
												this.nextLevel,
												RequestType.Cache_Write));
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
						CacheLine cl = this.access(addr);
						if (cl != null)
							cl.setState(MESI.MODIFIED);
					}
				}
				else
				{
					System.err.println("Cache Error : A request was of type other than Cache_Read or Cache_Write. The encountered request type was : " + outstandingRequestList.get(0).getRequestType());
					System.exit(1);
				}
				
				//Remove the processed entry from the outstanding request list
				outstandingRequestList.remove(0);
			}
		}
		
		private void handleRequestForCopy(EventQueue eventQ, Event event)
		{
			this.nextLevel.busController.getBusAndPutEvent(
					event.update(eventQ, 
							event.getRequestingElement().getLatencyDelay(),
							this, 
							event.getRequestingElement(), 
							RequestType.Reply_with_shared_copy));
		}
		
		private void handleRequestForModifiedCopy(EventQueue eventQ, Event event)
		{
			ArrayList<Event> eventList = new ArrayList<Event>();
			long addr = ((AddressCarryingEvent)event).getAddress();
			eventList.add(
					event.update(eventQ,
							event.getRequestingElement().getLatencyDelay(),
							this, 
							event.getRequestingElement(), 
							RequestType.Reply_with_shared_copy));
			eventList.add(
					new AddressCarryingEvent(eventQ, 
							this.nextLevel.getLatencyDelay(),
							this, 
							this.nextLevel, 
							RequestType.Cache_Write,
							addr));
			this.nextLevel.busController.getBusAndPutEvents(eventList);
			
			CacheLine cl = this.access(addr);
			if (cl != null)
				cl.setState(MESI.SHARED);
		}
		
		private void handleReplyWithSharedCopy(EventQueue eventQ, Event event)
		{
			this.fillAndSatisfyRequests(eventQ, event, MESI.SHARED);
		}
		
		private void handleWriteModifiedToSharedMem(EventQueue eventQ, Event event)
		{
			SimulationElement requestingCache = event.getRequestingElement();
			long addr = ((AddressCarryingEvent)event).getAddress();
			this.nextLevel.busController.getBusAndPutEvent(
					event.update(
							eventQ,
							this.nextLevel.getLatencyDelay(),
							this,
							this.nextLevel,
							RequestType.Cache_Write));
			this.nextLevel.busController.getBusAndPutEvent(
					new AddressCarryingEvent(
							eventQ,
							this.nextLevel.getLatencyDelay(),
							requestingCache,
							this.nextLevel,
							RequestType.Cache_Read,
							addr));
			
			CacheLine cl = this.access(addr);
			if (cl != null)
				cl.setState(MESI.INVALID);
		}
		
		private void handleInvalidate(Event event)
		{
			CacheLine cl = this.access(((AddressCarryingEvent)event).getAddress());
			if (cl != null)
				cl.setState(MESI.INVALID);
		}
		
		public void updateDirectory(int cacheLine, int requestingCore, RequestType reqType, EventQueue eventQ, long address, Event event) {
//System.out.println("Coming inside update directory!");
if(cacheLine > CentralizedDirectory.numOfEntries){
	System.out.println("Outside directory range!"+cacheLine);
	if(reqType==RequestType.Cache_Read){
		if (this.isLastLevel)
		{
			MemorySystem.mainMemory.getPort().put(
					new AddressCarryingEvent(
							eventQ,
							MemorySystem.mainMemory.getLatencyDelay(),
							this, 
							MemorySystem.mainMemory,
							RequestType.Main_Mem_Read,
							address));
			return;
		}
		else
		{
			this.nextLevel.getPort().put(
					new AddressCarryingEvent(
							eventQ,
							this.nextLevel.getLatencyDelay(),
							this, 
							this.nextLevel,
							RequestType.Cache_Read, 
							address));
			return;
		}
	}
	return;
}
			if(reqType==RequestType.Cache_Read){
				readMissUpdate(cacheLine, requestingCore, eventQ,address,event);
			}
			else if(reqType==RequestType.Cache_Write){
				writeMissUpdate(cacheLine, requestingCore,eventQ,address,event);
			}
			else{
				System.out.println("Inside Centralized Directory, Encountered an event which is neither cache read nor cache write");
				return;
			}
		}
		
		private void writeMissUpdate(int cacheLine, int requestingCore, EventQueue eventQ, long address, Event event) {
//System.out.println("Directory Write");			
//Hashtable<Long, DirectoryEntry> directory = CentralizedDirectory.directory;
DirectoryEntry[] directory = CentralizedDirectory.directory;

			int numPresenceBits = CentralizedDirectory.numPresenceBits;
			int directoryAccessDelay=SystemConfig.directoryAccessLatency;
			int memWBDelay=SystemConfig.memWBDelay;
			int invalidationSendDelay=SystemConfig.invalidationSendDelay;
			int invalidationAckCollectDelay=SystemConfig.invalidationAckCollectDelay;
			int ownershipChangeDelay=SystemConfig.ownershipChangeDelay;
//			DirectoryEntry dirEntry = directory.get((long)cacheLine);
			DirectoryEntry dirEntry = directory[cacheLine];
			DirectoryState state= dirEntry.getState();
			//DirectoryEntry dirEntry= dirEntry;
			if(state==DirectoryState.uncached){
				dirEntry.setPresenceBit(requestingCore, true);			
				dirEntry.setState(DirectoryState.exclusive);
//				directory.put((long)cacheLine,dirEntry);
				if (this.isLastLevel)
				{
					MemorySystem.mainMemory.getPort().put(
							new AddressCarryingEvent(
									eventQ,
									MemorySystem.mainMemory.getLatencyDelay()+directoryAccessDelay,
									this, 
									MemorySystem.mainMemory,
									RequestType.Main_Mem_Write,
									address));
					return;
				}
				else
				{
					this.nextLevel.getPort().put(
							new AddressCarryingEvent(
									eventQ,
									this.nextLevel.getLatencyDelay()+directoryAccessDelay,
									this, 
									this.nextLevel,
									RequestType.Cache_Write, 
									address));
					return;
				}
				
			}
			else if(state==DirectoryState.readOnly){
				for(int i=0;i<numPresenceBits;i++){
					if(dirEntry.getPresenceBit(i)){
						//TODO send invalidation messages
						if(i!=requestingCore){
							dirEntry.setPresenceBit(i,false);
							this.nextLevel.prevLevel.get(i).getPort().put(
									new AddressCarryingEvent(
											eventQ,
											directoryAccessDelay+invalidationAckCollectDelay+invalidationSendDelay,
											this, 
											this.nextLevel.prevLevel.get(i),
											RequestType.MESI_Invalidate, 
											address));
						}
					}
				}
				dirEntry.setPresenceBit(requestingCore, true);			
				dirEntry.setState(DirectoryState.exclusive);
				//TODO Check if it is correct!
				fill(address,MESI.EXCLUSIVE);
				if (this.isLastLevel)
				{
					MemorySystem.mainMemory.getPort().put(
							new AddressCarryingEvent(
									eventQ,
									MemorySystem.mainMemory.getLatencyDelay()+directoryAccessDelay+invalidationAckCollectDelay+invalidationSendDelay,
									this, 
									MemorySystem.mainMemory,
									RequestType.Main_Mem_Read,
									address));
					return;
				}
				else
				{
					this.nextLevel.getPort().put(
							new AddressCarryingEvent(
									eventQ,
									this.nextLevel.getLatencyDelay()+directoryAccessDelay+invalidationAckCollectDelay+invalidationSendDelay,
									this, 
									this.nextLevel,
									RequestType.Cache_Read, 
									address));
					return;
				}
			}
			else if(state==DirectoryState.exclusive){
				//TODO send ownership change message
				int ownerNum = dirEntry.getOwner();
				if(ownerNum==-1)
					System.out.println("Nobody owns this line. Some Error.");
				dirEntry.setPresenceBit(ownerNum,false);
				dirEntry.setPresenceBit(requestingCore, true);			
				dirEntry.setState(DirectoryState.exclusive);
				
//				directory.put((long)cacheLine,dirEntry);

				//TODO Check if it is correct!
				fill(address,MESI.EXCLUSIVE);
				
				if (this.isLastLevel)
				{
					MemorySystem.mainMemory.getPort().put(
							new AddressCarryingEvent(
									eventQ,
									MemorySystem.mainMemory.getLatencyDelay()+directoryAccessDelay+ownershipChangeDelay,
									this, 
									MemorySystem.mainMemory,
									RequestType.Main_Mem_Read,
									address));
					return;
				}
				else
				{
					this.nextLevel.getPort().put(
							new AddressCarryingEvent(
									eventQ,
									this.nextLevel.getLatencyDelay()+directoryAccessDelay+ownershipChangeDelay,
									this, 
									this.nextLevel,
									RequestType.Cache_Read, 
									address));
					return;
				}
				
			}
			else
				return;
		}
		
		private void writeHitUpdate(int cacheLine, int requestingCore, EventQueue eventQ, long address, Event event){
			DirectoryEntry[] directory = CentralizedDirectory.directory;
			int directoryAccessDelay=SystemConfig.directoryAccessLatency;
			int memWBDelay=SystemConfig.memWBDelay;
			int invalidationSendDelay=SystemConfig.invalidationSendDelay;
			int invalidationAckCollectDelay=SystemConfig.invalidationAckCollectDelay;
			int ownershipChangeDelay=SystemConfig.ownershipChangeDelay;
//			DirectoryEntry dirEntry = directory.get((long)cacheLine);
			int numPresenceBits = CentralizedDirectory.numPresenceBits;
			DirectoryEntry dirEntry = directory[cacheLine];
			DirectoryState state= dirEntry.getState();
			SimulationElement requestingElement = event.getRequestingElement();
			if(state==DirectoryState.readOnly){
				for(int i=0;i<numPresenceBits;i++){
					if(dirEntry.getPresenceBit(i)){
						//Invalidate others
						if(i!=requestingCore){
							dirEntry.setPresenceBit(i,false);
							this.nextLevel.prevLevel.get(i).getPort().put(
									new AddressCarryingEvent(
											eventQ,
											directoryAccessDelay+invalidationAckCollectDelay+invalidationSendDelay,
											this, 
											this.nextLevel.prevLevel.get(i),
											RequestType.MESI_Invalidate, 
											address));
						}
					}
				}
				dirEntry.setPresenceBit(requestingCore, true);
				dirEntry.setState(DirectoryState.exclusive);
			}
		}

		private void readMissUpdate(int cacheLine, int requestingCore, EventQueue eventQ, long address, Event event) {
//Hashtable<Long, DirectoryEntry> directory = CentralizedDirectory.directory;
			DirectoryEntry[] directory = CentralizedDirectory.directory;
			int directoryAccessDelay=SystemConfig.directoryAccessLatency;
			int memWBDelay=SystemConfig.memWBDelay;
			int dataTransferDelay=SystemConfig.dataTransferDelay;
//			DirectoryEntry dirEntry = directory.get((long)cacheLine);
			DirectoryEntry dirEntry = directory[cacheLine];
			DirectoryState state= dirEntry.getState();
			SimulationElement requestingElement = event.getRequestingElement();
			if(state==DirectoryState.readOnly){
//System.out.println("Directory Read 1");
				dirEntry.setPresenceBit(requestingCore, true);
				
				//TODO Check if it is correct! 
				fill(address,MESI.SHARED);
				
				requestingElement .getPort().put(
						event.update(
								eventQ,
								requestingElement.getLatencyDelay()+directoryAccessDelay,
								this,
								requestingElement,
								RequestType.Mem_Response));
			}
			else if(state==DirectoryState.uncached ){
//System.out.println("Directory Read 2");
				dirEntry.setPresenceBit(requestingCore, true);
				dirEntry.setState(DirectoryState.readOnly);
				if (this.isLastLevel)
				{
					MemorySystem.mainMemory.getPort().put(
							new AddressCarryingEvent(
									eventQ,
									MemorySystem.mainMemory.getLatencyDelay()+directoryAccessDelay,
									this, 
									MemorySystem.mainMemory,
									RequestType.Main_Mem_Read,
									address));
					return;
				}
				else
				{
					this.nextLevel.getPort().put(
							new AddressCarryingEvent(
									eventQ,
									this.nextLevel.getLatencyDelay()+directoryAccessDelay,
									this, 
									this.nextLevel,
									RequestType.Cache_Read, 
									address));
					return;
				}
			}
			else if(state==DirectoryState.exclusive){
//System.out.println("Directory Read 3");
				dirEntry.setPresenceBit(requestingCore, true);
				dirEntry.setState(DirectoryState.readOnly);
				//TODO Check if it is correct!
				fill(address,MESI.SHARED);
				
				requestingElement.getPort().put(
						event.update(
								eventQ,
								requestingElement.getLatencyDelay()+directoryAccessDelay+dataTransferDelay,
								this,
								requestingElement,
								RequestType.Mem_Response));
				//TODO should write back to memory be done ?
				return;
			}
			else
				return;
		}
}