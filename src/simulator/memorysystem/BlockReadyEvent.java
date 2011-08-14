package memorysystem;

import java.util.ArrayList;

import pipeline.outoforder.ReorderBufferEntry;
import generic.PortRequestEvent;
import generic.ExecutionCompleteEvent;

import config.CacheConfig;
import emulatorinterface.Newmain;

import memorysystem.LSQEntry.LSQEntryType;

import generic.Core;
import generic.GlobalClock;
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
		CacheLine evictedLine = receivingCache.fill(addr);//FIXME : Have to handle whole eviction process
		if (evictedLine != null)
		{
			if (receivingCache.isLastLevel)
				newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker,  
						1, //noOfSlots, 
						new NewMainMemAccessEvent(MemorySystem.getMainMemLatencyDelay(),//FIXME :main memory latency is going to come here
								receivingCache, 
								0, //tieBreaker,
								evictedLine.getTag() << receivingCache.blockSizeBits,
								RequestType.MEM_WRITE)));
			else
				newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker, 
						1, //noOfSlots,
						new NewCacheAccessEvent(receivingCache.nextLevel.getLatencyDelay(),//FIXME
								receivingCache,
								receivingCache.nextLevel,
								LSQ.INVALID_INDEX, 
								0, //tieBreaker,
								new CacheRequestPacket(RequestType.MEM_WRITE,
										evictedLine.getTag() << receivingCache.blockSizeBits))));
		}
		
		long blockAddr = addr >> receivingCache.blockSizeBits;
		if (!/*NOT*/receivingCache.outstandingRequestTable.containsKey(blockAddr))
		{
			System.err.println("Memory System Error : An outstanding request not found in the requesting element");
			System.exit(1);
		}
		
		ArrayList<CacheOutstandingRequestTableEntry> outstandingRequestList = receivingCache.outstandingRequestTable.get(blockAddr);
		
		while (!/*NOT*/outstandingRequestList.isEmpty())
		{
			if (outstandingRequestList.get(0).requestType == RequestType.MEM_READ)
			{
				//Pass the value to the waiting element
				//Create an event (BlockReadyEvent) for the waiting element
				//Generate the event for the Upper level cache or LSQ
				if (outstandingRequestList.get(0).lsqIndex == LSQ.INVALID_INDEX)
					//Generate the event for the Upper level cache
					newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker,  
							1, //noOfSlots,
							new BlockReadyEvent(outstandingRequestList.get(0).requestingElement.getLatencyDelay(),//FIXME 
															this.getProcessingElement(),
															outstandingRequestList.get(0).requestingElement, 
															0, //tieBreaker
															RequestType.MEM_BLOCK_READY,
															outstandingRequestList.get(0).address,
															lsqIndex)));
				else
					//Generate the event to tell the LSQ
					newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker, 
							1, //noOfSlots,
							new BlockReadyEvent(outstandingRequestList.get(0).requestingElement.getLatencyDelay(),//FIXME 
															this.getProcessingElement(),
															outstandingRequestList.get(0).requestingElement, 
															0, //tieBreaker
															RequestType.LSQ_LOAD_COMPLETE,
															outstandingRequestList.get(0).address,
															outstandingRequestList.get(0).lsqIndex)));
			}
			
			else if (outstandingRequestList.get(0).requestType == RequestType.MEM_WRITE)
			{
				//Write the value to the block (Do Nothing)
				//Pass the value to the waiting element
	/*			//Create an event (BlockReadyEvent) for the waiting element
				if (outstandingRequestList.get(0).lsqIndex != LSQ.INVALID_INDEX)
					//(If the requesting element is LSQ)
					//Generate the event to tell the LSQ
					newEventQueue.addEvent(new BlockReadyEvent(new Time_t(GlobalClock.getCurrentTime() +
																		outstandingRequestList.get(0).requestingElement.getLatency().getTime()),//FIXME 
															this.getProcessingElement(),
															outstandingRequestList.get(0).requestingElement, 
															0, //tieBreaker
															RequestType.LSQ_WRITE_COMMIT,
															outstandingRequestList.get(0).address,
															lsqIndex));
	*/			
				if (receivingCache.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
				{
					//Handle in any case (Whether requesting element is LSQ or cache)
					//TODO : handle write-value forwarding (for Write-Through and Coherent caches)
					if (receivingCache.isLastLevel)
						newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker, 
								1, //noOfSlots,
								new NewMainMemAccessEvent(MemorySystem.getMainMemLatencyDelay(),//FIXME :main memory latency is going to come here
																		receivingCache, 
																		0, //tieBreaker,
																		outstandingRequestList.get(0).address,
																		RequestType.MEM_WRITE)));
					else
						newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker, 
								1, //noOfSlots,
								new NewCacheAccessEvent(receivingCache.nextLevel.getLatencyDelay(),//FIXME
																		receivingCache,
																		receivingCache.nextLevel,
																		LSQ.INVALID_INDEX, 
																		0, //tieBreaker,
																		new CacheRequestPacket(RequestType.MEM_WRITE,
																							outstandingRequestList.get(0).address))));
				}			
			}
			else
			{
				System.err.println("Memory System Error : A request was of type other than MEM_READ or MEM_WRITE");
				System.exit(1);
			}
			
			//Remove the processed entry from the outstanding request list
			outstandingRequestList.remove(0);
		}
	}
	
	protected void receiveBlockAtLSQ(NewEventQueue newEventQueue, LSQ receivingLSQ)
	{
		if ((receivingLSQ.lsqueue[lsqIndex].getType() == LSQEntryType.LOAD) &&
				!receivingLSQ.lsqueue[lsqIndex].isForwarded())
		{
			receivingLSQ.lsqueue[lsqIndex].setForwarded(true);
			
			//No ports to be used in this event
			newEventQueue.addEvent(new ExecutionCompleteEvent(receivingLSQ.lsqueue[lsqIndex].getRobEntry(),
									-1,
									receivingLSQ.containingMemSys.core,
									this.getEventTime().getTime()));
		}/*
		else if (receivingLSQ.lsqueue[lsqIndex].getType() == LSQEntryType.STORE)
		{
			receivingLSQ.lsqueue[lsqIndex].setStoreCommitted(true);
			//TODO : Also to increment the head of the queue. Following code is from the LSQCommitEventFromROB
			/*
			processingLSQ.head = processingLSQ.incrementQ(processingLSQ.head);
			processingLSQ.curSize--;
			//long address = entry.getAddr();
			*
			//TODO : Commit the STORE entry in the LSQ and may be generate an event 
			//to tell the ROB or something
		}*/
			
	}
}
