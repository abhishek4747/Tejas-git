/*****************************************************************************
				BhartiSim Simulator
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

import java.util.ArrayList;

import memorysystem.LSQEntry.LSQEntryType;

import pipeline.outoforder_new_arch.OpTypeToFUTypeMapping;
import pipeline.outoforder_new_arch.ReorderBufferEntry;
import pipeline.statistical.DelayGenerator;

import generic.*;

public class LSQ extends SimulationElement
{
	CoreMemorySystem containingMemSys;
	protected LSQEntry[] lsqueue;
	protected int tail = 0; // You can start adding at the tail index
	protected int head = 0;	// Instructions retire at the head
	public int lsqSize;
	protected int curSize;
		
	public int noOfMemRequests = 0;
	public int NoOfLd = 0; //Total number of load instructions encountered
	public int NoOfSt = 0;
	public int NoOfForwards = 0; // Total number of forwards made by the LSQ
	
	public static final int INVALID_INDEX = -1;

	public LSQ(PortType portType, int noOfPorts, long occupancy, long latency, CoreMemorySystem containingMemSys, int lsqSize) 
	{
		super(portType, noOfPorts, occupancy, latency, containingMemSys.core.getFrequency());
		this.containingMemSys = containingMemSys;
		this.lsqSize = lsqSize;
		curSize = 0;
		lsqueue = new LSQEntry[lsqSize];	
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
		
		LSQEntry entry = new LSQEntry(type, robEntry);
		int index = tail;
		entry.setAddr(address);
		entry.setIndexInQ(index);
		lsqueue[index] = entry;
		tail = incrementQ(tail);
		this.curSize++;
//		System.out.println(curSize);
		return entry;
	}

	public boolean loadValidate(LSQEntry entry/*int index*/, Event event)//, long address)
	{
//		LSQEntry entry = lsqueue[index];
		
		//Test check
		if (lsqueue[entry.getIndexInQ()] != entry)
			System.out.println(" Entry index and actual entry dont match : LOAD" + entry.getIndexInQ());
		
		entry.setValid(true);
		boolean couldForward = loadResolve(entry.getIndexInQ(), entry, event);
		if(couldForward) 
		{
			NoOfForwards++;
		}
		//Otherwise the cache access is done through LSQValidateEvent
		
		return couldForward;
	}

	protected boolean loadResolve(int index, LSQEntry entry, Event event)
	{
		int tmpIndex;
		
		if (entry.getIndexInQ() == head)
			return false;
		else
			tmpIndex = decrementQ(index);

		while(true)
		{
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
							sendExecComplete(entry.getRobEntry());
						//For perfect pipeline
//						else if (entry.getRobEntry() == null)
//						{
//							Core.outstandingMemRequests--;
//						}
						return true;
					}
				}
				else
					break;
			}
			if(tmpIndex == head)
				break;
			tmpIndex = decrementQ(tmpIndex);
		}
		return false;
	}

	public void storeValidate(LSQEntry entry/*int index*/)//, long address)
	{
//		LSQEntry entry = lsqueue[index];

		//Test check
		if (lsqueue[entry.getIndexInQ()] != entry)
			System.out.println(" Entry index and actual entry dont match : STORE" + entry.getIndexInQ());
		
		entry.setValid(true);
		storeResolve(entry.getIndexInQ(), entry);
	}

	protected void storeResolve(int index, LSQEntry entry)
	{
		int sindex = incrementQ(index);
		while (sindex != tail) //Alright
		{
			LSQEntry tmpEntry = lsqueue[sindex];
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
							sendExecComplete(tmpEntry.getRobEntry());
						
						//For perfect pipeline
//						else if (tmpEntry.getRobEntry() == null)
//						{
//							Core.outstandingMemRequests--;
//						}
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
//		if (!(lsqueue[head].getType() == LSQEntryType.STORE ||
//				(lsqueue[head].getType() == LSQEntryType.LOAD && lsqueue[head].isForwarded() == true)))
//			return true;
		
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
								entry));
			}
			else
				Core.outstandingMemRequests--;
	
			this.head = this.incrementQ(this.head);
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
		else if (event.getRequestType() == RequestType.Mem_Response)
		{
			handleMemResponse(eventQ, event);
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
			//TODO Now, we directly check TLB as a function and schedule a validate event 
			// assuming a constant delay equal to Main memory latency
			this.getPort().put(
					event.update(
							eventQ,
							MemorySystem.mainMemory.getLatencyDelay(),
							null,
							this,
							RequestType.Validate_LSQ_Addr));
		}
	}
	
	public void handleAddrValidate(EventQueue eventQ, Event event)
	{
		LSQEntry lsqEntry = ((LSQEntryContainingEvent)(event)).getLsqEntry();
		
//		if(lsqEntry.getRobEntry().getIssued() == false)
//		{
//			System.out.println("validating a load/store that hasn't been issued");
//		}
		
		//If the LSQ entry is a load
		if (lsqEntry.getType() == LSQEntryType.LOAD)
		{
			//If the value could not be forwarded
			if (!(this.loadValidate(lsqEntry/*.getIndexInQ()*/, event)))
			{
				//TODO Read from the cache (CacheAccessEvent)
				//Direct address must not be set as it is pageID in some cases
				this.containingMemSys.l1Cache.getPort().put(
						event.update(
								eventQ,
								this.containingMemSys.l1Cache.getLatencyDelay(),
								this,
								this.containingMemSys.l1Cache,
								RequestType.Cache_Read));
			}
		}
		else //If the LSQ entry is a store
		{
			this.storeValidate(lsqEntry);
		}
	}
	
	protected void handleMemResponse(EventQueue eventQ, Event event)
	{
		LSQEntry lsqEntry = ((LSQEntryContainingEvent)(event)).getLsqEntry();
		
		if ((lsqEntry.getType() == LSQEntryType.LOAD) &&
				!lsqEntry.isRemoved() &&
				!lsqEntry.isForwarded())
		{
			
			//TODO Test check
			if (!lsqEntry.isValid())
				System.err.println(" 03 Invalid entry forwarded");
			
			lsqEntry.setForwarded(true);
			
			if (lsqEntry.getRobEntry() != null && !lsqEntry.getRobEntry().getExecuted())	
				sendExecComplete(lsqEntry.getRobEntry());
			
//			//For perfect pipeline
//			else if (lsqEntry.getRobEntry() == null)
//			{
//				Core.outstandingMemRequests--;
//			}
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
			
	}
	
	public void handleCommitsFromROB(EventQueue eventQ, Event event)
	{
		LSQEntry lsqEntry = ((LSQEntryContainingEvent)(event)).getLsqEntry();
		
		//Check the error condition
//		if (lsqEntry.getIndexInQ() != this.head)
//		{
//			System.err.println("Error in LSQ :  ROB sent commit for an instruction other than the one at the head");
//			System.exit(1);
//		}
		
		// advance the head of the queue
		
		// if it is a store, send the request to the cache
		if(lsqEntry.getType() == LSQEntry.LSQEntryType.STORE) 
		{
			//TODO Write to the cache
			this.containingMemSys.l1Cache.getPort().put(
					event.update(
							eventQ,
							this.containingMemSys.l1Cache.getLatencyDelay(),
							this,
							this.containingMemSys.l1Cache,
							RequestType.Cache_Write));
			
			this.head = this.incrementQ(this.head);
			this.curSize--;
		}
		
		//If it is a LOAD which has received its value
		else if (lsqEntry.isForwarded())
		{
			this.head = this.incrementQ(this.head);
			this.curSize--;
		}
		
		//If it is a LOAD which has not yet received its value
		else
		{
			System.err.println("Error in LSQ " +this.containingMemSys.coreID+ " :  ROB sent commit for a load which has not received its value");
			System.exit(1);
		}
	}
	
	public void sendExecComplete(ReorderBufferEntry robEntry)
	{
		if (!containingMemSys.core.isPipelineStatistical)
			containingMemSys.core.getEventQueue().addEvent(
					new ExecCompleteEvent(
							containingMemSys.core.getEventQueue(),
							GlobalClock.getCurrentTime(),
							null,
							containingMemSys.core.getExecEngine().getExecuter(),
							RequestType.EXEC_COMPLETE,
							robEntry));
		else
			DelayGenerator.insCountOut++;
	}
	
}