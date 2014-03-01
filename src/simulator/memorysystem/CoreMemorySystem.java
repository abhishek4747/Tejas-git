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

import generic.CachePullEvent;
import generic.PortType;
import generic.SimulationElement;
import generic.Core;
import generic.RequestType;
import config.SystemConfig;
import config.CacheConfig;

public abstract class CoreMemorySystem extends SimulationElement
{
	protected int coreID;
	protected Core core;
	protected Cache iCache;
	protected Cache l1Cache;
	protected TLB iTLB;
	protected TLB dTLB;
	protected LSQ lsqueue;
	
	protected long numInstructionSetChunksNoted = 0;
	protected long numDataSetChunksNoted = 0;
	
	protected CoreMemorySystem(Core core)
	{
		super(PortType.Unlimited, -1, -1, core.getEventQueue(), -1, -1);
		
		this.setCore(core);
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
		iCache = new Cache(cacheParameterObj, this);
		//add initial cachepull event
		this.core.getEventQueue().addEvent(
									new CachePullEvent(
											this.core.getEventQueue(),
											0,
											iCache,
											iCache,
											RequestType.PerformPulls,
											this.coreID));
		
		//Initialise the  L1 cache
		cacheParameterObj = SystemConfig.core[coreID].l1Cache;
		l1Cache = new Cache(cacheParameterObj, this);
		//add initial cachepull event
		this.core.getEventQueue().addEvent(
				new CachePullEvent(
						this.core.getEventQueue(),
						0,
						l1Cache,
						l1Cache,
						RequestType.PerformPulls,
						this.coreID));
		
		//Initialise the TLB
		int numPageLevels = 2;
		iTLB = new TLB(SystemConfig.core[coreID].ITLBPortType,
							SystemConfig.core[coreID].ITLBAccessPorts, 
							SystemConfig.core[coreID].ITLBPortOccupancy, 
							SystemConfig.core[coreID].ITLBLatency,
							this,
							SystemConfig.core[coreID].ITLBSize,
							SystemConfig.mainMemoryLatency * numPageLevels,
							SystemConfig.core[coreID].iTLBPower);
		
		dTLB = new TLB(SystemConfig.core[coreID].DTLBPortType,
				SystemConfig.core[coreID].DTLBAccessPorts, 
				SystemConfig.core[coreID].DTLBPortOccupancy, 
				SystemConfig.core[coreID].DTLBLatency,
				this,
				SystemConfig.core[coreID].DTLBSize,
				SystemConfig.mainMemoryLatency * numPageLevels,
				SystemConfig.core[coreID].dTLBPower);
		
		//Initialise the LSQ
		lsqueue = new LSQ(SystemConfig.core[coreID].LSQPortType,
		                    SystemConfig.core[coreID].LSQAccessPorts, 
							SystemConfig.core[coreID].LSQPortOccupancy, 
							SystemConfig.core[coreID].LSQLatency,
							this, 
							SystemConfig.core[coreID].LSQSize);
	//	lsqueue.setMultiPortType(SystemConfig.core[coreID].LSQMultiportType);
	}
	
	public abstract void issueRequestToInstrCache(long address);
	
	public abstract boolean issueRequestToL1Cache(RequestType requestType, long address);
	
	public LSQ getLsqueue() {
		return lsqueue;
	}
	
	public Cache getL1Cache() {
		return l1Cache;
	}

	public TLB getiTLB() {
		return iTLB;
	}
	
	public TLB getdTLB() {
		return dTLB;
	}

	public Cache getiCache() {
		return iCache;
	}

	public void setCore(Core core) {
		this.core = core;
	}

	public Core getCore() {
		return core;
	}
}
