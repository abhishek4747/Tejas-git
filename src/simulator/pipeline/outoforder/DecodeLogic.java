package pipeline.outoforder;

import generic.Core;

/**
 * schedule the completion of decode of instructions that are read from the fetch buffer
 * in the current clock cycle
 */

public class DecodeLogic {

	//the containing core
	private Core core;
	
	public DecodeLogic(Core containingCore)
	{
		core = containingCore;
	}
	
	public void scheduleDecodeCompletion()
	{
		//decode completion of decodeWidth number of instructions scheduled
		
		core.getEventQueue().addEvent(
				new DecodeCompleteEvent(
						core,
						core.getClock() + core.getDecodeTime()
						));
	}
}