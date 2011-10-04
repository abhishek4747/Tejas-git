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

import pipeline.outoforder.ReorderBufferEntry;

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
	
	//For telling that what addresses are processed this cycle (for BANKED multi-port option)
	protected ArrayList<Integer> banksAccessedThisCycle = new ArrayList<Integer>();
	
	//For telling how many requests are processed this cycle (for GENUINELY multi-ported option)
	protected int requestsProcessedThisCycle = 0;
	
	public static final int INVALID_INDEX = -1;

	public LSQ(PortType portType, int noOfPorts, Time_t occupancy, EventQueue eventQueue,
			Time_t latency, CoreMemorySystem containingMemSys, int lsqSize) 
	{
		super(portType, noOfPorts, occupancy, latency, containingMemSys.core.getFrequency());
		this.containingMemSys = containingMemSys;
		this.lsqSize = lsqSize;
		curSize = 0;
		lsqueue = new LSQEntry[lsqSize];	
	}

	public LSQEntry addEntry(boolean isLoad, long address, ReorderBufferEntry robEntry) //To be accessed at the time of allocating the entry
	{
		//if (curSize < lsqSize)
		//{
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
//			System.out.println(curSize);
			return entry;
		//}
		//else return QUEUE_FULL; // -1 signifies that the queue is full
	}

	public boolean loadValidate(int index, long address)
	{
		LSQEntry entry = lsqueue[index];
		entry.setValid(true);
		boolean couldForward = loadResolve(index, entry);
		if(couldForward) 
		{
			NoOfForwards++;
		}
		//Otherwise the cache access is done through LSQValidateEvent
		
		return couldForward;
	}

	protected boolean loadResolve(int index, LSQEntry entry)
	{
		int tmpIndex;
		
		if (index == head)
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
						// Successfully forwarded the value
						entry.setForwarded(true);
						if (entry.getRobEntry() != null && !entry.getRobEntry().getExecuted())
							containingMemSys.core.getEventQueue().addEvent(new ExecutionCompleteEvent(entry.getRobEntry(),
											-1,
											containingMemSys.core,
											GlobalClock.getCurrentTime()));
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

	public void storeValidate(int index, long address)
	{
		LSQEntry entry = lsqueue[index];
		entry.setValid(true);
		//storeResolve(index, entry);
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
						tmpEntry.setForwarded(true);
						if (tmpEntry.getRobEntry() != null && !tmpEntry.getRobEntry().getExecuted())
							containingMemSys.core.getEventQueue().addEvent(
								new ExecutionCompleteEvent(tmpEntry.getRobEntry(),
										-1,
										containingMemSys.core,
										GlobalClock.getCurrentTime()));
						
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

	
	//Only used by the perfect pipeline
	public boolean processROBCommitForPerfectPipeline(EventQueue eventQueue)
	{
		if (!(lsqueue[head].getType() == LSQEntryType.STORE ||
				(lsqueue[head].getType() == LSQEntryType.LOAD && lsqueue[head].isForwarded() == true)))
			return true;
		
		while ((lsqueue[head].getType() == LSQEntryType.STORE ||
				(lsqueue[head].getType() == LSQEntryType.LOAD && lsqueue[head].isForwarded() == true)) &&
				curSize > 0)
		{
			LSQEntry entry = lsqueue[head];
			
			// if it is a store, send the request to the cache
			if(entry.getType() == LSQEntry.LSQEntryType.STORE) 
			{
				//TODO Write to the cache
				CacheRequestPacket request = new CacheRequestPacket();
				//request.setThreadID(0);
				request.setType(RequestType.MEM_WRITE);
				request.setAddr(entry.getAddr());
				this.containingMemSys.l1Cache.getPort().put(new NewCacheAccessEvent(this.containingMemSys.l1Cache.getLatencyDelay(),
																this,
																this.containingMemSys.l1Cache,
																entry, 
																0, //tieBreaker,
																request));
			}
			else
				Core.outstandingMemRequests--;
	
			this.head = this.incrementQ(this.head);
			this.curSize--;
//			System.out.println(curSize);
			//containingMemSys.core.getExecEngine().outstandingMemRequests--;
		}
		return false;
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
}