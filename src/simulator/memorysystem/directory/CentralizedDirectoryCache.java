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
	public CentralizedDirectoryCache(CacheConfig cacheParameters,
			CoreMemorySystem containingMemSys, int numCores, Core[] coresArray) {
		super(cacheParameters, containingMemSys);
		directoryHashmap = new HashMap<Long, DirectoryEntry>();
		numPresenceBits = numCores;
		invalidations=0;
		writebacks=0;
		dataForwards=0;
		cores = coresArray;
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
		if(cl==null){
			fill(address, MESI.EXCLUSIVE);	//State is a dummy here. It can be anything but invalid
											//Actual DirectoryState is maintained in the dirEntry
											//processRequest and fill are called just to account for hits and misses in the directory
		}
		DirectoryEntry dirEntry = directoryHashmap.get(address);
		if(dirEntry==null){
			dirEntry=new DirectoryEntry(numPresenceBits, address);
			dirEntry.setState(DirectoryState.Invalid);		//Needless as when dirEntry is initialized, state is set to invalid
			directoryHashmap.put(address, dirEntry);
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
	protected void handleAccess(EventQueue eventQ, Event event){
		switch(event.getRequestType()){
		case WriteHitDirectoryUpdate:
			writeHitDirectoryUpdate(eventQ,event);
			break;
		case WriteMissDirectoryUpdate:
			writeMissDirectoryUpdate(eventQ,event);
			break;
		case ReadMissDirectoryUpdate:
			readMissDirectoryUpdate(eventQ,event);
			break;
		case EvictionDirectoryUpdate:
			EvictionDirectoryUpdate(eventQ,event);
			break;
		}
	}
	private void EvictionDirectoryUpdate(EventQueue eventQ,Event event) {
		long address = ((AddressCarryingEvent)event).getAddress();
		long dirAddress = address >>> this.blockSizeBits;
		DirectoryEntry dirEntry = lookup(dirAddress);
		DirectoryState state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 

		if(state==DirectoryState.Invalid){
			//Do Nothing
		}
		else if(state==DirectoryState.Modified){
			//Writeback the result
			int prevOwner = dirEntry.getOwner();
			//FIXME The following if-else should be present
			//This owner should be this node itself
			if(prevOwner==requestingCore){
				//If this core itself is the owner of the line, it will be written back in the cache logic itself. Update the directory
				//Unset the presence bit
				dirEntry.setPresenceBit(prevOwner, false);
				//Set the state to invalid - i.e. uncached
				dirEntry.setState(DirectoryState.Invalid);
			}
			else{
				dirEntry.setPresenceBit(requestingCore, false);	//Redundant
				//If some other node is the owner, that means that the line in this node was already invalid.
				//No need to do anything.
			}
		
		}
		else if(state==DirectoryState.Shared){
			//Unset the presence bit of this cache - i.e. uncached
			dirEntry.setPresenceBit(requestingCore, false);
			//If no owner left, set the dirState to invalid
			int owner = dirEntry.getOwner();
			if(owner==-1)
				dirEntry.setState(DirectoryState.Invalid);
		}
	}


	private void readMissDirectoryUpdate(EventQueue eventQ,Event event) {
		// TODO Auto-generated method stub
		long address = ((AddressCarryingEvent)event).getAddress();
		long dirAddress = address >>> this.blockSizeBits;
		DirectoryEntry dirEntry = lookup(dirAddress);
		DirectoryState state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 
		if(state==DirectoryState.Invalid){
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
				((Cache)requestingElement).nextLevel.getPort().put(
						new AddressCarryingEvent(
								eventQ,
								((Cache)requestingElement).nextLevel.getLatencyDelay(),
								requestingElement, 
								((Cache)requestingElement).nextLevel,
								RequestType.Cache_Read, 
								address,
								(event).coreId));
			}
		}
		else if(state==DirectoryState.Modified ){
			//Writeback from the previous owner to the lower levels
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
		}
		else if(state==DirectoryState.Shared){
			incrementDataForwards(1);
			int owner = dirEntry.getOwner();
			//Request any of the owners for the block
			((Cache)requestingElement).nextLevel.prevLevel.get(owner).getPort().put(
					new AddressCarryingEvent(
									eventQ,
									((Cache)requestingElement).getLatencyDelay(),
									requestingElement,
									((Cache)requestingElement).nextLevel.prevLevel.get(owner),
									RequestType.Cache_Read,
									address,
									(event).coreId));
		}
		dirEntry.setPresenceBit(requestingCore, true);
		dirEntry.setState(DirectoryState.Shared);
	
	}

	
	private void writeMissDirectoryUpdate(EventQueue eventQ,Event event) {

		long address = ((AddressCarryingEvent)event).getAddress();
		long dirAddress = address >>> this.blockSizeBits;
		DirectoryEntry dirEntry = lookup(dirAddress);
		DirectoryState state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 

		if(state==DirectoryState.Invalid){
			//Request lower levels
			if (((Cache)requestingElement).isLastLevel)
			{
				MemorySystem.mainMemory.getPort().put(
						/*new AddressCarryingEvent(
								eventQ,
								MemorySystem.mainMemory.getLatencyDelay(),
								requestingElement, 
								MemorySystem.mainMemory,
								RequestType.Main_Mem_Read,
								address,
								(event).coreId)*/
						event.update(eventQ,
								MemorySystem.mainMemory.getLatencyDelay(), 
								requestingElement, 
								MemorySystem.mainMemory, 
								RequestType.Main_Mem_Read));
				
			}
			else
			{
				((Cache)requestingElement).nextLevel.getPort().put(
						/*new AddressCarryingEvent(
								event.getEventQ(),
								((Cache)requestingElement).nextLevel.getLatencyDelay(),
								requestingElement, 
								((Cache)requestingElement).nextLevel,
								RequestType.Cache_Read, 
								address,
								(event).coreId)*/
						event.update(eventQ,
								((Cache)requestingElement).nextLevel.getLatencyDelay(), 
								requestingElement, 
								((Cache)requestingElement).nextLevel, 
								RequestType.Cache_Read));
			}
		}
		else if(state==DirectoryState.Modified){
			//request for the blocks from the previous owner
			int prevOwner = dirEntry.getOwner();
			incrementDataForwards(1);
			((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner).getPort().put(
					/*new AddressCarryingEvent(
							event.getEventQ(),
							((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner).getLatency(),
							requestingElement, 
							((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner),
							RequestType.Cache_Read_Writeback_Invalidate, 
							address,
							(event).coreId)*/
					event.update(eventQ, 
							((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner).getLatency(), 
							requestingElement, 
							((Cache)requestingElement).nextLevel.prevLevel.get(prevOwner), 
							RequestType.Cache_Read_Writeback_Invalidate));
			dirEntry.setPresenceBit(prevOwner, false);
			
		}
		else if(state==DirectoryState.Shared){
			//request for the blocks from any of the owners
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
			//invalidate all except for the one from which the block has been requested
			for(int i=0;i<numPresenceBits;i++){
				if(dirEntry.getPresenceBit(i) && i!=prevOwner && i!=requestingCore/*This last condition now reqd*/){
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
		}
		dirEntry.setPresenceBit(requestingCore, true);
		dirEntry.setState(DirectoryState.Modified);


	}

	private void writeHitDirectoryUpdate(EventQueue eventQ,Event event) {
		long address = ((AddressCarryingEvent)event).getAddress();
		long dirAddress = address >>> this.blockSizeBits;
		DirectoryEntry dirEntry = lookup(dirAddress);
		DirectoryState state = dirEntry.getState();
		SimulationElement requestingElement = event.getRequestingElement();
		int requestingCore = ((Cache)requestingElement).containingMemSys.getCore().getCore_number(); 

		if(dirEntry.getState()==DirectoryState.Invalid){
			dirEntry.setState(DirectoryState.Modified);
			dirEntry.setPresenceBit(requestingCore, true);
		}
		else if(dirEntry.getState()==DirectoryState.Modified){
//since it is a write HIT and state is Modified, if this core itself is the owner, there is nothing to do
/*				//Invalidate the previous owner
			int prevOwner = dirEntry.getOwner();
			dirEntry.setPresenceBit(prevOwner,false);
			this.nextLevel.prevLevel.get(prevOwner).getPort().put(
					new AddressCarryingEvent(
							event.getEventQ(),
							this.nextLevel.prevLevel.get(prevOwner).getLatency(),
							this, 
							this.nextLevel.prevLevel.get(prevOwner),
							RequestType.MESI_Invalidate, 
							address,
							(event).coreId));
			dirEntry.setPresenceBit(requestingCore, true);
*/
//			if(dirEntry.getOwner()!=requestingCore)
//				System.err.println("Directory entry inconsistency for Write Hit Modified case "+dirEntry.getOwner()+" "+requestingCore);
		
//FIXME The following code should not be executed because prevOwner should be same as requesting core as it is a hit!
/*				int prevOwner = dirEntry.getOwner();
			if (this.isLastLevel)
			{
				if (this.levelFromTop == CacheType.L1)
					MemorySystem.mainMemory.getPort().put(
							new AddressCarryingEvent(
									event.getEventQ(),
									MemorySystem.mainMemory.getLatencyDelay(),
									this.nextLevel.prevLevel.get(prevOwner),
									MemorySystem.mainMemory,
									RequestType.Main_Mem_Write,
									address,
									(event).coreId));
				else
					MemorySystem.mainMemory.getPort().put(
							event.update(
									event.getEventQ(),
									MemorySystem.mainMemory.getLatencyDelay(),
									this.nextLevel.prevLevel.get(prevOwner),
									MemorySystem.mainMemory,
									RequestType.Main_Mem_Write));
			}
			else
			{
				if (this.levelFromTop == CacheType.L1)
					this.nextLevel.getPort().put(
							new AddressCarryingEvent(
									event.getEventQ(),
									this.nextLevel.getLatencyDelay(),
									this.nextLevel.prevLevel.get(prevOwner),
									this.nextLevel,
									RequestType.Cache_Write,
									address,
									(event).coreId));
				else
					this.nextLevel.getPort().put(
							event.update(
									event.getEventQ(),
									this.nextLevel.getLatencyDelay(),
									this.nextLevel.prevLevel.get(prevOwner),
									this.nextLevel,
									RequestType.Cache_Write));
			}
			
			//Invalidate the previous owner
			dirEntry.setPresenceBit(prevOwner,false);
			this.nextLevel.prevLevel.get(prevOwner).getPort().put(
					new AddressCarryingEvent(
							event.getEventQ(),
							this.nextLevel.prevLevel.get(prevOwner).getLatency(),
							this, 
							this.nextLevel.prevLevel.get(prevOwner),
							RequestType.MESI_Invalidate, 
							address,
							(event).coreId));
			dirEntry.setPresenceBit(requestingCore, true);
*/
			
		}
		else if(dirEntry.getState()==DirectoryState.Shared){
			for(int i=0;i<numPresenceBits;i++){
				if(dirEntry.getPresenceBit(i)){
					//Invalidate others
					incrementInvalidations(1);
					if(i!=requestingCore){ 
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
			}
			dirEntry.setState(DirectoryState.Modified);
			dirEntry.setPresenceBit(requestingCore, true);
		}
		
	}

}
