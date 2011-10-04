package pipeline.outoforder;

import config.SimulationConfig;
import generic.GlobalClock;
import generic.Event;
import generic.Core;
import generic.EventQueue;
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

public class AllocateDestinationRegisterEvent extends Event {
	
	RenameTable renameTable;
	ReorderBufferEntry reorderBufferEntry;
	int threadID;
	Core core;
	EventQueue eventQueue;
	
	public AllocateDestinationRegisterEvent(ReorderBufferEntry reorderBufferEntry,
										RenameTable renameTable,
										Core core,
										long eventTime)
	{
		super(new Time_t(eventTime),
				null,
				null,
				(long) core.getExecEngine().getReorderBuffer().indexOf(reorderBufferEntry),
				RequestType.ALLOC_DEST_REG);
		
		this.reorderBufferEntry = reorderBufferEntry;
		this.threadID = reorderBufferEntry.getThreadID();
		this.renameTable = renameTable;
		this.core = core;
	}

	//@Override
	public void handleEvent(EventQueue eventQueue) {
		

		if(SimulationConfig.debugMode)
		{
			System.out.println("alloc : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + reorderBufferEntry.getInstruction());
		}
		
		this.eventQueue = eventQueue;
		
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
		RegisterFile tempRF = core.getExecEngine().getMachineSpecificRegisterFile(threadID);
		Operand tempOpnd = reorderBufferEntry.getInstruction().getDestinationOperand();
					
		int destPhyReg = (int) tempOpnd.getValue();
		
		if(tempRF.getValueValid(destPhyReg) == true)
				//|| tempRF.getProducerROBEntry((int)tempOpnd.getValue()).isWriteBackDone() == true)
		{
			reorderBufferEntry.setPhysicalDestinationRegister(destPhyReg);
			
			tempRF.setProducerROBEntry(reorderBufferEntry, destPhyReg);
			tempRF.setValueValid(false, destPhyReg);
			
			core.getExecEngine().setStallDecode1(false);
			
			this.eventQueue.addEvent(
					new RenameCompleteEvent(
							core,
							reorderBufferEntry,
							GlobalClock.getCurrentTime() + core.getRenamingTime()*core.getStepSize()
							));
			
			
		}
		
		else
		{
			long newEventTime = tempRF.getProducerROBEntry((int)tempOpnd.getValue()).getReadyAtTime();
			
			core.getExecEngine().setStallDecode1(true);
			
			/*this.eventQueue.addEvent(
					new AllocateDestinationRegisterEvent(
							reorderBufferEntry,
							null,
							core,
							newEventTime
							));
			*/
			if(newEventTime <= GlobalClock.getCurrentTime())
				this.getEventTime().setTime(GlobalClock.getCurrentTime() + core.getStepSize());
			else
				this.getEventTime().setTime(newEventTime);
			
			this.eventQueue.addEvent(this);
		}
	}
	
	void handleIntFloat()
	{
		Operand tempOpnd = reorderBufferEntry.getInstruction().getDestinationOperand();
		int r = renameTable.allocatePhysicalRegister(threadID, (int) tempOpnd.getValue());
		
		if(r >= 0)
		{
			reorderBufferEntry.setPhysicalDestinationRegister(r);
			renameTable.setProducerROBEntry(reorderBufferEntry, r);
			renameTable.setValueValid(false, r);
			
			core.getExecEngine().setStallDecode1(false);
			
			this.eventQueue.addEvent(
					new RenameCompleteEvent(
							core,
							reorderBufferEntry,
							GlobalClock.getCurrentTime() + core.getRenamingTime()*core.getStepSize()
							));
		}
		else
		{
			core.getExecEngine().setStallDecode1(true);
			/*this.eventQueue.addEvent(
					new AllocateDestinationRegisterEvent(
									reorderBufferEntry,
									renameTable,
									core,
									getEventTime().getTime()+1
									));*/
			//this.setEventTime(new Time_t(getEventTime().getTime()+1));
			this.getEventTime().setTime(GlobalClock.getCurrentTime() + core.getStepSize());
			this.eventQueue.addEvent(this);
		}
	}
}