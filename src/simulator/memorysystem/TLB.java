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

	Contributors:  Moksh Upadhyay
*****************************************************************************/
package memorysystem;

import java.util.*;

import generic.*;

public class TLB extends SimulationElement
{
	CoreMemorySystem containingMemSys;
	protected Hashtable<Long, TLBEntry> TLBuffer;
	protected int TLBSize; //Number of entries
	protected double timestamp;
	protected int tlbHits = 0;
	protected int tlbMisses = 0;
	
	//Outstanding Request Table : Stores pageID vs LSQEntryIndex
	protected Hashtable<Long, ArrayList<Integer>> outstandingRequestTable;
	
	//For telling that what addresses are processed this cycle (for BANKED multi-port option)
	protected ArrayList<Long> addressesProcessedThisCycle = new ArrayList<Long>();
	
	//For telling how many requests are processed this cycle (for GENUINELY multi-ported option)
	protected int requestsProcessedThisCycle = 0;
	
	public TLB(int noOfPorts, Time_t occupancy, Time_t latency, CoreMemorySystem containingMemSys, int tlbSize) 
	{
		super(noOfPorts, occupancy, latency);
		
		TLBSize = tlbSize;
		this.timestamp = 0;
		TLBuffer = new Hashtable<Long, TLBEntry>(TLBSize);
	}
	
	/**
	 * Removes the page offset bits from the address
	 * @param virtualAddr : Complete virtual address
	 * @return pageID obtained by removing page offset bits from virtual address
	 */
	protected static long getPageID(long virtualAddr)
	{
		long pageID = virtualAddr >> Global.PAGE_OFFSET_BITS;
		return pageID;
	}
	
	public boolean searchTLBForPhyAddr(long virtualAddr) //Returns whether the address was already in the TLB or not
	{
		timestamp += 1.0; //Increment the timestamp to be set in this search
		boolean isEntryFoundInTLB;
		
		long pageID = getPageID(virtualAddr); //Remove the page offset bits from the address
		TLBEntry entry;
		
		if ((TLBuffer.isEmpty()) || ((entry = TLBuffer.get(pageID)) == null)) //Entry not found in the TLB
		{
			tlbMisses++;
			//Fetch the TLB entry from Main memory through the event TLBAddrSearchEvent
			//addTLBEntry(pageID);
			//return pageID;
			isEntryFoundInTLB = false;
		}
		else //Entry found in the page table
		{
			tlbHits++;
			entry.setTimestamp(timestamp);
			//return entry.getPhyAddr();
			isEntryFoundInTLB = true;
		}
		return isEntryFoundInTLB;
	}
	
	//Just have to provide the full address.
	//The pageID is calculated within
	protected void addTLBEntry(long pageID)
	{
		long addressForTLB = pageID << Global.PAGE_OFFSET_BITS;
		
		TLBEntry entry = new TLBEntry();
		entry.setPhyAddr(addressForTLB);
		entry.setTimestamp(timestamp);
		
		if (!(TLBuffer.size() < TLBSize)) // If there is no space in the TLB
		{
			long keyToRemove = searchOldestTimestamp(); //We use LRU replacement
			TLBuffer.remove(keyToRemove);
		}
		TLBuffer.put(pageID, entry);
	}
	
	
	private long searchOldestTimestamp()
	{
		long oldestAddr = 0;
		int minTimestamp = 2000000000; //Ultra-high value
		for (Enumeration<TLBEntry> entriesEnum = TLBuffer.elements(); entriesEnum.hasMoreElements(); )
		{
			TLBEntry entry = entriesEnum.nextElement();
			if (entry.getTimestamp() < minTimestamp)
			{
				oldestAddr = entry.getPhyAddr();
			}
		}
		return (getPageID(oldestAddr));
	}
	
	/**
	 * Tells whether the request of current event can be processed in the current cycle (due to device port availability)
	 * @return A boolean value :TRUE if the request can be processed and FALSE otherwise
	 */
/*	protected boolean canServiceRequest()
	{
		//TLB is a Genuinely multi-ported element
		//So if number of requests this cycle has not reached the total number of ports
		if (this.requestsProcessedThisCycle < this.ports)
		{
			requestsProcessedThisCycle++;
			return true;
		}
		else
			return false;
	}*/
	
	/**
	 * Used when a new request is made to a cache and there is a miss.
	 * This adds the request to the outstanding requests buffer of the cache
	 * @param pageID : pageID requested
	 * @return Whether the entry was already there or not
	 */
	protected boolean addOutstandingRequest(long pageID, int index)
	{
		boolean entryAlreadyThere;
		
		if (!/*NOT*/outstandingRequestTable.containsKey(pageID))
		{
			entryAlreadyThere = false;
			outstandingRequestTable.put(pageID, new ArrayList<Integer>());
		}
		else
			entryAlreadyThere = true;
		
		outstandingRequestTable.get(pageID).add(index);
		
		return entryAlreadyThere;
	}
}
