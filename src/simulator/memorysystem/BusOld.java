/*****************************************************************************
				Tejas Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
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

import java.util.*;

import memorysystem.CacheLine.MESI;

public class BusOld
{
	protected static ArrayList<Cache> upperLevels = new ArrayList<Cache>();
	protected static Cache lowerLevel;
	
	protected static int requestingThreadID;
	
	protected static BusReqType requestType;
	protected static long address;
	protected static Cache sourceCache;
	protected static CacheLine sourceLine;
	protected static Stack<CacheFillStackEntry> cacheFillStack;
	protected static LSQEntry lsqEntry;
	//protected static int latency;
	
	protected static int snoopingCoresProcessed = 0;
	protected static int copiesFound = 0;

	protected static CacheLine singleFoundCopy = null;
	protected static Cache cacheContainingTheCopy = null;
	
	protected static boolean blockRWITM = false;
	
	protected static ArrayList<BusRequestQElmnt> reqQueue = 
		new ArrayList<BusRequestQElmnt>();
	
	protected static boolean isLocked = false;
	
	public static enum BusReqType{
		INVALIDATE,	//Broadcast INVALID (Happens on a write hit if state is SHARED)
		RWITM,		//Broadcast "Read With Intent To Modify" (Happens on a write miss)
		MEM_ACCESS	//Memory (or lower level cache) access request (Happens on a read miss)
	}
	
	/**
	 * Used to add a new request to the bus. A new request can be added only if 
	 * there is no request already for the same cycle
	 * @param _requestType : What type of request this is?
	 * @param _sourceCache : Which cache sent the request?
	 * @return boolean value to tell whether the request added successfully or not
	 */
	public static void newRequest(int _requestingThreadID,
								BusReqType _requestType, 
								long _address,
								Cache _sourceCache,
								CacheLine _sourceLine,
								Stack<CacheFillStackEntry> _cacheFillStack,
								LSQEntry _lsqEntry)
	{
		requestingThreadID = _requestingThreadID;
		requestType = _requestType;
		address = _address;
		sourceCache  = _sourceCache;
		sourceLine = _sourceLine;
		cacheFillStack = _cacheFillStack;
		lsqEntry = _lsqEntry;
		snoopingCoresProcessed = 0;
		copiesFound = 0;
		singleFoundCopy = null;
		cacheContainingTheCopy = null;
		blockRWITM = false;
	}
	
	public static void processRequest()
	{
		switch (requestType)
		{
		case INVALIDATE:
			for (int i = 0; i < upperLevels.size(); i++)
			{
				if (upperLevels.get(i) != sourceCache)
					MemEventQueue.eventQueue.add(new InvalidateEvent(upperLevels.get(i),
																	address,
																	sourceCache,
																	sourceLine,
																	MemEventQueue.clock +
																	upperLevels.get(i).getLatency()));
			}
			break;
			
		case RWITM:
			for (int i = 0; i < upperLevels.size(); i++)
			{
				if (upperLevels.get(i) != sourceCache)
					MemEventQueue.eventQueue.add(new SearchLineCopiesEvent(requestingThreadID,
																		upperLevels.get(i),
																		address,
																		sourceCache,
																		MemEventQueue.clock +
																		upperLevels.get(i).getLatency()));
			}
			break;
			
		case MEM_ACCESS:
			for (int i = 0; i < upperLevels.size(); i++)
			{
				if (upperLevels.get(i) != sourceCache)
					MemEventQueue.eventQueue.add(new SearchLineCopiesEvent(requestingThreadID,
																		upperLevels.get(i),
																		address,
																		sourceCache,
																		MemEventQueue.clock +
																		upperLevels.get(i).getLatency()));
			}
			break;
		}
	}
	
	/**
	 * Just used to reset the Bus to a new request (if there is any) in the queue
	 * or to just free the lock on the bus so that the new requests can be added
	 */
	public static void endRequest()
	{
		MemEventQueue.eventQueue.add(new FillCacheStackEvent(requestingThreadID,
															lsqEntry,
															cacheFillStack,
															sourceLine.getState(),
															cacheFillStack.peek().cache.getLatency()));
		
		if (reqQueue.isEmpty())
			isLocked = false;//Free the lock
		else
		{
			//Add the new request to the Bus and process it
			BusRequestQElmnt nextRequestElmnt = reqQueue.remove(0);
			newRequest(nextRequestElmnt.requestingThreadID,
						nextRequestElmnt.requestType, 
						nextRequestElmnt.address, 
						nextRequestElmnt.sourceCache, 
						nextRequestElmnt.sourceLine, 
						nextRequestElmnt.cacheFillStack,
						nextRequestElmnt.lsqEntry);
			processRequest();
		}
	}
}
