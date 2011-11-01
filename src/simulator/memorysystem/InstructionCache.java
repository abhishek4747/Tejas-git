package memorysystem;

import java.util.ArrayList;

import generic.Event;
import generic.EventQueue;
import generic.ExecCompleteEvent;
import generic.GlobalClock;
import generic.RequestType;
import config.CacheConfig;

public class InstructionCache extends Cache
{	
	public InstructionCache(CacheConfig cacheParameters, EventQueue eventQ) {
		super(cacheParameters, eventQ);
	}
	
	@Override
	protected void handleMemResponse(EventQueue eventQ, Event event)
	{
		long addr = ((AddressCarryingEvent)(event)).getAddress();
		
		long blockAddr = addr >>> this.blockSizeBits;
		if (!/*NOT*/this.missStatusHoldingRegister.containsKey(blockAddr))
		{
			System.err.println("Memory System Error : An outstanding request not found in the requesting element");
			System.exit(1);
		}
		
		ArrayList<Event> outstandingRequestList = this.missStatusHoldingRegister.get(blockAddr);
		
		while (!/*NOT*/outstandingRequestList.isEmpty())
		{				
			if (outstandingRequestList.get(0).getRequestType() == RequestType.Cache_Read)
			{
				//Pass the value to the waiting element
				//TODO Add the EXEC_COMPLETE_EVENT
				eventQ.addEvent(
						new ExecCompleteEvent(
								containingMemSys.core.getEventQueue(),
								GlobalClock.getCurrentTime(),
								null,
								containingMemSys.core.getExecEngine().getFetcher(),
								RequestType.EXEC_COMPLETE,
								null));
			}
			else
			{
				System.err.println("Instruction Cache Error : A request was of type other than Cache_Read");
				System.exit(1);
			}
			
			//Remove the processed entry from the outstanding request list
			outstandingRequestList.remove(0);
		}
	}
}
