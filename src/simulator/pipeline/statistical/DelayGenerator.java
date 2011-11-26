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
	public static long insCountIn = 0;
	public static long insCountOut = 0;
	public static int totalIns = 0;
	public static int numFwded = 0;
	
	static Random randomNumberGenerator = new Random();
	//Simulate the load-forwarding
	public static boolean forwardingDecision()
	{
		boolean toIssue = true;
		totalIns++;
//		Random randomNumberGenerator = new Random();
		if (randomNumberGenerator.nextInt(100) <= 20)
		{
			toIssue = false;
			numFwded++;
		}
		
		return toIssue;
	}
	
	//Schedule the instruction for a later point of time
	public static void scheduleAddressReady(Instruction instruction, Core core)
	{
		insCountIn++;
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
						core.getEventQueue(),
						lsqueue.getLatencyDelay() + getRandomDelay(), //FIXME : Add some delay
						null,
						lsqueue,
						RequestType.Tell_LSQ_Addr_Ready,
						lsqEntry));
	}
	
	public static int getRandomDelay()
	{
		int delay = 0;
		
//		Random randomNumberGenerator = new Random();
		return randomNumberGenerator.nextInt(10);
	}
}
