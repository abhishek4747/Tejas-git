package memorysystem.snoopyCoherence;

import java.util.ArrayList;
import java.util.Stack;

import generic.*;

import generic.RequestType;
import memorysystem.*;
import memorysystem.BusOld.BusReqType;

public class Bus
{
	protected BusController busController;
	protected Cache requestingCache;
	protected RequestType requestType;
	protected long address;
	protected CacheLine sourceLine;
	
	protected int snoopingCoresProcessed = 0;
//	protected int copiesFound = 0;
//
//	protected CacheLine singleFoundCopy = null;
//	protected Cache cacheContainingTheCopy = null;
//	
//	protected boolean blockRWITM = false;
//	
//	protected ArrayList<BusRequestQElmnt> reqQueue = 
//		new ArrayList<BusRequestQElmnt>();
//	
	protected boolean isLocked = false;
	
	public enum BusReqType{
		INVALIDATE,	//Broadcast INVALID (Happens on a write hit if state is SHARED)
		RWITM,		//Broadcast "Read With Intent To Modify" (Happens on a write miss)
		MEM_ACCESS	//Memory (or lower level cache) access request (Happens on a read miss)
	}

	private Cache getBank(long address)
	{
		return this.busController.lowerCache;
	}
	
	/**
	 * Used to add a new request to the bus. A new request can be added only if 
	 * there is no request already for the same cycle
	 * @param _requestType : What type of request this is?
	 * @param _sourceCache : Which cache sent the request?
	 * @return boolean value to tell whether the request added successfully or not
	 */
	public void putRequest(Cache _requestingCache,
							CacheLine _sourceLine,
							RequestType _requestType, 
							long _address)
	{
		requestingCache = _requestingCache;
		sourceLine = _sourceLine;
		requestType = _requestType;
		address = _address;
//		snoopingCoresProcessed = 0;
//		copiesFound = 0;
//		singleFoundCopy = null;
//		cacheContainingTheCopy = null;
//		blockRWITM = false;
	}
	
//	public static void processRequest()
//	{
//		switch (requestType)
//		{
//		case INVALIDATE:
//			for (int i = 0; i < upperLevels.size(); i++)
//			{
//				if (upperLevels.get(i) != sourceCache)
//					MemEventQueue.eventQueue.add(new InvalidateEvent(upperLevels.get(i),
//																	address,
//																	sourceCache,
//																	sourceLine,
//																	MemEventQueue.clock +
//																	upperLevels.get(i).getLatency()));
//			}
//			break;
//			
//		case RWITM:
//			for (int i = 0; i < upperLevels.size(); i++)
//			{
//				if (upperLevels.get(i) != sourceCache)
//					MemEventQueue.eventQueue.add(new SearchLineCopiesEvent(requestingThreadID,
//																		upperLevels.get(i),
//																		address,
//																		sourceCache,
//																		MemEventQueue.clock +
//																		upperLevels.get(i).getLatency()));
//			}
//			break;
//			
//		case MEM_ACCESS:
//			for (int i = 0; i < upperLevels.size(); i++)
//			{
//				if (upperLevels.get(i) != sourceCache)
//					MemEventQueue.eventQueue.add(new SearchLineCopiesEvent(requestingThreadID,
//																		upperLevels.get(i),
//																		address,
//																		sourceCache,
//																		MemEventQueue.clock +
//																		upperLevels.get(i).getLatency()));
//			}
//			break;
//		}
//	}
//	
//	/**
//	 * Just used to reset the Bus to a new request (if there is any) in the queue
//	 * or to just free the lock on the bus so that the new requests can be added
//	 */
//	public static void endRequest()
//	{
//		MemEventQueue.eventQueue.add(new FillCacheStackEvent(requestingThreadID,
//															lsqEntry,
//															cacheFillStack,
//															sourceLine.getState(),
//															cacheFillStack.peek().cache.getLatency()));
//		
//		if (reqQueue.isEmpty())
//			isLocked = false;//Free the lock
//		else
//		{
//			//Add the new request to the Bus and process it
//			BusRequestQElmnt nextRequestElmnt = reqQueue.remove(0);
//			newRequest(nextRequestElmnt.requestingThreadID,
//						nextRequestElmnt.requestType, 
//						nextRequestElmnt.address, 
//						nextRequestElmnt.sourceCache, 
//						nextRequestElmnt.sourceLine, 
//						nextRequestElmnt.cacheFillStack,
//						nextRequestElmnt.lsqEntry);
//			processRequest();
//		}
//	}
}
