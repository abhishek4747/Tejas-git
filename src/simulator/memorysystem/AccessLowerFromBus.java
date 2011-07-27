package memorysystem;

import generic.MemoryAccessType;

import java.util.Stack;

import config.SystemConfig;

import memorysystem.CacheLine.MESI;

public class AccessLowerFromBus
{	
	public static void access(int threadID, Cache sourceCache, MESI stateToSet)
	{	
		CacheRequestPacket request = new CacheRequestPacket();
		request.setThreadID(threadID);
		request.setAddr(Bus.address);
		request.setType(MemoryAccessType.READ);
		Stack<CacheFillStackEntry> cacheFillStack = new Stack<CacheFillStackEntry>();
		cacheFillStack.add(new CacheFillStackEntry(sourceCache, request));
		
		if (sourceCache.isLastLevel)
		{
			MemEventQueue.eventQueue/*.get(threadID)*/.add(new MainMemAccessEvent(threadID,
																				cacheFillStack,
																				stateToSet,
																				MemEventQueue.clock
																				+ SystemConfig.mainMemoryLatency));
		}
		else
		{
			MemEventQueue.eventQueue/*.get(threadID)*/.add(new CacheAccessEvent(threadID,
																				sourceCache.nextLevel,
																				request,
																				stateToSet,
																				cacheFillStack,
																				MemEventQueue.clock
																				+ sourceCache.nextLevel.getLatency()));
		}
	}
}
