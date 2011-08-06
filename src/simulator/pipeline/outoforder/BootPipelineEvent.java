package pipeline.outoforder;

import emulatorinterface.communication.IPCBase;
import generic.Core;
import generic.GlobalClock;
import generic.NewEvent;
import generic.NewEventQueue;
import generic.RequestType;
import generic.Time_t;

public class BootPipelineEvent extends NewEvent {

	Core[] cores;
	IPCBase ipcBase;
	NewEventQueue eventQueue;
	
	public BootPipelineEvent(Core[] cores, IPCBase ipcBase, NewEventQueue eventQueue, long eventTime)
	{
		super(
				new Time_t(eventTime),
				null,
				null,
				0,
				RequestType.BOOT_PIPELINE
			);
		
		this.cores = cores;
		this.ipcBase = ipcBase;
		this.eventQueue = eventQueue;
	}
	@Override
	public void handleEvent(NewEventQueue newEventQueue) {
		
		for(int i = 0; i < ipcBase.MAXNUMTHREADS; i++)
		{
			if(ipcBase.getReaderThreads()[i].getInputToPipeline().isEmpty() == false)
			{
				for(int j = 0; j < 1; j++)//TODO number_of_cores
				{
					cores[j].boot();
				}
				return;
			}
		}
		
		GlobalClock.setCurrentTime(0);
		this.eventQueue.addEvent(this);

	}

}
