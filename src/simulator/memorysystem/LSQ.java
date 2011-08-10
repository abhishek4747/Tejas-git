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

import pipeline.outoforder.ReorderBufferEntry;

import generic.*;

public class LSQ extends SimulationElement
{
	CoreMemorySystem containingMemSys;
	protected LSQEntry[] lsqueue;
	protected int tail = 0; // You can start adding at the tail index
	protected int head = 0;	// Instructions retire at the head
	protected int lsqSize;
	protected int curSize;
	//public static final int QUEUE_FULL = -1;
		
	public int NoOfLd = 0; //Total number of load instructions encountered
	public int NoOfForwards = 0; // Total number of forwards made by the LSQ
	
	//For telling that what addresses are processed this cycle (for BANKED multi-port option)
	protected ArrayList<Integer> banksAccessedThisCycle = new ArrayList<Integer>();
	
	//For telling how many requests are processed this cycle (for GENUINELY multi-ported option)
	protected int requestsProcessedThisCycle = 0;
	
	public static final int INVALID_INDEX = -1;

	public LSQ(int noOfPorts, Time_t occupancy, Time_t latency,
			CoreMemorySystem containingMemSys, int lsqSize) 
	{
		super(noOfPorts, occupancy, latency);
		this.containingMemSys = containingMemSys;
		this.lsqSize = lsqSize;
		curSize = 0;
		lsqueue = new LSQEntry[lsqSize];	
	}

	public int addEntry(boolean isLoad, long address, ReorderBufferEntry robEntry) //To be accessed at the time of allocating the entry
	{
		//if (curSize < lsqSize)
		//{
			LSQEntry.LSQEntryType type = (isLoad) ? LSQEntry.LSQEntryType.LOAD 
					: LSQEntry.LSQEntryType.STORE;
			LSQEntry entry = new LSQEntry(type, robEntry);
			int index = tail;
			entry.setAddr(address);
			lsqueue[index] = entry;
			tail = incrementQ(tail);
			this.curSize++;
			return index;
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
		int tmpIndex = decrementQ(index);

		while(true)
		{
			LSQEntry tmpEntry = lsqueue[tmpIndex];
			if (tmpEntry.getType() == LSQEntry.LSQEntryType.STORE)
			{
				if (tmpEntry.getAddr() == entry.getAddr())
				{
					if (tmpEntry.isValid())
					{
						// Successfully forwarded the value
						entry.setForwarded(true);
						containingMemSys.core.getEventQueue().addEvent(new ExecutionCompleteEvent(entry.getRobEntry(),
										-1,
										containingMemSys.core,
										GlobalClock.getCurrentTime()));
						return true;
					}
					else
						break;
				}
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
		storeResolve(index, entry);
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
					if (tmpEntry.isValid())
					{
						tmpEntry.setForwarded(true);
						containingMemSys.core.getEventQueue().addEvent(
								new ExecutionCompleteEvent(tmpEntry.getRobEntry(),
										-1,
										containingMemSys.core,
										GlobalClock.getCurrentTime()));
						NoOfForwards++;
					}
				}
			}
			else //It is a STORE
			{
				if((tmpEntry.getAddr() == entry.getAddr()) || !(tmpEntry.isValid()))
					return;
			}
			
			// increment
			sindex = incrementQ(sindex);
		}
	}
/*
	public void processROBCommit(int index)
	{
		Went into LSQCommitEventFromROB
	}
*/
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
	
	/**
	 * Tells whether the request of current event can be processed in the current cycle (due to device port availability)
	 * @param index : The LSQ index to be accessed
	 * @return A boolean value :TRUE if the request can be processed and FALSE otherwise
	 */
/*	protected boolean canServiceRequest(int index)
	{
		//For Genuinely multi-ported elements, if number of requests this cycle has not reached the total number of ports
		if  ((this.getMultiPortType() == MultiPortingType.GENUINE) && (this.requestsProcessedThisCycle < this.ports))
		{
			requestsProcessedThisCycle++;
			return true;
		}
		
		//For Banked multi-ported elements
		else if ((this.getMultiPortType() == MultiPortingType.BANKED) && 
				!/*NOT*this.banksAccessedThisCycle.contains(index/(lsqSize/(this.ports))))
		{
			banksAccessedThisCycle.add(index/(lsqSize/(this.ports)));
			return true;
		}
		else
			return false;
	}*/
}