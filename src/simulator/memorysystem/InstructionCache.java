package memorysystem;

import java.util.ArrayList;

import pipeline.statistical.DelayGenerator;

import generic.Event;
import generic.EventQueue;
import generic.ExecCompleteEvent;
import generic.GlobalClock;
import generic.RequestType;
import config.CacheConfig;

public class InstructionCache extends Cache
{	
	public InstructionCache(CacheConfig cacheParameters, CoreMemorySystem containingMemSys) {
		super(cacheParameters, containingMemSys);
	}
	
	@Override
	protected void handleMemResponse(EventQueue eventQ, Event event)
	{
		long addr = ((AddressCarryingEvent)(event)).getAddress();
		
		CacheLine evictedLine = this.fill(addr, MESI.EXCLUSIVE);
		if (evictedLine != null)
		{
			if (this.isLastLevel)
				MemorySystem.mainMemory.getPort().put(
						new AddressCarryingEvent(
								eventQ,
								MemorySystem.mainMemory.getLatencyDelay(),
								this, 
								MemorySystem.mainMemory,
								RequestType.Main_Mem_Write,
								evictedLine.getTag() << this.blockSizeBits));
			else
				this.nextLevel.getPort().put(
						new AddressCarryingEvent(
								eventQ,
								this.nextLevel.getLatencyDelay(),
								this,
								this.nextLevel,
								RequestType.Cache_Write,
								evictedLine.getTag() << this.blockSizeBits));
		}
		
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
				if (!containingMemSys.getCore().isPipelineStatistical)
					if (!containingMemSys.getCore().isPipelineInorder)
						eventQ.addEvent(
								outstandingRequestList.get(0).update(
										eventQ,
										GlobalClock.getCurrentTime(),
										this,
										containingMemSys.getCore().getExecEngine().getFetcher(),
										RequestType.Mem_Response));
					else
						outstandingRequestList.get(0).getRequestingElement().getPort().put(
								outstandingRequestList.get(0).update(
										eventQ,
										0,
										this,
										outstandingRequestList.get(0).getRequestingElement(),
										RequestType.Mem_Response));
				else
					DelayGenerator.insCountOut++;
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
