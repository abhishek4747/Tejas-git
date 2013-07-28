/*****************************************************************************
				Tejas Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
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

	Contributors:  Moksh Upadhyay, Smruti R. Sarangi 
*****************************************************************************/
package memorysystem;

import memorysystem.LSQEntry.LSQEntryType;
import pipeline.outoforder.OutOrderCoreMemorySystem;
import pipeline.outoforder.ReorderBufferEntry;

import generic.*;

public class LSQ extends SimulationElement
{
	CoreMemorySystem containingMemSys;
	protected LSQEntry[] lsqueue;
	protected int tail; // points to last valid entry
	protected int head; // points to first valid entry
	public int lsqSize;
	protected int curSize;
		
	public int noOfMemRequests = 0;
	public int NoOfLd = 0;
	public int NoOfSt = 0;
	public int NoOfForwards = 0; // Total number of forwards made by the LSQ
	
	public static final int INVALID_INDEX = -1;
	
	public LSQ(PortType portType, int noOfPorts, long occupancy, long latency, CoreMemorySystem containingMemSys, int lsqSize) 
	{
		super(portType, noOfPorts, occupancy, latency, containingMemSys.getCore().getFrequency());
		this.containingMemSys = containingMemSys;
		this.lsqSize = lsqSize;
		head = -1;
		tail = -1;
		curSize = 0;
		lsqueue = new LSQEntry[lsqSize];
		for(int i = 0; i < lsqSize; i++)
		{
			LSQEntry entry = new LSQEntry(LSQEntryType.LOAD, null);
			entry.setAddr(-1);
			entry.setIndexInQ(i);
			lsqueue[i] = entry;
		}
	}

	public LSQEntry addEntry(boolean isLoad, long address, ReorderBufferEntry robEntry) //To be accessed at the time of allocating the entry
	{
		noOfMemRequests++;
		LSQEntry.LSQEntryType type = (isLoad) ? LSQEntry.LSQEntryType.LOAD 
				: LSQEntry.LSQEntryType.STORE;
		
		if (isLoad)
			NoOfLd++;
		else
			NoOfSt++;
		
		if(head == -1)
		{
			head = tail = 0;
		}
		else
		{
			tail = (tail + 1)%lsqSize;
		}
		
		LSQEntry entry = lsqueue[tail];
		if(!entry.isRemoved())
			System.err.println("entry currently in use being re-allocated");
		entry.recycle();
		entry.setType(type);
		entry.setRobEntry(robEntry);
		entry.setAddr(address);
		this.curSize++;
		return entry;
	}

	public boolean loadValidate(LSQEntry entry)
	{
		//Test check
		if (lsqueue[entry.getIndexInQ()] != entry)
			System.err.println(" Entry index and actual entry dont match : LOAD" + entry.getIndexInQ());
		
		entry.setValid(true);
		boolean couldForward = loadResolve(entry.getIndexInQ(), entry);
		if(couldForward) 
		{
			NoOfForwards++;
			
			//Increment the counters for power calculations
			this.containingMemSys.getCore().powerCounters.incrementLsqAccess(1);
			this.containingMemSys.getCore().powerCounters.incrementLsqPregAccess(1);
			this.containingMemSys.getCore().powerCounters.incrementLsqLoadDataAccess(1);

		}
		//Otherwise the cache access is done through LSQValidateEvent
		
		return couldForward;
	}

	protected boolean loadResolve(int index, LSQEntry entry)
	{
		int tmpIndex;
		int ctr = 2;
		
		if (entry.getIndexInQ() == head)
			return false;
		else
			tmpIndex = decrementQ(index);

		while(true)
		{
			if(ctr > curSize)
			{
				break;
			}
			ctr++;
			
			LSQEntry tmpEntry = lsqueue[tmpIndex];
			if (tmpEntry.getType() == LSQEntry.LSQEntryType.STORE)
			{
				if (tmpEntry.isValid())
				{
					if (tmpEntry.getAddr() == entry.getAddr())
					{
						//TODO Test check
						if (!entry.isValid())
							System.err.println(" 01 Invalid entry forwarded");
						
						// Successfully forwarded the value
						entry.setForwarded(true);
						if (entry.getRobEntry() != null && !entry.getRobEntry().getExecuted())
							((OutOrderCoreMemorySystem)containingMemSys).sendExecComplete(entry.getRobEntry());
						return true;
					}
				}
				else
					break;
			}
			tmpIndex = decrementQ(tmpIndex);
		}
		return false;
	}

	public void storeValidate(LSQEntry entry)
	{
		//Test check
		if (lsqueue[entry.getIndexInQ()] != entry)
			System.err.println(" Entry index and actual entry dont match : STORE" + entry.getIndexInQ());
		
		entry.setValid(true);
		storeResolve(entry.getIndexInQ(), entry);
	}

	protected void storeResolve(int index, LSQEntry entry)
	{
		int sindex = incrementQ(index);
		int ctr = 2;
		
		while (true)
		{
			LSQEntry tmpEntry = lsqueue[sindex];
			
			if(ctr > curSize)
			{
				break;
			}
			ctr++;
			
			if (tmpEntry.getType() == LSQEntry.LSQEntryType.LOAD)
			{
				if(tmpEntry.getAddr() == entry.getAddr()) 
				{
					if (tmpEntry.isValid() && !tmpEntry.isForwarded())
					{
						//TODO Test check
						if (!tmpEntry.isValid())
							System.err.println(" 02 Invalid entry forwarded");
						
						tmpEntry.setForwarded(true);
						if (tmpEntry.getRobEntry() != null && !tmpEntry.getRobEntry().getExecuted())
							((OutOrderCoreMemorySystem)containingMemSys).sendExecComplete(tmpEntry.getRobEntry());
						
						NoOfForwards++;
					}
				}
			}
			else //It is a STORE
			{
				if((tmpEntry.getAddr() == entry.getAddr()) || !(tmpEntry.isValid()))
					break;
			}
			
			// increment
			sindex = incrementQ(sindex);
		}
	}

	
	//Only used by the statistical pipeline
	public void processROBCommitForStatisticalPipeline(EventQueue eventQueue)
	{
		while (curSize > 0 && ((lsqueue[head].getType() == LSQEntryType.STORE && lsqueue[head].isValid())||
				(lsqueue[head].getType() == LSQEntryType.LOAD && lsqueue[head].isForwarded() == true)))
		{
			LSQEntry entry = lsqueue[head];
			
			// if it is a store, send the request to the cache
			if(entry.getType() == LSQEntry.LSQEntryType.STORE) 
			{
				//TODO Write to the cache
				this.containingMemSys.l1Cache.getPort().put(
						new LSQEntryContainingEvent(
								eventQueue,
								this.containingMemSys.l1Cache.getLatencyDelay(),
								this,
								this.containingMemSys.l1Cache,
								RequestType.Cache_Write,
								entry,
								this.containingMemSys.coreID));
			}
//			else
//				Core.outstandingMemRequests--;
	
			if(head == tail)
			{
				head = tail = -1;
			}
			else
			{
				this.head = this.incrementQ(this.head);
			}
			this.curSize--;
//			System.out.println(curSize);
			//containingMemSys.core.getExecEngine().outstandingMemRequests--;
		}
//		return false;
	}

	
	protected int incrementQ(int value)
	{
		value = (value+1)%lsqSize;
		return value;
	}
	protected int decrementQ(int value)
	{
		if (value > 0)
			value--;
		else if (value == 0)
			value = lsqSize - 1;
		return value;
	}
	
	public boolean isEmpty()
	{
		if (curSize == 0)
			return true;
		else 
			return false;
	}
	
	public boolean isFull()
	{
		if (curSize >= lsqSize)
			return true;
		else 
			return false;
	}

	public int getLsqSize() {
		return lsqSize;
	}
	
	public void setRemoved(int index)
	{
		lsqueue[index].setRemoved(true);
	}
	
	public void handleEvent(EventQueue eventQ, Event event)
	{
		if (event.getRequestType() == RequestType.Tell_LSQ_Addr_Ready)
		{
			handleAddressReady(eventQ, event);
		}
		else if (event.getRequestType() == RequestType.Validate_LSQ_Addr)
		{
			handleAddrValidate(eventQ, event);
		}
		else if (event.getRequestType() == RequestType.LSQ_Commit)
		{
			handleCommitsFromROB(eventQ, event);
		}
		else if (event.getRequestType() == RequestType.Attempt_L1_Issue)
		{
			handleAttemptL1Issue(event);
		}
	}
	
	public void handleAddressReady(EventQueue eventQ, Event event)
	{
		LSQEntry lsqEntry = ((LSQEntryContainingEvent)(event)).getLsqEntry();
		long virtualAddr = lsqEntry.getAddr();
		
//		if(lsqEntry.getRobEntry().getIssued() == false)
//		{
//			System.out.println("validating a load/store that hasn't been issued");
//		}
		
		if (this.containingMemSys.TLBuffer.searchTLBForPhyAddr(virtualAddr))
		{
			this.handleAddrValidate(eventQ, event);
		}
		else
		{
			//Fetch the physical address from from Page table
			//Now, we directly check TLB as a function and schedule a validate event 
			// assuming a constant delay equal to TLB miss penalty
			this.getPort().put(
					event.update(
							eventQ,
							this.containingMemSys.TLBuffer.getMemoryPenalty(),
							null,
							this,
							RequestType.Validate_LSQ_Addr));
		}
	}
	
	public void handleAddrValidate(EventQueue eventQ, Event event)
	{
		LSQEntry lsqEntry = ((LSQEntryContainingEvent)(event)).getLsqEntry();
		
		//If the LSQ entry is a load
		if (lsqEntry.getType() == LSQEntryType.LOAD)
		{
			
			if (!(this.loadValidate(lsqEntry)))
			{
				handleAttemptL1Issue(event);
			}
		}
		else //If the LSQ entry is a store
		{
			this.storeValidate(lsqEntry);
		}
	}
	
	public void handleAttemptL1Issue(Event event)
	{
		LSQEntry lsqEntry = ((LSQEntryContainingEvent)(event)).getLsqEntry();
		
		if(lsqEntry.isForwarded() == true)
		{
			// its possible that while waiting for the L1 cache to get free,
			//this LSQ entry gets its value through forwarding
			//as a result of an earlier store getting its address validated
			return;
		}
		
		boolean requestIssued = this.containingMemSys.issueRequestToL1Cache(RequestType.Cache_Read, lsqEntry.getAddr());
		
		if(requestIssued == false)
		{
			event.addEventTime(1);
			event.setRequestType(RequestType.Attempt_L1_Issue);
			event.getEventQ().addEvent(event);
		}
		else
		{
			lsqEntry.setIssued(true);
		}
	}
	
	public void handleMemResponse(long address)
	{
		LSQEntry lsqEntry = null;
		
		int index = head;
		for(int i = 0; i < curSize; i++)
		{
			lsqEntry = lsqueue[index];
			
			if ((lsqEntry.getType() == LSQEntryType.LOAD) &&
					!lsqEntry.isRemoved() &&
					!lsqEntry.isForwarded() &&
					lsqEntry.getAddr() == address)
			{
				
				//TODO Test check
				if (!lsqEntry.isValid())
				{
					index = (index+1)%lsqSize;
					continue;
				}
				
				lsqEntry.setForwarded(true);
				
				if (lsqEntry.getRobEntry() != null && !lsqEntry.getRobEntry().getExecuted()
						&& !containingMemSys.core.isPipelineStatistical)	
					((OutOrderCoreMemorySystem)containingMemSys).sendExecComplete(lsqEntry.getRobEntry());
			}/*
			else if (receivingLSQ.lsqueue[lsqIndex].getType() == LSQEntryType.STORE)
			{
				receivingLSQ.lsqueue[lsqIndex].setStoreCommitted(true);
				//TODO : Also to increment the head of the queue. Following code is from the LSQCommitEventFromROB
				/*
				processingLSQ.head = processingLSQ.incrementQ(processingLSQ.head);
				processingLSQ.curSize--;
				//long address = entry.getAddr();
				*
				//TODO : Commit the STORE entry in the LSQ and may be generate an event 
				//to tell the ROB or something
			}*/
			
			index = (index+1)%lsqSize;
		}	
	}
	
	public void handleCommitsFromROB(EventQueue eventQ, Event event)
	{
		LSQEntry lsqEntry = ((LSQEntryContainingEvent)(event)).getLsqEntry();
		
		/*
		 * a wide OOO pipeline may send multiple commits to the LSQ in one cycle;
		 * these commits need not come in the same order as they were inserted;
		 * (this is due to the priority queue implementation in the event queue)
		 * to handle this, all memory operations from 'head' to the one in the event are committed
		 */
		
		int commitUpto = lsqEntry.getIndexInQ();
		
		if(lsqueue[commitUpto].isRemoved() == true)
		{
			return;
		}
		
		int i = head;
		
		for(; ; i = (i+1)%lsqSize)
		{
			LSQEntry tmpEntry = lsqueue[i];
			
			// if it is a store, send the request to the cache
			if(tmpEntry.getType() == LSQEntry.LSQEntryType.STORE) 
			{
				if(tmpEntry.isValid() == false)
				{
					System.err.println("store not ready to be committed");
				}
				
				boolean requestIssued = containingMemSys.issueRequestToL1Cache(RequestType.Cache_Write, tmpEntry.getAddr());
				
				if(requestIssued == false)
				{
					event.addEventTime(1);
					event.getEventQ().addEvent(event);
					break; //removals must be in-order : if u can't commit the operation at the head, u can't commit the ones that follow it
				}

				else
				{
					if(head == tail)
					{
						head = tail = -1;
					}
					else
					{
						this.head = this.incrementQ(this.head);
					}
					this.curSize--;
					tmpEntry.setRemoved(true);
				}
			}
			
			//If it is a LOAD which has received its value
			else if (tmpEntry.isForwarded())
			{
				if(head == tail)
				{
					head = tail = -1;
				}
				else
				{
					this.head = this.incrementQ(this.head);
				}
				this.curSize--;
				tmpEntry.setRemoved(true);
			}
			
			//If it is a LOAD which has not yet received its value
			else
			{
				System.err.println("Error in LSQ " +this.containingMemSys.coreID+ " :  ROB sent commit for a load which has not received its value");
				System.out.println(tmpEntry.getIndexInQ() + " : load : " + tmpEntry.getAddr());
				System.exit(1);
			}
			
			if(i == commitUpto)
			{
				break;
			}
		}
	}
}