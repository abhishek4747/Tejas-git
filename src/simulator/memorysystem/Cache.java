/*****************************************************************************
				BhartiSim Simulator
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

import config.CacheConfig;
import config.SystemConfig;
import memorysystem.Bus.BusReqType;
import memorysystem.CacheLine.MESI;
import misc.Util;
import generic.*;

public class Cache extends SimulationElement
{
		/* cache parameters */
		//CoreMemorySystem containingMemSys;
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
		
		protected boolean enforcesCoherence = false; //The cache which is shared between the coherent cache level
		protected boolean isCoherent = false; //Tells whether the level is coherent or not
		protected boolean isFirstLevel = false;
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
		
//		//For telling that which banks are accessed this cycle (for BANKED multi-port option)
//		protected ArrayList<Integer> banksAccessedThisCycle = new ArrayList<Integer>();
		
//		//For telling how many requests are processed this cycle (for GENUINELY multi-ported option)
//		protected int requestsProcessedThisCycle = 0;
		
		public static final long NOT_EVICTED = -1;
		
		protected CacheLine access(long addr)
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
		
		private void mark(CacheLine ll, long tag)
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
		
		public Cache(CacheConfig cacheParameters, EventQueue eventQ)
		{
			super(cacheParameters.portType,
					cacheParameters.getAccessPorts(), 
					cacheParameters.getPortOccupancy(),
					eventQ,
					cacheParameters.getLatency(),
					cacheParameters.operatingFreq);
			
			// set the parameters
			this.blockSize = cacheParameters.getBlockSize();
			this.assoc = cacheParameters.getAssoc();
			this.size = cacheParameters.getSize();
			this.blockSizeBits = Util.logbase2(blockSize);
			this.assocBits = Util.logbase2(assoc);
			this.numLines = getNumLines();
			
			this.writePolicy = cacheParameters.getWritePolicy();
			this.isFirstLevel = cacheParameters.isFirstLevel();
			this.isLastLevel = cacheParameters.isLastLevel();
			this.nextLevelName = cacheParameters.getNextLevel();
			
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
		
		protected CacheLine fill(long addr) //Returns a copy of the evicted line
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
			fillLine.setState(MESI.SHARED);
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
		 * Tells whether the request of current event can be processed in the current cycle (due to device port availability)
		 * @param addr : The address to be accessed
		 * @return A boolean value :TRUE if the request can be processed and FALSE otherwise
		 */
/*		protected boolean canServiceRequest(long addr)
		{
			//For Genuinely multi-ported elements, if number of requests this cycle has not reached the total number of ports
			if  ((this.getMultiPortType() == MultiPortingType.GENUINE) && (this.requestsProcessedThisCycle < this.ports))
			{
				requestsProcessedThisCycle++;
				return true;
			}
			
			//For Banked multi-ported elements
			else if (this.getMultiPortType() == MultiPortingType.BANKED)
			{
				/* remove the block size *
				long tag = addr >>> this.blockSizeBits;

				long lineIndex = tag & numLinesMask;
				
				if (!/*NOT*banksAccessedThisCycle.contains(lineIndex/(numLines/(this.ports))))
				{
					banksAccessedThisCycle.add((int) (lineIndex/(numLines/(this.ports))));
					return true;
				}
			}
			
			//Otherwise
			return false;
		}*/
		
		/**
		 * Used when a new request is made to a cache and there is a miss.
		 * This adds the request to the outstanding requests buffer of the cache
		 * @param blockAddr : Memory Address requested
		 * @param requestType : MEM_READ or MEM_WRITE
		 * @param requestingElement : Which element made the request. Helpful in backtracking and filling the stack
		 */
		protected boolean addOutstandingRequest(Event event, long addr)
//											long addr, 
//											RequestType requestType, 
//											SimulationElement requestingElement,
//											LSQEntry lsqEntry)
		{
			boolean entryAlreadyThere;
			
			long blockAddr = addr >>> blockSizeBits;
			
			if (!/*NOT*/missStatusHoldingRegister.containsKey(blockAddr))
			{
				entryAlreadyThere = false;
//				missStatusHoldingRegister.put(blockAddr, new ArrayList<CacheMissStatusHoldingRegisterEntry>());
				missStatusHoldingRegister.put(blockAddr, new ArrayList<Event>());
			}
			else
				entryAlreadyThere = true;
			
//			missStatusHoldingRegister.get(blockAddr).add(new CacheMissStatusHoldingRegisterEntry(requestType,
//																							requestingElement,
//																							addr,
//																							lsqEntry));
			missStatusHoldingRegister.get(blockAddr).add(event);
			
			return entryAlreadyThere;
		}
/*		
		/**
		 * When a data block requested by an outstanding request arrives through a BlockReadyEvent,
		 * this method is called to process the arrival of block and process all the outstanding requests.
		 * @param addr : Memory address requested
		 *
		protected void receiveOutstandingRequestBlock(long addr)
		{
			CacheLine evictedLine = this.fill(addr);//FIXME
			
			if (!/*NOT*outstandingRequestTable.containsKey(addr))
			{
				System.err.println("Memory System Crash : An outstanding request not found in the requesting element");
				System.exit(1);
			}
			
			ArrayList<CacheOutstandingRequestTableEntry> outstandingRequestList = outstandingRequestTable.get(addr);
			
			while (!/*NOT*outstandingRequestList.isEmpty())
			{
				if (outstandingRequestList.get(0).requestType == RequestType.MEM_READ)
				{
					//Pass the value to the waiting element
					//Create an event (BlockReadyEvent) for the waiting element
					if (outstandingRequestList.get(0).lsqIndex == LSQ.INVALID_INDEX)
						//Generate the event for the Upper level cache
						MemEventQueue.eventQueue.add(new BlockReadyEvent(outstandingRequestList.get(0).requestingElement.getLatency(), 
																	this,
																	outstandingRequestList.get(0).requestingElement, 
																	0, //tieBreaker
																	RequestType.MEM_BLOCK_READY));
					else
						//Generate the event to tell the LSQ
						MemEventQueue.eventQueue.add(new BlockReadyEvent(outstandingRequestList.get(0).requestingElement.getLatency(), 
																	this,
																	outstandingRequestList.get(0).requestingElement, 
																	0, //tieBreaker
																	RequestType.MEM_BLOCK_READY,
																	outstandingRequestList.get(0).lsqIndex));
				}
				
				else if (outstandingRequestList.get(0).requestType == RequestType.MEM_WRITE)
				{
					//Write the value to the block (Do Nothing)
					//Pass the value to the waiting element
					//Create an event (BlockReadyEvent) for the waiting element
					if (outstandingRequestList.get(0).lsqIndex != LSQ.INVALID_INDEX)
						//(If the requesting element is LSQ)
						//Generate the event to tell the LSQ
						MemEventQueue.eventQueue.add(new BlockReadyEvent(outstandingRequestList.get(0).requestingElement.getLatency(), 
																	this,
																	outstandingRequestList.get(0).requestingElement,
																	0, //tieBreaker
																	RequestType.MEM_BLOCK_READY,
																	outstandingRequestList.get(0).lsqIndex));
					
					//Handle in any case (Whether requesting element is LSQ or cache)
					//TODO : handle write-value forwarding (for Write-Through and Coherent caches)
					
				}
				else
				{
					System.err.println("Memory System Crash : A request was of type other than MEM_READ or MEM_WRITE");
					System.exit(1);
				}
			}
		}*/
		
		public void handleEvent(Event event)
		{
			if (event.getRequestType() == RequestType.Cache_Read
					|| event.getRequestType() == RequestType.Cache_Write)
				handleAccess(event);
			else if (event.getRequestType() == RequestType.Mem_Response)
				handleMemResponse(event);
		}
		
		private void handleAccess(Event event)
		{
			SimulationElement requestingElement = event.getRequestingElement();
			RequestType requestType = event.getRequestType();
			long address;
			
			if (this.isFirstLevel && !MemorySystem.bypassLSQ)
				address = ((LSQEntry)(event.getPayload())).getAddr();
			else
				address = (Long)(event.getPayload());
			
			//Process the access
			CacheLine cl = this.processRequest(requestType, address);

			//IF HIT
			if (cl != null)
			{
				//Schedule the requesting element to receive the block TODO (for LSQ)
				if (requestType == RequestType.Cache_Read)
					//Just return the read block
					requestingElement.getPort().put(event.update(requestingElement.getLatencyDelay(),
																this,
																requestingElement,
																0,//tieBreaker,
																RequestType.Mem_Response,
																event.getPayload()));
//					requestingElement.getPort().put(new BlockReadyEvent(requestingElement.getLatencyDelay(), 
//																this.processingCache,
//																this.getRequestingElement(),
//																0,//tieBreaker
//																RequestType.MEM_BLOCK_READY,
//																request.getAddr(),
//																lsqEntry));
				else if (requestType == RequestType.Cache_Write)
				{
					//Write the data to the cache block (Do Nothing)
	/*				//Tell the LSQ (if this is L1) that write is done
					if (lsqIndex != LSQ.INVALID_INDEX)
					{
						eventQueue.addEvent(new BlockReadyEvent(new Time_t(GlobalClock.getCurrentTime() +
																		this.getRequestingElement().getLatency().getTime()), 
																	this.processingCache,
																	this.getRequestingElement(),
																	0,//tieBreaker
																	RequestType.LSQ_WRITE_COMMIT,
																	request.getAddr(),
																	lsqIndex));
					}	
	*/
					//If the cache level is Write-through
					if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
					{
						//Handle in any case (Whether requesting element is LSQ or cache)
						//TODO : handle write-value forwarding (for Write-Through and Coherent caches)
						if (this.isLastLevel)
							MemorySystem.mainMemPort.put(event.update(MemorySystem.getMainMemLatencyDelay(),
																	this,
																	null,
																	0,//tieBreaker,
																	RequestType.Main_Mem_Write,
																	address));
//							MemorySystem.mainMemPort.put(new NewMainMemAccessEvent(MemorySystem.getMainMemLatencyDelay(),//FIXME :main memory latency is going to come here
//																			processingCache, 
//																			0, //tieBreaker,
//																			request.getAddr(),
//																			RequestType.MEM_WRITE));
						else
							this.nextLevel.getPort().put(event.update(this.nextLevel.getLatencyDelay(),
																	this,
																	this.nextLevel,
																	0,//tieBreaker,
																	RequestType.Cache_Write, 
																	address));
//							this.nextLevel.getPort().put(new NewCacheAccessEvent(processingCache.nextLevel.getLatencyDelay(),//FIXME
//																			processingCache,
//																			processingCache.nextLevel,
//																			null, 
//																			0, //tieBreaker,
//																			request));
					}
					else
					{
						Core.outstandingMemRequests--;
					}
						
				}
			}
			
			//IF MISS
			else
			{			
				//Add the request to the outstanding request buffer
				boolean alreadyRequested = this.addOutstandingRequest(event, address);
//														address, 
//														event.getRequestType(), 
//														this.getRequestingElement(), 
//														lsqEntry);
				
				if (!alreadyRequested)
				{
					// access the next level
					if (this.isLastLevel)
					{
						//FIXME
						MemorySystem.mainMemPort.put(event.update(MemorySystem.getMainMemLatencyDelay(),
																this, 
																null, 
																0,//tieBreaker,
																RequestType.Main_Mem_Read,
																address));
//						MemorySystem.mainMemPort.put(new NewMainMemAccessEvent(MemorySystem.getMainMemLatencyDelay(), //FIXME 
//																		processingCache, 
//																		0, //tieBreaker,
//																		request.getAddr(),
//																		RequestType.MEM_READ));
						return;
					}
					else
					{
						//Change the parameters of this event to forward it for scheduling next cache's access
//						event.setEventTime(processingCache.nextLevel.getLatencyDelay());//FIXME
//						event.setRequestingElement(processingCache);
//						event.processingCache = processingCache.nextLevel;
//						event.lsqEntry = null;
//						event.request.setType(RequestType.MEM_READ);
						this.nextLevel.getPort().put(event.update(this.nextLevel.getLatencyDelay(),
																this, 
																this.nextLevel,
																0,//tieBreaker, 
																RequestType.Cache_Read, 
																address));
						return;
					}
				}
			}
		}
		
		private void handleMemResponse(Event event)
		{
			long addr = (Long)(event.getPayload());
			
			CacheLine evictedLine = this.fill(addr);//FIXME : Have to handle whole eviction process
			if (evictedLine != null)
			{
				if (this.isLastLevel)
					MemorySystem.mainMemPort.put(new Event(MemorySystem.getMainMemLatencyDelay(),//FIXME :main memory latency is going to come here
															this, 
															null,
															0, //tieBreaker,
															RequestType.Main_Mem_Write,
															evictedLine.getTag() << this.blockSizeBits));
				else
					this.nextLevel.getPort().put(new Event(this.nextLevel.getLatencyDelay(),//FIXME
															this,
															this.nextLevel,
															0, //tieBreaker,
															RequestType.Cache_Write,
															evictedLine.getTag() << this.blockSizeBits));
			}
			
			long blockAddr = addr >>> this.blockSizeBits;
			if (!/*NOT*/this.missStatusHoldingRegister.containsKey(blockAddr))
			{
				System.err.println("Memory System Error : An outstanding request not found in the requesting element");
				System.exit(1);
			}
			
//			ArrayList<CacheMissStatusHoldingRegisterEntry> outstandingRequestList = this.missStatusHoldingRegister.remove(blockAddr);
			ArrayList<Event> outstandingRequestList = this.missStatusHoldingRegister.remove(blockAddr);
			
			while (!/*NOT*/outstandingRequestList.isEmpty())
			{
				if (outstandingRequestList.get(0).getRequestType() == RequestType.Cache_Read)
				{
					//Pass the value to the waiting element
					//Create an event (BlockReadyEvent) for the waiting element
					//Generate the event for the Upper level cache or LSQ
					Class lsqClass = LSQ.class;
//					if (outstandingRequestList.get(0).lsqEntry == null)
//					if (!/*NOT*/lsqClass.isInstance(outstandingRequestList.get(0).getRequestingElement()))
						//Generate the event for the Upper level cache
						outstandingRequestList.get(0).getRequestingElement().getPort().put(
								outstandingRequestList.get(0).update(outstandingRequestList.get(0).getRequestingElement().getLatencyDelay(),
																	this,
																	outstandingRequestList.get(0).getRequestingElement(),
																	0,//tieBreaker,
																	RequestType.Mem_Response,
																	outstandingRequestList.get(0).getPayload()));
//								new Event(outstandingRequestList.get(0).getRequestingElement().getLatencyDelay(),//FIXME 
//																this.getProcessingElement(),
//																outstandingRequestList.get(0).requestingElement, 
//																0, //tieBreaker
//																RequestType.MEM_BLOCK_READY,
//																outstandingRequestList.get(0).address,
//																outstandingRequestList.get(0).lsqEntry));
//					else
						//Generate the event to tell the LSQ
//						outstandingRequestList.get(0).getRequestingElement().getPort().put(new BlockReadyEvent(outstandingRequestList.get(0).requestingElement.getLatencyDelay(),//FIXME 
//																this.getProcessingElement(),
//																outstandingRequestList.get(0).requestingElement, 
//																0, //tieBreaker
//																RequestType.LSQ_LOAD_COMPLETE,
//																outstandingRequestList.get(0).address,
//																outstandingRequestList.get(0).lsqEntry));
				}
				
				else if (outstandingRequestList.get(0).getRequestType() == RequestType.Cache_Write)
				{
					//Write the value to the block (Do Nothing)
					//Pass the value to the waiting element
		/*			//Create an event (BlockReadyEvent) for the waiting element
					if (outstandingRequestList.get(0).lsqIndex != LSQ.INVALID_INDEX)
						//(If the requesting element is LSQ)
						//Generate the event to tell the LSQ
						eventQueue.addEvent(new BlockReadyEvent(new Time_t(GlobalClock.getCurrentTime() +
																			outstandingRequestList.get(0).requestingElement.getLatency().getTime()),//FIXME 
																this.getProcessingElement(),
																outstandingRequestList.get(0).requestingElement, 
																0, //tieBreaker
																RequestType.LSQ_WRITE_COMMIT,
																outstandingRequestList.get(0).address,
																lsqIndex));
		*/			
					if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
					{
						//Handle in any case (Whether requesting element is LSQ or cache)
						//TODO : handle write-value forwarding (for Write-Through and Coherent caches)
						long address;
						if (this.isFirstLevel)
							address = ((LSQEntry)(outstandingRequestList.get(0).getPayload())).getAddr();
						else
							address = (Long)(outstandingRequestList.get(0).getPayload());
							
						
						if (this.isLastLevel)
							MemorySystem.mainMemPort.put(event.update(MemorySystem.getMainMemLatencyDelay(),
									this,
									null,
									0,//tieBreaker,
									RequestType.Main_Mem_Write,
									address));
//							MemorySystem.mainMemPort.put(new NewMainMemAccessEvent(MemorySystem.getMainMemLatencyDelay(),//FIXME :main memory latency is going to come here
//																			this, 
//																			0, //tieBreaker,
//																			outstandingRequestList.get(0).address,
//																			RequestType.MEM_WRITE));
						else
							this.nextLevel.getPort().put(event.update(MemorySystem.getMainMemLatencyDelay(),
									this,
									null,
									0,//tieBreaker,
									RequestType.Cache_Write,
									address));
//							this.nextLevel.getPort().put(new NewCacheAccessEvent(this.nextLevel.getLatencyDelay(),//FIXME
//																			this,
//																			receivingCache.nextLevel,
//																			null, 
//																			0, //tieBreaker,
//																			new CacheRequestPacket(RequestType.MEM_WRITE,
//																								outstandingRequestList.get(0).address)));
					}
					else
					{
						Core.outstandingMemRequests--;
					}
				}
				else
				{
					System.err.println("Memory System Error : A request was of type other than MEM_READ or MEM_WRITE");
					System.exit(1);
				}
				
				//Remove the processed entry from the outstanding request list
				outstandingRequestList.remove(0);
			}
		}
}