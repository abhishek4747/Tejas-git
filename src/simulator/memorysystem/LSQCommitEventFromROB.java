package memorysystem;

import java.util.Stack;

import generic.*;

public class LSQCommitEventFromROB extends NewEvent
{
	int lsqIndex;
	
	public LSQCommitEventFromROB(Time_t eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker,
			RequestType requestType, int lsqIndex) 
	{
		super(eventTime, requestingElement, processingElement, tieBreaker,
				requestType);
		this.lsqIndex = lsqIndex;
	}

	public void handleEvent(NewEventQueue newEventQueue)
	{
		LSQ processingLSQ = (LSQ)(this.getProcessingElement());
		
		//Check the error condition
		if (lsqIndex != processingLSQ.head)
		{
			System.err.println("Error in LSQ :  ROB sent commit for an instruction other than the one at the head");
			System.exit(1);
		}
		
//TODO : This needs to be moved some place especially for the store when it finally commits()
		// advance the head of the queue
		LSQEntry entry = processingLSQ.lsqueue[lsqIndex];
		
		// if it is a store, send the request to the cache
		if(entry.getType() == LSQEntry.LSQEntryType.STORE) 
		{
			//TODO Write to the cache
			CacheRequestPacket request = new CacheRequestPacket();
			//request.setThreadID(0);
			request.setType(RequestType.MEM_WRITE);
			request.setAddr(processingLSQ.lsqueue[lsqIndex].getAddr());
			newEventQueue.addEvent(new PortRequestEvent(0, //tieBreaker, 
					RequestType.PORT_REQUEST, 
					1, //noOfSlots,
					new NewCacheAccessEvent(processingLSQ.containingMemSys.l1Cache.getLatencyDelay(), //FIXME
															processingLSQ,
															processingLSQ.containingMemSys.l1Cache,
															lsqIndex, 
															0, //tieBreaker,
															request)));
			
			processingLSQ.head = processingLSQ.incrementQ(processingLSQ.head);
			processingLSQ.curSize--;
		}
		
		//If it is a LOAD which has received its value
		else if (entry.isForwarded())
		{
			processingLSQ.head = processingLSQ.incrementQ(processingLSQ.head);
			processingLSQ.curSize--;
			//long address = entry.getAddr();
		}
		
		//If it is a LOAD which has not yet received its value
		else
		{
			//TODO Uncomment these in the real system
			//Global.commitErrors++;
			System.err.println("Error in LSQ " +processingLSQ.containingMemSys.coreID+ " :  ROB sent commit for a load which has not received its value");
			System.exit(1);
		}
	}
}
