package pipeline.statistical;

import java.util.Random;

import generic.Core;
import generic.Instruction;
import generic.RequestType;
import generic.OperationType;

import memorysystem.LSQEntry;
import memorysystem.LSQ;
import memorysystem.LSQEntryContainingEvent;

public class DelayGenerator 
{
	//Simulate the load-forwarding
	public static boolean forwardingDecision()
	{
		boolean toIssue = true;
		
		Random randomNumberGenerator = new Random();
		if (randomNumberGenerator.nextInt(100) <= 20)
			toIssue = false;
		
		return toIssue;
	}
	
	//Schedule the instruction for a later point of time
	public static void scheduleAddressReady(Instruction instruction, Core core)
	{
		//TODO Schedule LD/ST instructions at a later point of time, based on a random number
		LSQ lsqueue = core.getStatisticalPipeline().coreMemSys.getLsqueue();
		
		boolean isLoad;
		if (instruction.getOperationType() == OperationType.load)
			isLoad = true;
		else
			isLoad = false;
		
		LSQEntry lsqEntry = lsqueue.addEntry(isLoad, instruction.getSourceOperand1().getValue(), null);
		
		lsqueue.getPort().put(
				new LSQEntryContainingEvent(
						lsqueue.getLatencyDelay(), //FIXME : Add some delay
						null,
						lsqueue,
						RequestType.Tell_LSQ_Addr_Ready,
						lsqEntry));
	}
}
