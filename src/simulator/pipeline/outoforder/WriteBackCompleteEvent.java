package pipeline.outoforder;

import config.SimulationConfig;
import generic.Core;
import generic.GlobalClock;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.Time_t;

public class WriteBackCompleteEvent extends Event {
	
	Core core;
	RegisterFile tempRF;
	RenameTable tempRN;
	int tempDestPhyReg;
	ReorderBufferEntry reorderBufferEntry;
	int whichWBFlag;						//if 1, refers to 1st operand of an xchg operation - given by source operand 1
											//		writeBackDone1 flag is updated
											//if 2, refers to 2nd operand of an xchg operation - given by source operand 2
											//		writeBackDone2 flag is updated
											//if 3, refers to destination operand of all other operations
											//		writeBackDone1 flag is updated AND
											//		writeBackDone2 flag is updated
	
	public WriteBackCompleteEvent(
									Core core,
									ReorderBufferEntry reorderBufferEntry,
									int whichWBFlag,
									RegisterFile tempRF,
									RenameTable tempRN,
									int tempDestPhyReg,
									long eventTime )
	{
		super(new Time_t(eventTime), null, null, core.getExecEngine().getReorderBuffer().indexOf(reorderBufferEntry), RequestType.WRITEBACK_COMPLETE);
		
		this.core = core;
		this.tempRF = tempRF;
		this.tempRN = tempRN;
		this.tempDestPhyReg = tempDestPhyReg;
		this.reorderBufferEntry = reorderBufferEntry;
		this.whichWBFlag = whichWBFlag;		
	}

	@Override
	public void handleEvent(EventQueue eventQueue) {
		
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

		if(SimulationConfig.debugMode)
		{
			System.out.println("wb : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + reorderBufferEntry.getInstruction());
		}

	}

}
