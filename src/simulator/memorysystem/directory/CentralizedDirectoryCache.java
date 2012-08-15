package memorysystem.directory;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

import java.util.HashMap;

import config.CacheConfig;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CacheLine;
import memorysystem.CoreMemorySystem;
import memorysystem.MESI;
import memorysystem.MemorySystem;

public class CentralizedDirectoryCache extends Cache{

	private int numPresenceBits;
	private long invalidations;
	private long writebacks;
	private long dataForwards;
	Core[] cores;
	public boolean debug =false;
	
	
	private HashMap<Long, DirectoryEntry> directoryHashmap;
	public CentralizedDirectoryCache(CacheConfig cacheParameters,
			CoreMemorySystem containingMemSys, int numCores, Core[] coresArray) {
		super(cacheParameters, containingMemSys);
		directoryHashmap = new HashMap<Long, DirectoryEntry>();
		numPresenceBits = numCores;
		invalidations=0;
		writebacks=0;
		dataForwards=0;
		cores = coresArray;
		new HashMap<Long, Cache>();
		this.levelFromTop = CacheType.Directory;
	}
	public int getNumPresenceBits() {
		return numPresenceBits;
	}
	public void setNumPresenceBits(int numPresenceBits) {
		this.numPresenceBits = numPresenceBits;
	}
	public DirectoryEntry lookup(long address, boolean calledFromEviction) {
		//	Search for the directory entry 
		//if not found, create one with invalid state 
		CacheLine cl = processRequest(RequestType.Cache_Read, address);
		if(cl==null) 
		{
			fill(address, MESI.EXCLUSIVE);	//State is a dummy here. It can be anything but invalid
											//Actual DirectoryState is maintained in the dirEntry
											//processRequest and fill are called just to account for hits and misses in the directory
		}
		DirectoryEntry dirEntry = directoryHashmap.get(address);
		if(dirEntry==null){
			if(calledFromEviction)
			{
				System.out.println(" address not found  " + address);
				System.err.println("cache line evicted but no entry in directory found " + address);
				System.exit(1);
			}
			dirEntry=new DirectoryEntry(numPresenceBits, address);
			if(debug) System.out.println(" new Address added in directory  " + address);
			dirEntry.setState( DirectoryState.uncached  );		//Needless as when dirEntry is initialized, state is set to invalid
			directoryHashmap.put( address, dirEntry );
		}
		return dirEntry;
	}
	public long getInvalidations() {
		return invalidations;
	}
	public void incrementInvalidations(int invalidations) {
		this.invalidations += invalidations;
	}
	public long getWritebacks() {
		return writebacks;
	}
	public void incrementWritebacks(int writebacks) {
		this.writebacks += writebacks;
	}
	public long getDataForwards() {
		return dataForwards;
	}
	public void incrementDataForwards(int dataForwards) {
		this.dataForwards += dataForwards;
	}
	
	public void handleEvent( EventQueue eventQ, Event event )
	{
		if(debug) System.out.println("directory handling address "+( (AddressCarryingEvent)event).getAddress() + event.getRequestType()+ " coreID  " + event.coreId);
		if( event.getRequestType() == RequestType.WriteHitDirectoryUpdate )
		{
			writeHitDirectoryUpdate(eventQ,event);
		} 
		else if( event.getRequestType() == RequestType.WriteMissDirectoryUpdate )
		{
			writeMissDirectoryUpdate(eventQ,event);
		}
		else if( event.getRequestType() == RequestType.ReadMissDirectoryUpdate )
		{
			readMissDirectoryUpdate(eventQ, event);
		}
		else if( event.getRequestType() == RequestType.EvictionDirectoryUpdate )
		{
			EvictionDirectoryUpdate(eventQ, event);
		}
		else if (event.getRequestType() == RequestType.MemResponseDirectoryUpdate)
		{
			memResponseDIrectoryUpdate(eventQ,event);
		}
		
		if(debug) System.out.println("directory handled event for address  "+( (AddressCarryingEvent)event).getAddress() + event.getRequestType() + " coreID  " + event.coreId+ "\n\n\n");
	}
	
	private void memResponseDIrectoryUpdate(EventQueue eventQ, Event event) 
	{
		long address = ((AddressCarryingEvent)event).getAddress();
		long dirAddress = getDirectoryAddress((AddressCarryingEvent) event);
		DirectoryEntry dirEntry = lookup(dirAddress,true);
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 
		if(debug) System.out.println("memresponse for address  "+( (AddressCarryingEvent)event).getAddress() + event.getRequestType() + " directory address "+getDirectoryAddress((AddressCarryingEvent) event)+ dirEntry.getState() + ", coreid" + event.coreId);
		dirEntry.setPresenceBit(requestingCore, true);
	}
	
	private boolean checkAndScheduleEventForNextCycle(long dirAddress, Event event)
	{
		if(debug) System.out.println("Check and ScheduleEvent  "+( (AddressCarryingEvent)event).getAddress() + event.getRequestType() + ", coreid" + event.coreId);
		DirectoryEntry dirEntry  = lookup(dirAddress,false);
		if(dirEntry.getOwner() == -1)
		{
			if(dirEntry.getState() == DirectoryState.uncached)
			{
				if(debug) System.out.println("returned false");
				return false;
			}
			this.getPort().put(event.update(event.getEventQ(),
														 1,
														 event.getRequestingElement(), 
														 this, 
														 event.getRequestType()));
			if(debug) System.out.println("returned true");
			return true;
		} 
		if(debug) System.out.println("returned false");
		return false;
	}
	
	public void EvictionDirectoryUpdate(EventQueue eventQ,Event event) 
	{
		long address = ((AddressCarryingEvent)event).getAddress();
		long dirAddress =getDirectoryAddress((AddressCarryingEvent)event);
		DirectoryEntry dirEntry = lookup(dirAddress,true);
		DirectoryState state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 
		if(debug) System.out.println("evictionDirectoryUpdate for address  "+( (AddressCarryingEvent)event).getAddress() + event.getRequestType() + " directory address "+((Cache)event.getRequestingElement()).computeTag(address)+ dirEntry.getState()+ ", coreid" + event.coreId);
		
		if(checkAndScheduleEventForNextCycle(dirAddress, event))
			return;

		if(state==DirectoryState.Modified){
			//Writeback the result
			int prevOwner = dirEntry.getOwner();
			if( prevOwner==requestingCore ){
				((Cache)requestingElement).propogateWrite((AddressCarryingEvent)event);
				dirEntry.setPresenceBit(prevOwner, false);
				//Set the state to invalid - i.e. uncached
				dirEntry.setState(DirectoryState.uncached );
				dirEntry.resetAllPresentBits();
			}
			else{
				//System.err.println(" cacheline in modified state but evicted from some other core ");
				//System.exit(1);
				dirEntry.setPresenceBit(requestingCore, false);	//Redundant
				//If some other node is the owner, that means that the line in this node was already invalid.
				//No need to do anything.
				if(dirEntry.getOwner() == -1)
					dirEntry.setState(DirectoryState.uncached);
			}
		}
		else if(state==DirectoryState.Shared || 
				        state == DirectoryState.Exclusive )
		{
			//Unset the presence bit of this cache - i.e. uncached
			dirEntry.setPresenceBit(requestingCore, false);
			//If no owner left, set the dirState to invalid
			int owner = dirEntry.getOwner();
			if(owner==-1)
				dirEntry.setState(DirectoryState.uncached );
		}
	}


	public void readMissDirectoryUpdate(EventQueue eventQ,Event event) {
		// TODO Auto-generated method stub
		long address = ((AddressCarryingEvent)event).getAddress();
		long dirAddress =getDirectoryAddress((AddressCarryingEvent)event);
		DirectoryEntry dirEntry = lookup(dirAddress,false);
		DirectoryState state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();
		//if(debug) System.out.println(" inside readMiss handling event for address " + ((AddressCarryingEvent)event).getAddress() + "," + state + ", directory address " + dirAddress );
		if(debug) System.out.println("readmissDirectory for address  "+( (AddressCarryingEvent)event).getAddress() + event.getRequestType() + " directory address "+getDirectoryAddress((AddressCarryingEvent) event) + dirEntry.getState() +" , coreId "+ event.coreId);

		DirectoryState stateToSet = DirectoryState.uncached;
		
		if(checkAndScheduleEventForNextCycle(dirAddress, event))
		{
			return;
		}
		
		if( state==DirectoryState.uncached  )
		{
			//dirEntry.setState(Dire);
			stateToSet = DirectoryState.Exclusive;
			if (((Cache)requestingElement).isLastLevel)
			{
				MemorySystem.mainMemory.getPort().put(
						new AddressCarryingEvent(
								eventQ,
								MemorySystem.mainMemory.getLatencyDelay(),
								requestingElement, 
								MemorySystem.mainMemory,
								RequestType.Main_Mem_Read,
								address,
								(event).coreId));
			}
			else
			{
				Cache requestingCache = (Cache)requestingElement;
				requestingCache.sendReadRequestToLowerCache((AddressCarryingEvent)event);
			}
		}
		else if(state==DirectoryState.Modified )
		{
			incrementWritebacks(1);
			int prevOwner = dirEntry.getOwner();
			//Request the owner for the block
			incrementDataForwards(1);
			if(cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop != CacheType.L1)
			{
				System.err.println(" getL1 is not returning L1 cache " + cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop);
				System.exit(1);
			}
			cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
					new AddressCarryingEvent(
									eventQ,
									((Cache)requestingElement).getLatencyDelay(),
									requestingElement,
									cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache(),
									RequestType.Cache_Read_Writeback,
									address,
									(event).coreId));
			stateToSet = DirectoryState.Shared; //TODO check at owner whether the line is evicted or not
																							//Presently It is not checked
		}
		else if(state==DirectoryState.Shared 
				       ||  state == DirectoryState.Exclusive )
		{
			incrementDataForwards(1);
			int owner = dirEntry.getOwner();
			if(cores[owner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop != CacheType.L1)
			{
				System.err.println(" getL1 is not returning L1 cache " + cores[owner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop);
				System.exit(1);
			}
			cores[owner].getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
					new AddressCarryingEvent(
									eventQ,
									((Cache)requestingElement).getLatencyDelay(),
									requestingElement,
									cores[owner].getExecEngine().getCoreMemorySystem().getL1Cache(),
									RequestType.Send_Mem_Response,
									address,
									(event).coreId));
			stateToSet = DirectoryState.Shared;
		}
		dirEntry.setState(stateToSet);
	}

	
	public void writeMissDirectoryUpdate(EventQueue eventQ,Event event) {

		long address = ((AddressCarryingEvent)event).getAddress();
		long dirAddress =getDirectoryAddress((AddressCarryingEvent) event);
		DirectoryEntry dirEntry = lookup(dirAddress,false);
		DirectoryState state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 
		if(debug) System.out.println("writeMissDirectory for address  "+( (AddressCarryingEvent)event).getAddress() + event.getRequestType() + " directory address "+getDirectoryAddress((AddressCarryingEvent) event) + dirEntry.getState() +" , coreId "+ event.coreId);

		if(checkAndScheduleEventForNextCycle(dirAddress, event))
		{
			return;
		}
		
		if(state==DirectoryState.uncached )
		{
			dirEntry.setState(DirectoryState.Exclusive);
			//Request lower levels
			if (((Cache)requestingElement).isLastLevel)
			{
				MemorySystem.mainMemory.getPort().put(
						new AddressCarryingEvent(
								eventQ,
								MemorySystem.mainMemory.getLatencyDelay(),
								requestingElement, 
								MemorySystem.mainMemory,
								RequestType.Main_Mem_Read,
								address,
								(event).coreId));
				
			}
			else
			{
				Cache requestingCache = (Cache)requestingElement;
				requestingCache.sendReadRequestToLowerCache((AddressCarryingEvent)event);
			}
		}
		else if( state==DirectoryState.Modified  )
		{
			//request for the blocks from the previous owner
			int prevOwner = dirEntry.getOwner();
			if(prevOwner == event.coreId)
			{
				/*System.err.println(" cache miss and cache line in modified state ");
				System.exit(1);*/
			}
			else
			{
				incrementDataForwards(1);
				if(cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop != CacheType.L1)
				{
					System.err.println(" getL1 is not returning L1 cache " + cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop);
					System.exit(1);
				}
				cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
						new AddressCarryingEvent(
										eventQ,
										((Cache)requestingElement).getLatencyDelay(),
										requestingElement,
										cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache(),
										RequestType.Send_Mem_Response_Invalidate,
										address,
										(event).coreId));
				dirEntry.setPresenceBit(prevOwner, false);
				dirEntry.resetAllPresentBits();
			}
		}
		else if( state==DirectoryState.Shared  )
		{
			//request for the blocks from any of the owners
			incrementDataForwards(1);
			int prevOwner = dirEntry.getOwner();
			if(cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop != CacheType.L1)
			{
				System.err.println(" getL1 is not returning L1 cache " + cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop);
				System.exit(1);
			}
			cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
					new AddressCarryingEvent(
							eventQ,
							((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner).getLatency(),
							requestingElement, 
							cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache(),
							RequestType.Send_Mem_Response_Invalidate, //TODO change requestType to something
							address,
							(event).coreId));
			dirEntry.setPresenceBit(prevOwner, false);
			//invalidate all except for the one from which the block has been requested
			for(int i=0;i<numPresenceBits;i++)
			{
				if(dirEntry.getPresenceBit(i) && i!=prevOwner && i!=requestingCore/*This last condition now reqd*/)
				{
					dirEntry.setPresenceBit(i, false);
					if(cores[i].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop != CacheType.L1)
					{
						System.err.println(" getL1 is not returning L1 cache " + cores[i].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop);
						System.exit(1);
					}
					cores[i].getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
							new AddressCarryingEvent(
									eventQ,
									((Cache)requestingElement).nextLevel.prevLevel.get(i).getLatency(),
									this, 
									cores[i].getExecEngine().getCoreMemorySystem().getL1Cache(),
									RequestType.MESI_Invalidate, 
									address,
									(event).coreId));
				}
			}
			dirEntry.resetAllPresentBits();
			dirEntry.setState( DirectoryState.Modified );
		}
		else if(state==DirectoryState.Exclusive )
		{
			if(requestingCore == dirEntry.getOwner())
			{
				dirEntry.setState(DirectoryState.Modified);
				//System.err.println(" cache line in exclusive mode but still a cache miss found " + dirEntry.address + " address " + dirAddress + "event address " + ((AddressCarryingEvent)event).getAddress());
				//System.exit(1);
			}
			else
			{
				incrementDataForwards(1);
				int prevOwner = dirEntry.getOwner();
				if(cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop != CacheType.L1)
				{
					System.err.println(" getL1 is not returning L1 cache " + cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop);
					System.exit(1);
				}
				cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
						new AddressCarryingEvent(
								eventQ,
								((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner).getLatency(),
								requestingElement, 
								cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache(),
								RequestType.Send_Mem_Response_Invalidate, 
								address,
								(event).coreId));
				dirEntry.setPresenceBit(prevOwner, false);
				dirEntry.setState( DirectoryState.Modified );
				dirEntry.resetAllPresentBits();
			}
		}
	}

	public void writeHitDirectoryUpdate(EventQueue eventQ,Event event) {
		long address = ((AddressCarryingEvent)event).getAddress();
		long dirAddress =getDirectoryAddress((AddressCarryingEvent) event);
		DirectoryEntry dirEntry = lookup(dirAddress,false);
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 
		if(debug) System.out.println("writeHitDirectory for address  "+( (AddressCarryingEvent)event).getAddress() + event.getRequestType() + " directory address "+((Cache)event.getRequestingElement()).computeTag(address) + dirEntry.getState() +" , coreId "+ event.coreId);

		if(checkAndScheduleEventForNextCycle(dirAddress, event))
		{
			return;
		}
		
		if(dirEntry.getState()==DirectoryState.uncached ){
			System.err.println(" not possible because of cache hit ");
		}
		else if(dirEntry.getState()==DirectoryState.Modified)
		{
			if(requestingCore != dirEntry.getOwner())
			{
				int prevOwner = dirEntry.getOwner();
				incrementDataForwards(1);
				if(cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop != CacheType.L1)
				{
					System.err.println(" getL1 is not returning L1 cache " + cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop);
					System.exit(1);
				}
				cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
						new AddressCarryingEvent(
										eventQ,
										((Cache)requestingElement).getLatencyDelay(),
										requestingElement,
										cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache(),
										RequestType.Send_Mem_Response_On_WriteHit,//TODO handle this event in cache 
										address,
										(event).coreId));
				dirEntry.resetAllPresentBits();
			}
		}
		else if(dirEntry.getState()==DirectoryState.Shared)
		{
			for(int i=0;i<numPresenceBits;i++){
				if(dirEntry.getPresenceBit(i))
				{
					//Invalidate others
					incrementInvalidations(1);
					if(i!=requestingCore)
					{ 
						dirEntry.setPresenceBit(i,false);
						if(cores[i].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop != CacheType.L1)
						{
							System.err.println(" getL1 is not returning L1 cache " + cores[i].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop);
							System.exit(1);
						}
						cores[i].getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
								new AddressCarryingEvent(
										event.getEventQ(),
										((Cache)requestingElement).nextLevel.prevLevel.get(i).getLatency(),
										this, 
										cores[i].getExecEngine().getCoreMemorySystem().getL1Cache(),
										RequestType.MESI_Invalidate, 
										address,
										(event).coreId));
						
					}
				}
				dirEntry.setState(DirectoryState.Modified);
				dirEntry.resetAllPresentBits();
				dirEntry.setPresenceBit(requestingCore, true);
			}			
		}
		else if(dirEntry.getState()==DirectoryState.Exclusive )
		{
			if(requestingCore == dirEntry.getOwner( ))
			{
				dirEntry.setState(DirectoryState.Modified);
			}
			else
			{
				incrementDataForwards(1);
				int prevOwner = dirEntry.getOwner();
				if(cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop != CacheType.L1)
				{
					System.err.println(" getL1 is not returning L1 cache " + cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().levelFromTop);
					System.exit(1);
				}
				cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
						new AddressCarryingEvent(
								eventQ,
								((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner).getLatency(),
								requestingElement, 
								cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache(),
								RequestType.Send_Mem_Response_On_WriteHit, 
								address,
								(event).coreId));
				dirEntry.setPresenceBit(prevOwner, false);
				dirEntry.setState( DirectoryState.Modified );
				dirEntry.resetAllPresentBits();
			}
		}
	}
	
	public long getDirectoryAddress(AddressCarryingEvent event)
	{
		long address = event.getAddress();
		long tag = ((Cache)event.getRequestingElement()).computeTag(address);
		int startIdx = ((Cache)event.getRequestingElement()).getStartIdx(address);
		
		long addressToStore =  address >>>  ((Cache)event.getRequestingElement()).blockSizeBits;
		
		if(debug) System.out.println("address returned " + addressToStore);
		
		return addressToStore;
	}
}
