package pipeline.outoforder;

import generic.Core;
import generic.GlobalClock;
import generic.NewEventQueue;

public class WriteBackLogic {
	
	public static void writeBack (
			ReorderBufferEntry reorderBufferEntry,
			int whichWBFlag,
			RegisterFile tempRF,
			RenameTable tempRN,
			int tempDestPhyReg,
			Core core)
	{
		NewEventQueue eventQueue = core.getEventQueue();
		
		if(//reorderBufferEntry.isOperand1Available &&
				//reorderBufferEntry.isOperand2Available &&
				reorderBufferEntry.getExecuted() == true)
		{
			if(tempRF != null)
			{
				long slotAvailableTime = tempRF.getPort().getNextSlot();
				if(slotAvailableTime <= GlobalClock.getCurrentTime())
				{
					//port to register file is available
					//occupying port
					tempRF.getPort().occupySlots(1, core.getStepSize());
					//scheduling write-back complete event
					eventQueue.addEvent(new WriteBackCompleteEvent(
																		core,
																		reorderBufferEntry,
																		whichWBFlag,
																		tempRF,
																		tempRN,
																		tempDestPhyReg,
																		GlobalClock.getCurrentTime() + core.getRegFileOccupancy()*core.getStepSize()
																		));
				}
				else
				{
					//port to register file is not available
					eventQueue.addEvent(new WriteBackAttemptEvent(
																		core,
																		tempRF,
																		tempRN,
																		tempDestPhyReg,																
																		reorderBufferEntry,
																		whichWBFlag,
																		GlobalClock.getCurrentTime() + slotAvailableTime));
				}
			}
		}
		else
		{
			
		}
	}


}