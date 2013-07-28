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

import java.util.ArrayList;
import java.util.Hashtable;

import pipeline.inorder.FetchUnitIn;
import pipeline.inorder.MemUnitIn;
import pipeline.outoforder.FetchLogic;
import pipeline.outoforder.ReorderBufferEntry;
import generic.CachePullEvent;
import generic.Event;
import generic.OMREntry;
import generic.PortType;
import generic.SimulationElement;
import generic.EventQueue;
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
	protected TLB TLBuffer;
	protected LSQ lsqueue;
	protected MissStatusHoldingRegister L1MissStatusHoldingRegister;
	protected MissStatusHoldingRegister iMissStatusHoldingRegister;
	
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
		TLBuffer = new TLB(SystemConfig.core[coreID].TLBPortType,
							SystemConfig.core[coreID].TLBAccessPorts, 
							SystemConfig.core[coreID].TLBPortOccupancy, 
							SystemConfig.core[coreID].TLBLatency,
							this,
							SystemConfig.core[coreID].TLBSize,
							SystemConfig.mainMemoryLatency * numPageLevels);
		
		//Initialise the LSQ
		lsqueue = new LSQ(SystemConfig.core[coreID].LSQPortType,
		                    SystemConfig.core[coreID].LSQAccessPorts, 
							SystemConfig.core[coreID].LSQPortOccupancy, 
							SystemConfig.core[coreID].LSQLatency,
							this, 
							SystemConfig.core[coreID].LSQSize);
	//	lsqueue.setMultiPortType(SystemConfig.core[coreID].LSQMultiportType);
	//	L1MissStatusHoldingRegister = new MissStatusHoldingRegister(0, cacheParameterObj.mshrSize);
	//	iMissStatusHoldingRegister = new MissStatusHoldingRegister(0, cacheParameterObj.mshrSize);
		L1MissStatusHoldingRegister = new Mode1MSHR(SystemConfig.core[coreID].LSQSize);
		iMissStatusHoldingRegister = new Mode1MSHR(SystemConfig.core[coreID].LSQSize);
	}
	
	public abstract void issueRequestToInstrCache(long address);
	
	public abstract boolean issueRequestToL1Cache(RequestType requestType, long address);
	
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

	public void setCore(Core core) {
		this.core = core;
	}

	public Core getCore() {
		return core;
	}
	
	public MissStatusHoldingRegister getL1MSHR()
	{
		return L1MissStatusHoldingRegister;
	}
	
	public MissStatusHoldingRegister getiMSHR()
	{
		return iMissStatusHoldingRegister;
	}
	
	public boolean isMshrFull()
	{
		if(L1MissStatusHoldingRegister.isFull() && iMissStatusHoldingRegister.isFull())
		{
			return true;
		}
		return false;
	}
}
