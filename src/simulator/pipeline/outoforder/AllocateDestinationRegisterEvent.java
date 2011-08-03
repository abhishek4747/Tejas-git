package pipeline.outoforder;

import generic.GlobalClock;
import generic.NewEvent;
import generic.Core;
import generic.NewEventQueue;
import generic.Operand;
import generic.OperandType;
import generic.RequestType;
import generic.Time_t;

/**
 *handling of the event :
 *call allocatePhysicalRegister()
 *if obtained
 *		set physicalRegisterAllocated to true
 *else
 *		schedule new AllocateDestinationRegisterEvent at time current_clock+1
 */

public class AllocateDestinationRegisterEvent extends NewEvent {
	
	RenameTable renameTable;
	ReorderBufferEntry reorderBufferEntry;
	Core core;
	NewEventQueue eventQueue;
	
	public AllocateDestinationRegisterEvent(ReorderBufferEntry reorderBufferEntry,
										RenameTable renameTable,
										Core core,
										long eventTime)
	{
		super(new Time_t(eventTime),
				null,
				null,
				(long) core.getExecEngine().getReorderBuffer()
				.getROB().indexOf(reorderBufferEntry),
				RequestType.ALLOC_DEST_REG);
		
		this.reorderBufferEntry = reorderBufferEntry;
		this.renameTable = renameTable;
		this.core = core;
	}

	//@Override
	public void handleEvent(NewEventQueue newEventQueue) {
		
		this.eventQueue = newEventQueue;
		
		OperandType tempOpndType = reorderBufferEntry.getInstruction().
									getDestinationOperand().getOperandType();		
		
		if(tempOpndType == OperandType.machineSpecificRegister)
		{
			handleMSR();			
		}
		
		else
		{
			//destination is an integer register or a floating point register
			handleIntFloat();
		}
	}
	
	void handleMSR()
	{
		RegisterFile tempRF = core.getExecEngine().getMachineSpecificRegisterFile();
		Operand tempOpnd = reorderBufferEntry.getInstruction().getDestinationOperand();
					
		if(tempRF.getValueValid((int)tempOpnd.getValue()) == true ||
				tempRF.getProducerROBEntry((int)tempOpnd.getValue()).getExecuted() == true)
		{
			reorderBufferEntry.setPhysicalDestinationRegister((int) tempOpnd.getValue());
			
			core.getExecEngine().setStallDecode1(false);
			
			this.eventQueue.addEvent(
					new RenameCompleteEvent(
							core,
							reorderBufferEntry,
							GlobalClock.getCurrentTime() + core.getRenamingTime()
							));
			
			
		}
		
		else
		{
			long newEventTime = tempRF.getProducerROBEntry((int)tempOpnd.getValue()).getReadyAtTime();
			
			core.getExecEngine().setStallDecode1(true);
			
			this.eventQueue.addEvent(
					new AllocateDestinationRegisterEvent(
							reorderBufferEntry,
							null,
							core,
							newEventTime
							));
		}
	}
	
	void handleIntFloat()
	{
		Operand tempOpnd = reorderBufferEntry.getInstruction().getDestinationOperand();
		int r = renameTable.allocatePhysicalRegister((int) tempOpnd.getValue());
		
		if(r >= 0)
		{
			reorderBufferEntry.setPhysicalDestinationRegister(r);
			renameTable.setProducerROBEntry(reorderBufferEntry, r);
			core.getExecEngine().setStallDecode1(false);
			
			this.eventQueue.addEvent(
					new RenameCompleteEvent(
							core,
							reorderBufferEntry,
							GlobalClock.getCurrentTime() + core.getRenamingTime()
							));
		}
		else
		{
			core.getExecEngine().setStallDecode1(true);
			this.eventQueue.addEvent(
					new AllocateDestinationRegisterEvent(
									reorderBufferEntry,
									renameTable,
									core,
									getEventTime().getTime()+1
									));
		}
	}
}