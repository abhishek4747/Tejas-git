package memorysystem;

import java.util.ArrayList;

import memorysystem.LSQEntry.LSQEntryType;

import generic.NewEventQueue;
import generic.Time_t;
import generic.NewEvent;
import generic.RequestType;
import generic.SimulationElement;

/**
 * @author Moksh Upadhyay
 * Used to indicate to a memory element (generally caches) to inform 
 * them that a block has been received for some outstanding request
 */
public class BlockReadyEvent extends NewEvent
{
	/**
	 * Just stores the LSQ entry index if the ready event is for an LSQ.
	 * Stores the INVALID_INDEX otherwise.
	 */
	int lsqIndex = LSQ.INVALID_INDEX;
	long address;
/*
	//For the caches
	public BlockReadyEvent(Time_t eventTime, SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker,
			RequestType requestType) 
	{
		super(eventTime, requestingElement, processingElement, tieBreaker, requestType);
		// TODO Auto-generated constructor stub
	}
*/ 
	//For LSQ
	public BlockReadyEvent(Time_t eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker,
			RequestType requestType, long address, int lsqIndex) 
	{
		super(eventTime, requestingElement, processingElement, tieBreaker,
				requestType);
		this.lsqIndex = lsqIndex;
		this.address = address;
	}

	@Override
	public void handleEvent(NewEventQueue newEventQueue)
	{
		if (lsqIndex == LSQ.INVALID_INDEX) //The processing element is a Cache
		{
			Cache receivingCache = (Cache)(this.getProcessingElement());
			receiveBlockAtCache(newEventQueue, receivingCache, address);
		}
		else //The processing element is an LSQ
		{
			LSQ receivingLSQ = (LSQ)(this.getProcessingElement());
			receiveBlockAtLSQ(newEventQueue, receivingLSQ);
		}
	}
	
	
	/**
	 * @author Moksh Upadhyay
	 * When a data block requested by an outstanding request arrives through a BlockReadyEvent,
	 * this method is called to process the arrival of block and process all the outstanding requests.
	 * @param addr : Memory address requested
	 */
	protected void receiveBlockAtCache(NewEventQueue newEventQueue,Cache receivingCache, long addr)
	{
		CacheLine evictedLine = receivingCache.fill(addr);//FIXME
		
		if (!/*NOT*/receivingCache.outstandingRequestTable.containsKey(addr))
		{
			System.err.println("Memory System Crash : An outstanding request not found in the requesting element");
			System.exit(1);
		}
		
		ArrayList<CacheOutstandingRequestTableEntry> outstandingRequestList = receivingCache.outstandingRequestTable.get(addr);
		
		while (!/*NOT*/outstandingRequestList.isEmpty())
		{
			if (outstandingRequestList.get(0).requestType == RequestType.MEM_READ)
			{
				//Pass the value to the waiting element
				//Create an event (BlockReadyEvent) for the waiting element
				//Generate the event for the Upper level cache or LSQ
				if (outstandingRequestList.get(0).lsqIndex == LSQ.INVALID_INDEX)
					//Generate the event for the Upper level cache
					newEventQueue.addEvent(new BlockReadyEvent(outstandingRequestList.get(0).requestingElement.getLatency(),//FIXME 
															this.getProcessingElement(),
															outstandingRequestList.get(0).requestingElement, 
															0, //tieBreaker
															RequestType.MEM_BLOCK_READY,
															address,
															lsqIndex));
				else
					//Generate the event to tell the LSQ
					newEventQueue.addEvent(new BlockReadyEvent(outstandingRequestList.get(0).requestingElement.getLatency(),//FIXME 
															this.getProcessingElement(),
															outstandingRequestList.get(0).requestingElement, 
															0, //tieBreaker
															RequestType.LSQ_LOAD_COMPLETE,
															address,
															lsqIndex));
			}
			
			else if (outstandingRequestList.get(0).requestType == RequestType.MEM_WRITE)
			{
				//Write the value to the block (Do Nothing)
				//Pass the value to the waiting element
				//Create an event (BlockReadyEvent) for the waiting element
				if (outstandingRequestList.get(0).lsqIndex != LSQ.INVALID_INDEX)
					//(If the requesting element is LSQ)
					//Generate the event to tell the LSQ
					newEventQueue.addEvent(new BlockReadyEvent(outstandingRequestList.get(0).requestingElement.getLatency(),//FIXME 
															this.getProcessingElement(),
															outstandingRequestList.get(0).requestingElement, 
															0, //tieBreaker
															RequestType.LSQ_WRITE_COMMIT,
															address,
															lsqIndex));
				
				//Handle in any case (Whether requesting element is LSQ or cache)
				//TODO : handle write-value forwarding (for Write-Through and Coherent caches)
				
			}
			else
			{
				System.err.println("Memory System Crash : A request was of type other than MEM_READ or MEM_WRITE");
				System.exit(1);
			}
		}
	}
	
	protected void receiveBlockAtLSQ(NewEventQueue newEventQueue, LSQ receivingLSQ)
	{
		if (receivingLSQ.lsqueue[lsqIndex].getType() == LSQEntryType.LOAD)
		{
			receivingLSQ.lsqueue[lsqIndex].setForwarded(true);
			
			//TODO : May be here will come a call to an event to tell the processing unit
			//that the data is available
		}
		else if (receivingLSQ.lsqueue[lsqIndex].getType() == LSQEntryType.STORE)
		{
			//TODO : Commit the STORE entry in the LSQ and may be generate an event 
			//to tell the ROB or something
		}
			
	}
}
