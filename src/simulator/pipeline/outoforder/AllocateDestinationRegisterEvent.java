package pipeline.outoforder;

import generic.Event;
import generic.Core;
import generic.Operand;
import generic.OperandType;

/**
 *handling of the event :
 *call allocatePhysicalRegister()
 *if obtained
 *		set physicalRegisterAllocated to true
 *else
 *		schedule new AllocateDestinationRegisterEvent at time current_clock+1
 */

public class AllocateDestinationRegisterEvent extends Event {
	
	RenameTable renameTable;
	ReorderBufferEntry reorderBufferEntry;
	Core core;
	
	public AllocateDestinationRegisterEvent(ReorderBufferEntry reorderBufferEntry,
										RenameTable renameTable,
										Core core,
										long eventTime)
	{
		super(eventTime,
				2,
				core.getExecEngine().getReorderBuffer()
				.getROB().indexOf(reorderBufferEntry));
		
		this.reorderBufferEntry = reorderBufferEntry;
		this.renameTable = renameTable;
		this.core = core;
	}

	//@Override
	public void handleEvent() {
		
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
			
			core.getEventQueue().addEvent(
					new RenameCompleteEvent(
							core,
							reorderBufferEntry,
							core.getClock() + core.getRenamingTime()
							));
			
			core.getExecEngine().setStallDecode(false);
		}
		
		else
		{
			long newEventTime = tempRF.getProducerROBEntry((int)tempOpnd.getValue()).getReadyAtTime();
			
			core.getEventQueue().addEvent(
					new AllocateDestinationRegisterEvent(
							reorderBufferEntry,
							null,
							core,
							newEventTime
							));
			
			core.getExecEngine().setStallDecode(true);
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
			core.getEventQueue().addEvent(
					new RenameCompleteEvent(
							core,
							reorderBufferEntry,
							core.getClock() + core.getRenamingTime()
							));
			core.getExecEngine().setStallDecode(false);
		}
		else
		{
			core.getEventQueue().addEvent(new AllocateDestinationRegisterEvent(
									reorderBufferEntry,
									renameTable,
									core,
									getEventTime()+1
									));
			core.getExecEngine().setStallDecode(true);
		}
	}
}