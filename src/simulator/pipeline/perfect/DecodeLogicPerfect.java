package pipeline.perfect;

import generic.Core;
import generic.GlobalClock;
import generic.SimulationElement;
import generic.Time_t;

/**
 * schedule the completion of decode of instructions that are read from the fetch buffer
 * in the current clock cycle
 */

public class DecodeLogicPerfect extends SimulationElement {

	//the containing core
	private Core core;
	
	public DecodeLogicPerfect(Core containingCore)
	{
		super(1, new Time_t(-1), new Time_t(-1), -1);
		core = containingCore;
	}
	
	public void scheduleDecodeCompletion(int threadID)
	{
		//decode completion of decodeWidth number of instructions scheduled
		
		core.getEventQueue().addEvent(
				new DecodeCompleteEventPerfect(
						core,
						threadID,
						GlobalClock.getCurrentTime() + core.getDecodeTime()*core.getStepSize()
						));
	}
}