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

import generic.EventQueue;
import generic.Core;
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
/*	
	public void read(long addr)
	{
		//Allocate the LSQ entry
		MemEventQueue.eventQueue.add(new LSQAddEvent(this, 
																	true, 
																	addr, 
																	MemEventQueue.clock + lsqueue.getLatency()));
		
		//TLBuffer.getPhyAddrPage(addr);
		//MemEventQueue.eventQueue.add(new TLBAccessEvent(this, addr, 25));
		//CacheRequestPacket request = new CacheRequestPacket();
		//request.tid = coreID;
		//request.setType(CacheRequestPacket.readWrite.READ);
		//request.setAddr(addr);
		//l1Cache.processEntry(request);
	}
	
	public void write(long addr)
	{
		//Allocate the LSQ entry
		MemEventQueue.eventQueue.add(new LSQAddEvent(this, 
																	false, 
																	addr, 
																	MemEventQueue.clock + lsqueue.getLatency()));
		
		//TLBuffer.getPhyAddrPage(addr);
		//CacheRequestPacket request = new CacheRequestPacket();
		//request.tid = coreID;
		//request.setType(CacheRequestPacket.readWrite.WRITE);
		//request.setAddr(addr);
		//l1Cache.processEntry(request);
	}
	*/

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
