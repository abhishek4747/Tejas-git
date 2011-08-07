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
		protected boolean isLastLevel; //Tells whether there are any more levels of cache
		protected CacheConfig.WritePolicy writePolicy; //WRITE_BACK or WRITE_THROUGH
		
		protected String nextLevelName; //Name of the next level cache according to the configuration file
		protected ArrayList<Cache> prevLevel = new ArrayList<Cache>(); //Points towards the previous level in the cache hierarchy
		protected Cache nextLevel; //Points towards the next level in the cache hierarchy

		protected CacheLine lines[];
		
		protected Hashtable<Long, ArrayList<CacheOutstandingRequestTableEntry>> outstandingRequestTable
						= new Hashtable<Long, ArrayList<CacheOutstandingRequestTableEntry>>();
		
		public int hits;
		public int misses;
		public int evictions;
		
		//For telling that which banks are accessed this cycle (for BANKED multi-port option)
		protected ArrayList<Integer> banksAccessedThisCycle = new ArrayList<Integer>();
		
		//For telling how many requests are processed this cycle (for GENUINELY multi-ported option)
		protected int requestsProcessedThisCycle = 0;
		
		public static final long NOT_EVICTED = -1;
		
		protected CacheLine access(long addr)
		{
			/* remove the block size */
			long tag = addr >> this.blockSizeBits;

			/* search all the lines that might match */
			
			long laddr = tag >> this.assocBits;
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
		
		public Cache(CacheConfig cacheParameters)
		{
			// set the parameters
			//containingMemSys = _containingMemSys;
			this.blockSize = cacheParameters.getBlockSize();
			this.assoc = cacheParameters.getAssoc();
			this.size = cacheParameters.getSize();
			this.blockSizeBits = Util.logbase2(blockSize);
			this.assocBits = Util.logbase2(assoc);
			this.numLines = getNumLines();
			
			this.writePolicy = cacheParameters.getWritePolicy();
			this.isLastLevel = cacheParameters.isLastLevel();
			this.nextLevelName = cacheParameters.getNextLevel();
			this.latency = cacheParameters.getLatency();
			//this.setPorts(cacheParameters.getAccessPorts());
			//this.setMultiPortType(cacheParameters.getMultiportType());
			
			this.numLinesBits = Util.logbase2(numLines);
			this.timestamp = 0;
			this.numLinesMask = numLines - 1;
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
			{
				mark(cl);
				
				
				
				/*if (this.writeMode == CacheConfig.writeModes.WRITE_BACK) //Set the modified-bit only if the cache is write-back
					cl.setModified(true);
				else //The cache is write through
				{
					//Write through in the next level or memory
					if (this.isLastLevel)
					{
						//TODO Enter code for writing to main memory
						Stack<CacheFillStackEntry> fillStack = new Stack<CacheFillStackEntry>();
						MemEventQueue.eventQueue/*.get(containingMemSys.threadID)*.add(new MainMemAccessEvent(containingMemSys.threadID,
																			null, 
																			fillStack, 
																			MemEventQueue.clock
																			+ SystemConfig.mainMemoryLatency));
					}
					else
					{
						CacheRequestPacket writeThrough = new CacheRequestPacket();
						writeThrough.setAddr(addr);
						writeThrough.setType(MemoryAccessType.WRITE);
						writeThrough.setThreadID(0); //TODO This has to be taken care of in memory consistency
						writeThrough.setWriteThrough(true);//TODO For testing purposes only
						//this.nextLevel.processEntry(writeThrough);
						Stack<CacheFillStackEntry> fillStack = new Stack<CacheFillStackEntry>();
						MemEventQueue.eventQueue/*.get(containingMemSys.threadID)*.add(new CacheAccessEvent(containingMemSys.threadID,
																			null,
																			this.nextLevel, 
																			writeThrough, 
																			fillStack, 
																			MemEventQueue.clock
																			+ this.nextLevel.latency));
					}
				}*/
			}
			return cl;
		}
		
		protected CacheLine fill(long addr) //Returns a copy of the evicted line
		{
			CacheLine evictedLine = null;
			
			/* remove the block size */
			long tag = addr >> this.blockSizeBits;

			/* search all the lines that might match */
			long laddr = tag >> this.assocBits;
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
				double minTimeStamp = 100000000;
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
			
			/*if ((request.getType() == MemoryAccessType.WRITE) && (this.writeMode == CacheConfig.writeModes.WRITE_BACK))
			{
				fillLine.setState(MESI.MODIFIED);
			}
			else if (request.getType() == MemoryAccessType.READ)
			{
				fillLine.setState(MESI.EXCLUSIVE);
			}*/
//			fillLine.setState(stateToSet);
			return evictedLine;
		}
	
		public CacheLine processRequest(CacheRequestPacket request)
		{
			//boolean isHit;
			/* access the Cache */
			CacheLine ll = null;
			if(request.getType() == RequestType.MEM_READ)
				ll = this.read(request.getAddr());
			else if (request.getType() == RequestType.MEM_WRITE)
				ll = this.write(request.getAddr());
			
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
				long tag = addr >> this.blockSizeBits;

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
		protected void addOutstandingRequest(long addr, 
											RequestType requestType, 
											SimulationElement requestingElement,
											int index)
		{
			long blockAddr = addr >> blockSizeBits;
			
			if (!/*NOT*/outstandingRequestTable.containsKey(blockAddr))
			{
				outstandingRequestTable.put(blockAddr, new ArrayList<CacheOutstandingRequestTableEntry>());
			}
			
			outstandingRequestTable.get(blockAddr).add(new CacheOutstandingRequestTableEntry(requestType,
																							requestingElement,
																							addr,
																							index));
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
}