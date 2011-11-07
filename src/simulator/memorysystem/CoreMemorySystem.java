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

import pipeline.outoforder_new_arch.ReorderBufferEntry;
import generic.SimulationElement;
import generic.EventQueue;
import generic.Core;
import generic.RequestType;
import config.SystemConfig;
import config.CacheConfig;

public class CoreMemorySystem 
{
	protected int coreID;
	protected Core core;
	protected Cache iCache;
	protected Cache l1Cache;
	//protected Cache l2Cache;
	//protected Cache l3Cache;
	protected TLB TLBuffer;
	protected LSQ lsqueue;
	
	public CoreMemorySystem(Core core, EventQueue eventQ)
	{
		this.core = core;
		this.coreID = core.getCore_number();
		
		//Initialise the  L3 cache
		//CacheConfig cacheParameterObj;// = SystemConfig.core[coreID].l3Cache;
		//l3Cache = new Cache(cacheParameterObj, this); 
		
		//Initialise the  L2 cache
		//cacheParameterObj = SystemConfig.core[coreID].l2Cache;
		//l2Cache = new Cache(cacheParameterObj, this); 
		//l2Cache.nextLevel = l3Cache;
		
		//Initialise the  instruction cache
		CacheConfig cacheParameterObj;
		cacheParameterObj = SystemConfig.core[coreID].iCache;
		iCache = new InstructionCache(cacheParameterObj, eventQ);
		
		//Initialise the  L1 cache
		cacheParameterObj = SystemConfig.core[coreID].l1Cache;
		l1Cache = new Cache(cacheParameterObj, eventQ);
		
		//Initialise the TLB
		TLBuffer = new TLB(SystemConfig.core[coreID].TLBPortType,
							SystemConfig.core[coreID].TLBAccessPorts, 
							SystemConfig.core[coreID].TLBPortOccupancy, 
							SystemConfig.core[coreID].TLBLatency,
							eventQ,
							this,
							SystemConfig.core[coreID].TLBSize);
		
		//Initialise the LSQ
		lsqueue = new LSQ(SystemConfig.core[coreID].LSQPortType,
		                    SystemConfig.core[coreID].LSQAccessPorts, 
							SystemConfig.core[coreID].LSQPortOccupancy, 
							SystemConfig.core[coreID].LSQLatency,
							eventQ,
							this, 
							SystemConfig.core[coreID].LSQSize);
	//	lsqueue.setMultiPortType(SystemConfig.core[coreID].LSQMultiportType);
	}
	
	public void allocateLSQEntry(boolean isLoad, long address, ReorderBufferEntry robEntry)
	{
		if (!MemorySystem.bypassLSQ)
			robEntry.setLsqEntry(lsqueue.addEntry(isLoad, address, robEntry));
	}
	
	//To issue the request to LSQ
	public void issueRequestToLSQ(SimulationElement requestingElement, 
											ReorderBufferEntry robEntry)
	{
		if(robEntry.isOperand1Available() == false ||
						robEntry.isOperand2Available() == false ||
						robEntry.getAssociatedIWEntry() == null ||
						robEntry.getIssued() == false)
		{
			System.out.println("attempting to validate the address of a load/store that hasn't been issued");
		}
		
		lsqueue.getPort().put(
				new LSQEntryContainingEvent(
						core.getEventQueue(),
						lsqueue.getLatencyDelay(), 
						requestingElement, //Requesting Element
						lsqueue, 
						RequestType.Tell_LSQ_Addr_Ready,
						robEntry.getLsqEntry()));
	}
	
	//To commit Store in LSQ
	public void issueLSQStoreCommit(ReorderBufferEntry robEntry)
	{
		lsqueue.getPort().put(
				 new LSQEntryContainingEvent(
						core.getEventQueue(),
						lsqueue.getLatencyDelay(),
						null,
						lsqueue, 
						RequestType.LSQ_Commit, 
						robEntry.getLsqEntry()));
	}
	
	//To issue the request directly to L1 cache
	public void issueRequestToL1Cache(SimulationElement requestingElement, 
											RequestType requestType, 
											long address)
	{
		l1Cache.getPort().put(
				new AddressCarryingEvent(
						core.getEventQueue(),
						l1Cache.getLatencyDelay(),
						requestingElement, 
						l1Cache,
						requestType, 
						address));
	}
	
	//To issue the request to instruction cache
	public void issueRequestToInstrCache(SimulationElement requestingElement,
											long address)
	{
		iCache.getPort().put(
				new AddressCarryingEvent(
						core.getEventQueue(),
						iCache.getLatencyDelay(),
						requestingElement, 
						iCache,
						RequestType.Cache_Read, 
						address));
	}

	public LSQ getLsqueue() {
		return lsqueue;
	}
	
	public Cache getL1Cache() {
		return l1Cache;
	}

	public TLB getTLBuffer() {
		return TLBuffer;
	}

	public Cache getiCache() {
		return iCache;
	}
}
