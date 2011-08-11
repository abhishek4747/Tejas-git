package pipeline.outoforder;

import generic.Core;
import generic.GlobalClock;
import generic.NewEvent;
import generic.NewEventQueue;
import generic.RequestType;
import generic.Time_t;

public class WriteBackAttemptEvent extends NewEvent {
	
	Core core;
	RegisterFile tempRF;
	RenameTable tempRN;
	int tempDestPhyReg;
	ReorderBufferEntry reorderBufferEntry;
	int whichWBFlag;
	
	public WriteBackAttemptEvent(Core core,
								RegisterFile tempRF,
								RenameTable tempRN,
								int tempDestPhyReg,
								ReorderBufferEntry reorderBufferEntry,
								int whichWBFlag,
								long eventTime)
	{
		super(new Time_t(eventTime), null, null, 0, RequestType.WRITEBACK_ATTEMPT);
		
		this.core = core;
		this.tempRF = tempRF;
		this.tempRN = tempRN;
		this.tempDestPhyReg = tempDestPhyReg;
		this.reorderBufferEntry = reorderBufferEntry;
		this.whichWBFlag = whichWBFlag;		
	}

	@Override
	public void handleEvent(NewEventQueue newEventQueue) {
		
		//attempt to write-back
		long slotAvailableTime = tempRF.getPort().getNextSlot().getTime();
		if(slotAvailableTime <= GlobalClock.getCurrentTime())
		{
			//port to register file is available
			//occupying port
			tempRF.getPort().occupySlots(core.getRegFileOccupancy());
			//scheduling write-back complete event
			core.getEventQueue().addEvent(new WriteBackCompleteEvent(
																core,
																reorderBufferEntry,
																whichWBFlag,
																tempRF,
																tempRN,
																tempDestPhyReg,
																GlobalClock.getCurrentTime() + core.getRegFileOccupancy()
																));
		}
		else
		{
			//port to register file is not available
			core.getEventQueue().addEvent(new WriteBackAttemptEvent(
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
