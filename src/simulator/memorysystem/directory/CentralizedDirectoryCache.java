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
	}
	
	private void memResponseDIrectoryUpdate(EventQueue eventQ, Event event) 
	{
		long dirAddress = getDirectoryAddress((AddressCarryingEvent) event);
		DirectoryEntry dirEntry = lookup(dirAddress,true);
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 
		dirEntry.setPresenceBit(requestingCore, true);
	}
	
	private boolean checkAndScheduleEventForNextCycle(long dirAddress, Event event)
	{
		DirectoryEntry dirEntry  = lookup(dirAddress,false);
		if(dirEntry.getOwner() == -1)
		{
			if(dirEntry.getState() == DirectoryState.uncached)
			{
				return false;
			}
			this.getPort().put(event.update(event.getEventQ(),
														 1,
														 event.getRequestingElement(), 
														 this, 
														 event.getRequestType()));
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

		long dirAddress =getDirectoryAddress((AddressCarryingEvent)event);
		DirectoryEntry dirEntry = lookup(dirAddress,false);
		DirectoryState state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();

		DirectoryState stateToSet = DirectoryState.uncached;
		
		if(checkAndScheduleEventForNextCycle(dirAddress, event))
		{
			return;
		}
		
		if( state==DirectoryState.uncached  )
		{
			stateToSet = DirectoryState.Exclusive;
			if (((Cache)requestingElement).isLastLevel)
			{
				sendRequestToMainMemory( (AddressCarryingEvent)event );
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
			sendMemResponse(dirEntry, (AddressCarryingEvent)event, RequestType.Cache_Read_Writeback);
			stateToSet = DirectoryState.Shared; //TODO check at owner whether the line is evicted or not
																							//Presently It is not checked
		}
		else if(state==DirectoryState.Shared 
				       ||  state == DirectoryState.Exclusive )
		{
			incrementDataForwards(1);
			sendMemResponse(dirEntry, (AddressCarryingEvent)event, RequestType.Send_Mem_Response);
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
				sendRequestToMainMemory((AddressCarryingEvent)event);
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
				cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
						new AddressCarryingEvent(
										eventQ,
										((Cache)requestingElement).getLatencyDelay(),
										requestingElement,
										cores[prevOwner].getExecEngine().getCoreMemorySystem().getL1Cache(),
										RequestType.Send_Mem_Response,
										address,
										(event).coreId));
			}
			else
			{
				sendMemResponse(dirEntry,(AddressCarryingEvent) event, RequestType.Send_Mem_Response_Invalidate);
				dirEntry.resetAllPresentBits();
			}
		}
		else if( state==DirectoryState.Shared  )
		{
			//request for the blocks from any of the owners
			sendMemResponse(dirEntry,(AddressCarryingEvent) event, RequestType.Send_Mem_Response_Invalidate);

			//invalidate all except for the one from which the block has been requested
			sendeventToSharers(dirEntry,(AddressCarryingEvent) event, RequestType.MESI_Invalidate);
			dirEntry.resetAllPresentBits();
			dirEntry.setState( DirectoryState.Modified );
		}
		else if(state==DirectoryState.Exclusive )
		{
			if(requestingCore == dirEntry.getOwner())
			{
				dirEntry.setState(DirectoryState.Modified);
				cores[requestingCore].getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
						new AddressCarryingEvent(
										eventQ,
										((Cache)requestingElement).getLatencyDelay(),
										requestingElement,
										cores[requestingCore].getExecEngine().getCoreMemorySystem().getL1Cache(),
										RequestType.Send_Mem_Response,
										address,
										(event).coreId));
			}
			else
			{
				sendMemResponse(dirEntry, (AddressCarryingEvent)event,RequestType.Send_Mem_Response_Invalidate );
				dirEntry.resetAllPresentBits();
				dirEntry.setState( DirectoryState.Modified );
			}
		}
	}

	public void writeHitDirectoryUpdate(EventQueue eventQ,Event event) {
		long dirAddress =getDirectoryAddress((AddressCarryingEvent) event);
		DirectoryEntry dirEntry = lookup(dirAddress,false);
		int requestingCore = event.coreId; 

		if(checkAndScheduleEventForNextCycle(dirAddress, event))
		{
			return;
		}
		
		if(dirEntry.getState()==DirectoryState.uncached ){
			//System.err.println(" not possible because of cache hit ");
		}
		else if(dirEntry.getState()==DirectoryState.Modified)
		{
			if(requestingCore != dirEntry.getOwner())
			{
				sendMemResponse(dirEntry, (AddressCarryingEvent)event,RequestType.Send_Mem_Response_On_WriteHit );			
				dirEntry.resetAllPresentBits();
			}
		}
		else if(dirEntry.getState()==DirectoryState.Shared)
		{
			sendeventToSharers(dirEntry,(AddressCarryingEvent) event, RequestType.MESI_Invalidate);	
			dirEntry.setState(DirectoryState.Modified);
			dirEntry.resetAllPresentBits();
			dirEntry.setPresenceBit(requestingCore, true);
		}
		else if(dirEntry.getState()==DirectoryState.Exclusive )
		{
			if(requestingCore == dirEntry.getOwner( ))
			{
				dirEntry.setState(DirectoryState.Modified);
			}
			else
			{
				sendMemResponse(dirEntry, (AddressCarryingEvent)event,RequestType.Send_Mem_Response_On_WriteHit);
				dirEntry.resetAllPresentBits();
				dirEntry.setState( DirectoryState.Modified );
			}
		}
	}
	
	private void sendMemResponse(DirectoryEntry dirEntry,AddressCarryingEvent event,RequestType requestType)
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
						event.getEventQ(),
						cores[owner].getExecEngine().getCoreMemorySystem().getL1Cache().getLatency(),
						event.getRequestingElement(), 
						cores[owner].getExecEngine().getCoreMemorySystem().getL1Cache(),
						requestType, 
						event.getAddress(),
						(event).coreId));
	}
	
	private void sendeventToSharers(DirectoryEntry dirEntry,AddressCarryingEvent event, RequestType requestType)
	{
		int requestingCore = event.coreId;
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
									cores[i].getExecEngine().getCoreMemorySystem().getL1Cache().getLatency(),
									this, 
									cores[i].getExecEngine().getCoreMemorySystem().getL1Cache(),
									requestType, 
									event.getAddress(),
									(event).coreId));
				}
			}
		}
	}
	
	private void sendRequestToMainMemory(AddressCarryingEvent event)
	{
		MemorySystem.mainMemory.getPort().put(
				new AddressCarryingEvent(
						event.getEventQ(),
						MemorySystem.mainMemory.getLatencyDelay(),
						event.getRequestingElement(), 
						MemorySystem.mainMemory,
						RequestType.Main_Mem_Read,
						event.getAddress(),
						(event).coreId));
	}
	
	public long getDirectoryAddress(AddressCarryingEvent event)
	{
		long address = event.getAddress();
		long addressToStore =  address >>>  ((Cache)event.getRequestingElement()).blockSizeBits;
		
		if(debug) System.out.println("address returned " + addressToStore);
		
		return addressToStore;
	}
}
