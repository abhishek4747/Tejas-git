package pipeline.outoforder;

import generic.Core;
import generic.NewEvent;
import generic.NewEventQueue;
import generic.RequestType;
import generic.Time_t;

public class WriteBackCompleteEvent extends NewEvent {
	
	Core core;
	RegisterFile tempRF;
	RenameTable tempRN;
	int tempDestPhyReg;
	ReorderBufferEntry reorderBufferEntry;
	int whichWBFlag;
	
	public WriteBackCompleteEvent(
									Core core,
									ReorderBufferEntry reorderBufferEntry,
									int whichWBFlag,
									RegisterFile tempRF,
									RenameTable tempRN,
									int tempDestPhyReg,
									long eventTime )
	{
		super(new Time_t(eventTime), null, null, 0, RequestType.WRITEBACK_COMPLETE);
		
		this.core = core;
		this.tempRF = tempRF;
		this.tempRN = tempRN;
		this.tempDestPhyReg = tempDestPhyReg;
		this.reorderBufferEntry = reorderBufferEntry;
		this.whichWBFlag = whichWBFlag;		
	}

	@Override
	public void handleEvent(NewEventQueue newEventQueue) {
		
		//update register file, rename table
		if(tempRN != null)
		{
			tempRN.setValueValid(true, tempDestPhyReg);
			tempRN.setProducerROBEntry(null, tempDestPhyReg);
		}
		else
		{
			tempRF.setValueValid(true, tempDestPhyReg);
			tempRF.setProducerROBEntry(null, tempDestPhyReg);
		}
		
		//set writeback flag
		if(whichWBFlag == 1)
			reorderBufferEntry.setWriteBackDone1(true);
		else if(whichWBFlag == 2)
			reorderBufferEntry.setWriteBackDone2(true);
		else
		{
			reorderBufferEntry.setWriteBackDone1(true);
			reorderBufferEntry.setWriteBackDone2(true);
		}
		
		//wakeup
		//it is possible that a consumer instruction entered
		//after the producer instruction completed execution, but before it completed write-back
		if(whichWBFlag == 1)
		{
			WakeUpLogic.wakeUpLogic(core, reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType(), tempDestPhyReg);
		}
		else if(whichWBFlag == 2)
		{
			WakeUpLogic.wakeUpLogic(core, reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType(), tempDestPhyReg);
		}
		else
		{
			WakeUpLogic.wakeUpLogic(core, reorderBufferEntry.getInstruction().getDestinationOperand().getOperandType(), tempDestPhyReg);
		}

	}

}
