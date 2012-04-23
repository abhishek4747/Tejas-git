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
				busController = new BusController(prevLevel, this, numberOfBuses, this, cacheParameters.getBusOccupancy());
			
			this.numLinesBits = Util.logbase2(numLines);
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
			if(cl != null)
				mark(cl);
			return cl;
		}
		
		protected CacheLine write(long addr)
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
					//evictedLines.addElement(fillLine.getTag());
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
		public int addOutstandingRequest(Event event, long addr)
		{
			int entryAlreadyThere = 0;
			
			long blockAddr = addr >>> blockSizeBits;
			
			if (!/*NOT*/missStatusHoldingRegister.containsKey(blockAddr))
			{
				entryAlreadyThere = 0;
				if(missStatusHoldingRegister.size() >= MSHRSize)
				{
					return 2;
				}
//				missStatusHoldingRegister.put(blockAddr, new ArrayList<CacheMissStatusHoldingRegisterEntry>());
				missStatusHoldingRegister.put(blockAddr, new OMREntry(new ArrayList<Event>(), false, null));
			}
			else if (missStatusHoldingRegister.get(blockAddr).outStandingEvents.isEmpty())
				entryAlreadyThere = 0;
			else if (missStatusHoldingRegister.get(blockAddr).outStandingEvents.size() <= MSHRSize)
				entryAlreadyThere = 1;
			else
				entryAlreadyThere = 2;
			
//			missStatusHoldingRegister.get(blockAddr).add(new CacheMissStatusHoldingRegisterEntry(requestType,
//																							requestingElement,
//																							addr,
//																							lsqEntry));
			if(entryAlreadyThere !=2)
				missStatusHoldingRegister.get(blockAddr).outStandingEvents.add(event);
			return entryAlreadyThere;
		}
		
		boolean isMSHRfull()
		{
			if(missStatusHoldingRegister.size() < MSHRSize)
			{
				return false;
			}
			else
			{
				Enumeration<OMREntry> omrEntryEnum = missStatusHoldingRegister.elements();
				while(omrEntryEnum.hasMoreElements())
				{
					if(omrEntryEnum.nextElement().outStandingEvents.size() < MSHRSize)
					{
						return false;
					}
				}
				return true;
			}
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


		
		protected void handleAccess(EventQueue eventQ, Event event)
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
				if(requestingElement.getClass() == MemUnitIn.class)
				{
					((MemUnitIn)requestingElement).getMissStatusHoldingRegister().remove(address);
				}
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
						this.nextLevel.busController.processWriteHit(eventQ, this, cl, address,((AddressCarryingEvent)event).coreId);
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
												address,
												((AddressCarryingEvent)event).coreId));
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
												address,
												((AddressCarryingEvent)event).coreId));
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
				int alreadyRequested = this.addOutstandingRequest(event, address);
				if (alreadyRequested == 0)
				{
					if(requestingElement.getClass() == MemUnitIn.class)
					{
						((MemUnitIn)requestingElement).getMissStatusHoldingRegister().remove(address);
					}
					if ((!this.isLastLevel) && this.nextLevel.coherence == CoherenceType.Snoopy)
					{
						if (requestType == RequestType.Cache_Read)
							this.nextLevel.busController.processReadMiss(eventQ, this, address,((AddressCarryingEvent)event).coreId);
						else if (requestType == RequestType.Cache_Write)
							this.nextLevel.busController.processWriteMiss(eventQ, this, address,((AddressCarryingEvent)event).coreId);
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
							AddressCarryingEvent addressEvent =	new AddressCarryingEvent(eventQ,
																						 MemorySystem.mainMemory.getLatencyDelay(),
																						 this, 
																						 MemorySystem.mainMemory,
																						 RequestType.Main_Mem_Read,
																						 address,
																						 ((AddressCarryingEvent)event).coreId); 
							MemorySystem.mainMemory.getPort().put(addressEvent);
							missStatusHoldingRegister.get((address >> blockSizeBits)).eventToForward = addressEvent;
							return;
						}
						else
						{
							AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																						 this.nextLevel.getLatencyDelay(),
																						 this, 
																						 this.nextLevel,
																						 RequestType.Cache_Read, 
																						 address,
																						 ((AddressCarryingEvent)event).coreId); 
							missStatusHoldingRegister.get((address >> blockSizeBits)).eventToForward = addressEvent;
							this.nextLevel.getPort().put(addressEvent);
							return;
						}
					}
				}
				else if(alreadyRequested == 1)
				{
					if(requestingElement.getClass() == MemUnitIn.class)
					{
						if(((MemUnitIn)requestingElement).getMissStatusHoldingRegister().containsKey(address))
							((MemUnitIn)requestingElement).getMissStatusHoldingRegister().remove(address);
						else
						{
							System.out.println("Request Not Present in Mem MSHR");
							System.exit(1);
						}
					}
				}
				else if(alreadyRequested == 2)
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
					else if (alreadyRequested ==2)
					{
						if(!this.connectedMSHR.contains(((MemUnitIn)requestingElement).getMissStatusHoldingRegister()))
							this.connectedMSHR.add(((MemUnitIn)requestingElement).getMissStatusHoldingRegister());
						((MemUnitIn)requestingElement).getMissStatusHoldingRegister().get(address).readyToProceed = true;
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
									evictedLine.getTag() << this.blockSizeBits,
									((AddressCarryingEvent)event).coreId));
				else
					this.nextLevel.getPort().put(
							new AddressCarryingEvent(
									eventQ,
									this.nextLevel.getLatencyDelay(),
									this,
									this.nextLevel,
									RequestType.Cache_Write,
									evictedLine.getTag() << this.blockSizeBits,
									((AddressCarryingEvent)event).coreId));
			}
			long blockAddr = addr >>> this.blockSizeBits;
			if (!this.missStatusHoldingRegister.containsKey(blockAddr))
			{
				System.err.println("Memory System Error : An outstanding request not found in the requesting element" + event.getRequestType() + event.getProcessingElement().getClass() + "  " + ((Cache)event.getProcessingElement()).levelFromTop);
				System.exit(1);
			}
			ArrayList<Event> outstandingRequestList = this.missStatusHoldingRegister.remove(blockAddr).outStandingEvents;
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
												address,
												((AddressCarryingEvent)event).coreId));
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
												address,
												((AddressCarryingEvent)event).coreId));
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
			while(connectedMSHR.size() > 0)
			{
				
				Hashtable<Long,OMREntry> tempMissStatusHoldingRegister = connectedMSHR.remove(0);
				Enumeration<OMREntry> omrIte = tempMissStatusHoldingRegister.elements();
				Enumeration<Long> omrKeys = tempMissStatusHoldingRegister.keys();
				while(omrIte.hasMoreElements())
				{
					OMREntry omrEntry = omrIte.nextElement();
					Long key = omrKeys.nextElement();
					if(omrEntry.readyToProceed)
					{
						SimulationElement requestingElement = omrEntry.eventToForward.getRequestingElement();
						if(requestingElement.getClass() != MemUnitIn.class)
						{
							omrEntry.readyToProceed = false;
						}
						handleAccess(eventQ, omrEntry.eventToForward);
					}
					if(missStatusHoldingRegister.size() >= MSHRSize)
					{
						break;
					}
				}
				if(missStatusHoldingRegister.size() >= MSHRSize)
				{
					break;
				}
			}
			/*}
			else
			{
				System.err.println("Memory System Error : An outstanding request not found in the requesting element" + event.getRequestType() + event.getProcessingElement().getClass() + "  " + ((Cache)event.getProcessingElement()).levelFromTop);
				System.exit(1);
			}*/
		}
		
		private void handleRequestForCopy(EventQueue eventQ, Event event)
		{
			this.nextLevel.busController.getBusAndPutEvent(event.update(eventQ, 
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
							addr,
							((AddressCarryingEvent)event).coreId));
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
							addr,
							((AddressCarryingEvent)event).coreId));
			
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
										address,
										((AddressCarryingEvent)event).coreId));
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
										address,
										((AddressCarryingEvent)event).coreId));
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
									address,
									((AddressCarryingEvent)event).coreId));
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
									address,
									((AddressCarryingEvent)event).coreId));
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
											address,
											((AddressCarryingEvent)event).coreId));
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
									address,
									((AddressCarryingEvent)event).coreId));
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
									address,
									((AddressCarryingEvent)event).coreId));
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
									address,
									((AddressCarryingEvent)event).coreId));
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
									address,
									((AddressCarryingEvent)event).coreId));
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
											address,
											((AddressCarryingEvent)event).coreId));
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
									address,
									((AddressCarryingEvent)event).coreId));
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
									address,
									((AddressCarryingEvent)event).coreId));
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