package pipeline.outoforder;

import generic.Core;
import generic.GlobalClock;
import generic.SimulationElement;
import generic.Time_t;

/**
 * schedule the completion of decode of instructions that are read from the fetch buffer
 * in the current clock cycle
 */

public class DecodeLogic extends SimulationElement {

	//the containing core
	private Core core;
	
	public DecodeLogic(Core containingCore)
	{
		super(1, new Time_t(-1), new Time_t(-1), -1);
		core = containingCore;
	}
	
	public void scheduleDecodeCompletion(int threadID)
	{
		//decode completion of decodeWidth number of instructions scheduled
		
		core.getEventQueue().addEvent(
				new DecodeCompleteEvent(
						core,
						threadID,
						GlobalClock.getCurrentTime() + core.getDecodeTime()*core.getStepSize()
						));
	}
}