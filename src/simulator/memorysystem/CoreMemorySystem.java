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
import generic.Event;
import generic.OMREntry;
import generic.SimulationElement;
import generic.EventQueue;
import generic.Core;
import generic.RequestType;
import config.SystemConfig;
import config.CacheConfig;

public class CoreMemorySystem 
{
	protected int coreID;
	private Core core;
	protected Cache iCache;
	protected Cache l1Cache;
	//protected Cache l2Cache;
	//protected Cache l3Cache;
	protected TLB TLBuffer;
	protected LSQ lsqueue;
	
	public CoreMemorySystem(Core core)
	{
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
		iCache = new InstructionCache(cacheParameterObj, this);
		
		//Initialise the  L1 cache
		cacheParameterObj = SystemConfig.core[coreID].l1Cache;
		l1Cache = new Cache(cacheParameterObj, this);
		
		//Initialise the TLB
		TLBuffer = new TLB(SystemConfig.core[coreID].TLBPortType,
							SystemConfig.core[coreID].TLBAccessPorts, 
							SystemConfig.core[coreID].TLBPortOccupancy, 
							SystemConfig.core[coreID].TLBLatency,
							this,
							SystemConfig.core[coreID].TLBSize);
		
		//Initialise the LSQ
		lsqueue = new LSQ(SystemConfig.core[coreID].LSQPortType,
		                    SystemConfig.core[coreID].LSQAccessPorts, 
							SystemConfig.core[coreID].LSQPortOccupancy, 
							SystemConfig.core[coreID].LSQLatency,
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
						getCore().getEventQueue(),
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
						getCore().getEventQueue(),
						lsqueue.getLatencyDelay(),
						null,
						lsqueue, 
						RequestType.LSQ_Commit, 
						robEntry.getLsqEntry()));
	}
	
	//To issue the request directly to L1 cache
	public void issueRequestToL1CacheFromInorder(SimulationElement requestingElement, 
											RequestType requestType, 
											long address,int coreId)
	{
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(getCore().getEventQueue(),
																	 l1Cache.getLatencyDelay(),
																	 requestingElement, 
																	 l1Cache,
																	 requestType, 
																	 address,
																	 coreId); 
		Hashtable<Long,OMREntry> missStatusHoldingRegister =((MemUnitIn)requestingElement).getMissStatusHoldingRegister();
		if(!missStatusHoldingRegister.containsKey(address))
		{
			ArrayList<Event> eventList = new ArrayList<Event>();
			eventList.add(addressEvent);
			missStatusHoldingRegister.put(address, new OMREntry(eventList,true,addressEvent));
			l1Cache.getPort().put(addressEvent);
		}
		else
		{
			missStatusHoldingRegister.get(address).outStandingEvents.add(addressEvent);
		}
	}
	
	public void issueRequestToL1CacheFromOutofOrder(SimulationElement requestingElement, 
			RequestType requestType, 
			long address,int coreId)
	{
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(getCore().getEventQueue(),
											 l1Cache.getLatencyDelay(),
											 requestingElement, 
											 l1Cache,
											 requestType, 
											 address,
											 coreId); 
		Hashtable<Long,OMREntry> missStatusHoldingRegister =((LSQ)requestingElement).getMissStatusHoldingRegister();
		if(!missStatusHoldingRegister.containsKey(address))
		{
			ArrayList<Event> eventList = new ArrayList<Event>();
			eventList.add(addressEvent);
			missStatusHoldingRegister.put(address, new OMREntry(eventList,true,addressEvent));
			l1Cache.getPort().put(addressEvent);
		}
		else
		{
			missStatusHoldingRegister.get(address).outStandingEvents.add(addressEvent);
		}
	}

	
	//To issue the request to instruction cache
	public void issueRequestToInstrCacheFromInorder(SimulationElement requestingElement,
											long address)
	{
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(getCore().getEventQueue(),
																	iCache.getLatencyDelay(),
																	requestingElement, 
																	iCache,
																	RequestType.Cache_Read, 
																	address);
		Hashtable<Long,OMREntry> missStatusHoldingRegister =((FetchUnitIn)requestingElement).getMissStatusHoldingRegister();
		missStatusHoldingRegister.put(address, new OMREntry(null,false,addressEvent));
		iCache.getPort().put(addressEvent);
	}
	public void issueRequestToInstrCacheFromOutofOrder(SimulationElement requestingElement,
			long address)
	{
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(getCore().getEventQueue(),
											iCache.getLatencyDelay(),
											requestingElement, 
											iCache,
											RequestType.Cache_Read, 
											address);
		
		Hashtable<Long,OMREntry> missStatusHoldingRegister =((FetchLogic)requestingElement).getMissStatusHoldingRegister();
		missStatusHoldingRegister.put(address, new OMREntry(null,false,addressEvent));
		iCache.getPort().put(addressEvent);
	}
	
	//To issue the request to instruction cache
	public void issueRequestToInstrCacheFromInorder(SimulationElement requestingElement,
											long address,int coreId)
	{
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(getCore().getEventQueue(),
																	iCache.getLatencyDelay(),
																	requestingElement, 
																	iCache,
																	RequestType.Cache_Read, 
																	address,
																	coreId);
		Hashtable<Long,OMREntry> missStatusHoldingRegister =((FetchUnitIn)requestingElement).getMissStatusHoldingRegister();
		if(!missStatusHoldingRegister.containsKey(address))
		{
			ArrayList<Event> eventList = new ArrayList<Event>();
			eventList.add(addressEvent);
			missStatusHoldingRegister.put(address,new OMREntry(eventList,true,addressEvent));
			iCache.getPort().put(addressEvent);
		}
		else
		{
			missStatusHoldingRegister.get(address).outStandingEvents.add(addressEvent);
		}
	}

	public void issueRequestToInstrCacheFromOutofOrder(SimulationElement requestingElement,
			long address,int coreId)
	{
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(getCore().getEventQueue(),
											iCache.getLatencyDelay(),
											requestingElement, 
											iCache,
											RequestType.Cache_Read, 
											address,
											coreId);
		Hashtable<Long,OMREntry> missStatusHoldingRegister =((FetchLogic)requestingElement).getMissStatusHoldingRegister();
		if(!missStatusHoldingRegister.containsKey(address))
		{
			ArrayList<Event> eventList = new ArrayList<Event>();
			eventList.add(addressEvent);
			missStatusHoldingRegister.put(address,new OMREntry(eventList,true,addressEvent));
			iCache.getPort().put(addressEvent);
		}
		else
		{
			missStatusHoldingRegister.get(address).outStandingEvents.add(addressEvent);
		}
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

	public void setCore(Core core) {
		this.core = core;
	}

	public Core getCore() {
		return core;
	}
}
