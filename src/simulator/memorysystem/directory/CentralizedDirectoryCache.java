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
import memorysystem.Cache.CacheType;

public class CentralizedDirectoryCache extends Cache{

	private int numPresenceBits;
	private long invalidations;
	private long writebacks;
	private long dataForwards;
	Core[] cores;
	
	
	private HashMap<Long, DirectoryEntry> directoryHashmap;
	private HashMap<Long,Cache> requestedCache;
	public CentralizedDirectoryCache(CacheConfig cacheParameters,
			CoreMemorySystem containingMemSys, int numCores, Core[] coresArray) {
		super(cacheParameters, containingMemSys);
		directoryHashmap = new HashMap<Long, DirectoryEntry>();
		numPresenceBits = numCores;
		invalidations=0;
		writebacks=0;
		dataForwards=0;
		cores = coresArray;
		requestedCache = new HashMap<Long, Cache>();
	}
	public int getNumPresenceBits() {
		return numPresenceBits;
	}
	public void setNumPresenceBits(int numPresenceBits) {
		this.numPresenceBits = numPresenceBits;
	}
	public DirectoryEntry lookup(long address) {
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
		//System.out.println("directory handling address "+( (AddressCarryingEvent)event).getAddress() + event.getRequestType());
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
	
	private void memResponseDIrectoryUpdate(EventQueue eventQ, Event event) {
		long address = ((AddressCarryingEvent)event).getAddress();
		long dirAddress = address >>> this.blockSizeBits;
		DirectoryEntry dirEntry = lookup(dirAddress);
		DirectoryState state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 
		
		if( state == DirectoryState.uncached )
		{
			if(requestingCore != dirEntry.getOwner())
			{
				System.err.println(" core which requested line is not owner ");
			}
			dirEntry.setState(DirectoryState.Exclusive);
			dirEntry.setPresenceBit(requestingCore, true);
		}
		else if( state == DirectoryState.Shared )
		{
			dirEntry.setPresenceBit(requestingCore, true);
		}

	}
	
	private void EvictionDirectoryUpdate(EventQueue eventQ,Event event) 
	{
		long address = ((AddressCarryingEvent)event).getAddress();
		long dirAddress = address >>> this.blockSizeBits;
		DirectoryEntry dirEntry = lookup(dirAddress);
		DirectoryState state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 
		System.out.println(" Evicting line with  block address " + dirAddress);
		
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
				System.err.println(" cacheline in modified state but evicted from some other core ");
				dirEntry.setPresenceBit(requestingCore, false);	//Redundant
				//If some other node is the owner, that means that the line in this node was already invalid.
				//No need to do anything.
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


	private void readMissDirectoryUpdate(EventQueue eventQ,Event event) {
		// TODO Auto-generated method stub
		long address = ((AddressCarryingEvent)event).getAddress();
		long dirAddress = address >>> this.blockSizeBits;
		DirectoryEntry dirEntry = lookup(dirAddress);
		DirectoryState state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();
		//System.out.println(" inside readMiss handling event for address " + ((AddressCarryingEvent)event).getAddress() + "," + state + ", directory address " + dirAddress );
		DirectoryState stateToSet = DirectoryState.uncached;
		
		if( state==DirectoryState.uncached  )
		{
			if( requestedCache.containsKey(dirAddress))
			{

				if(requestedCache.get(dirAddress).equals(event.getRequestingElement()))
				{
					System.err.println(" request from same cache again came before serving ");
				}
				else
				{
					this.getPort().put(event.update(event.getEventQ(),
															 1,
															 requestingElement, 
															 this, 
															 event.getRequestType()));
				}
			}
			else 
			{
				requestedCache.put(dirAddress, (Cache)requestingElement);
				dirEntry.setPresenceBit( event.coreId, true );

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
		}
		else if(state==DirectoryState.Modified )
		{
			incrementWritebacks(1);
			int prevOwner = dirEntry.getOwner();
			//Request the owner for the block
			incrementDataForwards(1);
			((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner).getPort().put(
					new AddressCarryingEvent(
									eventQ,
									((Cache)requestingElement).getLatencyDelay(),
									requestingElement,
									((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner),
									RequestType.Cache_Read_Writeback,
									address,
									(event).coreId));
			stateToSet = DirectoryState.Shared; //TODO check at owner whether the line is evicted or not
																							//Presently It is not checked
			//dirEntry.setPresenceBit(requestingCore, true);
		}
		else if(state==DirectoryState.Shared 
				       ||  state == DirectoryState.Exclusive )
		{
			incrementDataForwards(1);
			int owner = dirEntry.getOwner();

			((Cache)requestingElement).nextLevel.prevLevel.get(owner).getPort().put(
					new AddressCarryingEvent(
									eventQ,
									((Cache)requestingElement).getLatencyDelay(),
									requestingElement,
									((Cache)requestingElement).nextLevel.prevLevel.get(owner),
									RequestType.Send_Mem_Response,
									address,
									(event).coreId));
			stateToSet = DirectoryState.Shared;
			//dirEntry.setPresenceBit(requestingCore, true);
		}
		dirEntry.setState(stateToSet);
	}

	
	private void writeMissDirectoryUpdate(EventQueue eventQ,Event event) {

		long address = ((AddressCarryingEvent)event).getAddress();
		long dirAddress = address >>> this.blockSizeBits;
		DirectoryEntry dirEntry = lookup(dirAddress);
		DirectoryState state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 

		if(state==DirectoryState.uncached )
		{
			
			if( requestedCache.containsKey(dirAddress))
			{

				if(requestedCache.get(dirAddress).equals(event.getRequestingElement()))
				{
					System.err.println(" request from same cache again came before serving ");
				}
				else
				{
					this.getPort().put(event.update(event.getEventQ(),
															 1,
															 requestingElement, 
															 this, 
															 event.getRequestType()));
				}
			}
			else 
			{
				requestedCache.put(dirAddress, (Cache)requestingElement);
				dirEntry.setPresenceBit( event.coreId, true );
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
		}
		else if( state==DirectoryState.Modified  ){
			//request for the blocks from the previous owner
			int prevOwner = dirEntry.getOwner();
			if(prevOwner == -1)
			{
				if( requestedCache.containsKey(dirAddress))
				{

					if(requestedCache.get(dirAddress).equals(event.getRequestingElement()))
					{
						System.err.println(" request from same cache again came before serving ");
					}
					else
					{
						this.getPort().put(event.update(event.getEventQ(),
																 1,
																 requestingElement, 
																 this, 
																 event.getRequestType()));
					}
				}
				else 
				{
					requestedCache.put(dirAddress, (Cache)requestingElement);
				}
				return;
			}
			incrementDataForwards(1);
			((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner).getPort().put(
					new AddressCarryingEvent(
									eventQ,
									((Cache)requestingElement).getLatencyDelay(),
									requestingElement,
									((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner),
									RequestType.Cache_Read_Writeback_Invalidate,
									address,
									(event).coreId));
			dirEntry.setPresenceBit(prevOwner, false);
		}
		else if( state==DirectoryState.Shared  )
		{
			//request for the blocks from any of the owners
			incrementDataForwards(1);
			int prevOwner = dirEntry.getOwner();
			((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner).getPort().put(
					new AddressCarryingEvent(
							eventQ,
							((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner).getLatency(),
							requestingElement, 
							((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner),
							RequestType.Cache_Read_Writeback_Invalidate, //TODO change requestType to something
							address,
							(event).coreId));
			dirEntry.setPresenceBit(prevOwner, false);
			//invalidate all except for the one from which the block has been requested
			for(int i=0;i<numPresenceBits;i++)
			{
				if(dirEntry.getPresenceBit(i) && i!=prevOwner && i!=requestingCore/*This last condition now reqd*/)
				{
					dirEntry.setPresenceBit(i, false);
					((Cache)requestingElement).nextLevel.prevLevel.get(i).getPort().put(
							new AddressCarryingEvent(
									eventQ,
									((Cache)requestingElement).nextLevel.prevLevel.get(i).getLatency(),
									this, 
									((Cache)requestingElement).nextLevel.prevLevel.get(i),
									RequestType.MESI_Invalidate, 
									address,
									(event).coreId));
				}
			}
			dirEntry.resetAllPresentBits();
		}
		else if(state==DirectoryState.Exclusive )
		{
			if(requestingCore == dirEntry.getOwner())
			{
				System.err.println(" cache line in exclusive mode but still a cache miss found ");
			}
			else
			{
				incrementDataForwards(1);
				int prevOwner = dirEntry.getOwner();
				((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner).getPort().put(
						new AddressCarryingEvent(
								eventQ,
								((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner).getLatency(),
								requestingElement, 
								((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner),
								RequestType.Cache_Read_Writeback_Invalidate, 
								address,
								(event).coreId));
				dirEntry.setPresenceBit(prevOwner, false);
				dirEntry.setState( DirectoryState.Modified );
				dirEntry.resetAllPresentBits();
			}
		}
	}

	private void writeHitDirectoryUpdate(EventQueue eventQ,Event event) {
		long address = ((AddressCarryingEvent)event).getAddress();
		long dirAddress = address >>> this.blockSizeBits;
		DirectoryEntry dirEntry = lookup(dirAddress);
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 

		if(dirEntry.getState()==DirectoryState.uncached ){
			System.err.println(" not possible because of cache hit ");
		}
		else if(dirEntry.getState()==DirectoryState.Modified)
		{
			if(dirEntry.getOwner() == -1)
			{
				if( requestedCache.containsKey(dirAddress))
				{

					if(requestedCache.get(dirAddress).equals(event.getRequestingElement()))
					{
						System.err.println(" request from same cache again came before serving ");
					}
					else
					{
						this.getPort().put(event.update(event.getEventQ(),
																 1,
																 requestingElement, 
																 this, 
																 event.getRequestType()));
					}
				}
				else 
				{
					requestedCache.put(dirAddress, (Cache)requestingElement);
				}
				return;
			}
			if(requestingCore != dirEntry.getOwner())
			{
				int prevOwner = dirEntry.getOwner();
				incrementDataForwards(1);
				((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner).getPort().put(
						new AddressCarryingEvent(
										eventQ,
										((Cache)requestingElement).getLatencyDelay(),
										requestingElement,
										((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner),
										RequestType.Cache_Write_Writeback_Invalidate,//TODO handle this event in cache 
										address,
										(event).coreId));
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
						((Cache)requestingElement).nextLevel.prevLevel.get(i).getPort().put(
								new AddressCarryingEvent(
										event.getEventQ(),
										((Cache)requestingElement).nextLevel.prevLevel.get(i).getLatency(),
										this, 
										((Cache)requestingElement).nextLevel.prevLevel.get(i),
										RequestType.MESI_Invalidate, 
										address,
										(event).coreId));
						
					}
				}
				dirEntry.setState(DirectoryState.Modified);
				dirEntry.setPresenceBit(requestingCore, true);
			}			
		}
	}
}
