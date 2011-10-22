package pipeline.statistical;

import generic.Core;
import generic.Instruction;

public class DelayGenerator 
{
	public static boolean forwardingDecision()
	{
		return true;
	}
	
	public static void scheduleAddressReady(Instruction instruction, Core core)
	{
		//TODO Schedule LD/ST instructions at a later point of time, based on a random number
		
	}
}
