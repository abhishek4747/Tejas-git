package generic;

import pipeline.outoforder.ReorderBufferEntry;
import pipeline.outoforder.OpTypeToFUTypeMapping;
import pipeline.outoforder.RegisterFile;
import pipeline.outoforder.RenameTable;
import pipeline.outoforder.WakeUpLogic;
import pipeline.outoforder.WriteBackLogic;

/**
 * this event is scheduled at the clock_time at which an FU completes it's execution
 *
 * handling of the event :
 * if the execution has completed
 *	1) release FU - note : this is implicitly done
 *  2) set executed = true for the corresponding ROB entry
 *  3) set rename table / register file entry as valid
 *  4) wake up waiting instructions in the instruction window
 * else
 *	schedule new ExecutionCompleteEvent
 */

public class ExecutionCompleteEvent extends NewEvent {
	
	ReorderBufferEntry reorderBufferEntry;
	int threadID;
	int FUInstance;
	Core core;
	NewEventQueue eventQueue;
	
	public ExecutionCompleteEvent(ReorderBufferEntry reorderBufferEntry,
									int FUInstance,
									Core core,
									long eventTime)
	{
		super(new Time_t(eventTime),
				null,
				null,
				core.getExecEngine().getReorderBuffer().indexOf(reorderBufferEntry),
				RequestType.EXEC_COMPLETE	);
		
		this.reorderBufferEntry = reorderBufferEntry;
		this.threadID = reorderBufferEntry.getThreadID();
		this.FUInstance = FUInstance;
		this.core = core;
	}

	@Override
	public void handleEvent(NewEventQueue newEventQueue) {
		
		this.eventQueue = newEventQueue;
		
		if(reorderBufferEntry.getExecuted() == true)
		{
			System.out.println("already executed!");
			return;
		}
		
		if(reorderBufferEntry.getIssued() == false)
		{
			System.out.println("not yet issued, but execution complete");
			return;
		}
		/*
		if(core.getCoreMode() == CoreMode.CheckerSMT)
		{
			System.out.println("exec\n" + reorderBufferEntry);
		}*/
		
		RegisterFile tempRF = null;
		RenameTable tempRN = null;
		
		Operand tempOpnd = reorderBufferEntry.getInstruction().getDestinationOperand();
		if(tempOpnd == null)
		{
			if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg)
			{
				//doesn't use an FU				
				reorderBufferEntry.setExecuted(true);
				writeBackForXchg();
			}
			else
			{
				//writeBackForOthers(tempRF, tempRN);
				reorderBufferEntry.setExecuted(true);
				reorderBufferEntry.setWriteBackDone1(true);
				reorderBufferEntry.setWriteBackDone2(true);
			}
		}
		
		else
		{
			OperandType tempOpndType = tempOpnd.getOperandType(); 
			if(tempOpndType == OperandType.machineSpecificRegister)
			{
				tempRF = core.getExecEngine().getMachineSpecificRegisterFile(threadID);
				tempRN = null;
			}
			else if(tempOpndType == OperandType.integerRegister)
			{
				tempRF = core.getExecEngine().getIntegerRegisterFile();
				tempRN = core.getExecEngine().getIntegerRenameTable();
			}
			else if(tempOpndType == OperandType.floatRegister)
			{
				tempRF = core.getExecEngine().getFloatingPointRegisterFile();
				tempRN = core.getExecEngine().getFloatingPointRenameTable();
			}
			else
			{
				reorderBufferEntry.setExecuted(true);
				reorderBufferEntry.setWriteBackDone1(true);
				reorderBufferEntry.setWriteBackDone2(true);
				return;
			}
			
			if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.mov)
			{
				//doesn't use an FU			
				reorderBufferEntry.setExecuted(true);			
				writeBackForMov(tempRF, tempRN);
			}		
			else
			{
				writeBackForOthers(tempRF, tempRN);
			}
		}
		
	}
	
	void writeBackForMov(RegisterFile tempRF, RenameTable tempRN)
	{
		//wakeup waiting IW entries
		int tempDestPhyReg = reorderBufferEntry.getPhysicalDestinationRegister();
		OperandType tempOpndType = reorderBufferEntry.getInstruction().getDestinationOperand().getOperandType(); 
		WakeUpLogic.wakeUpLogic(core, tempOpndType, tempDestPhyReg);
		
		//attempt to write-back
		WriteBackLogic.writeBack(reorderBufferEntry, 3, tempRF, tempRN, tempDestPhyReg, core);
		
	}
	
	void writeBackForXchg()
	{
		RegisterFile tempRF = null;
		RenameTable tempRN = null;
		int phyReg;
		
		//operand 1
		phyReg = reorderBufferEntry.getOperand1PhyReg1();
		if(reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.machineSpecificRegister)
		{
			tempRF = core.getExecEngine().getMachineSpecificRegisterFile(threadID);
			WakeUpLogic.wakeUpLogic(core, OperandType.machineSpecificRegister, phyReg);
		}
		else if(reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.integerRegister)
		{
			tempRF = core.getExecEngine().getIntegerRegisterFile();
			tempRN = core.getExecEngine().getIntegerRenameTable();
			WakeUpLogic.wakeUpLogic(core, OperandType.integerRegister, phyReg);
		}
		else if(reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.floatRegister)
		{
			tempRF = core.getExecEngine().getFloatingPointRegisterFile();
			tempRN = core.getExecEngine().getFloatingPointRenameTable();
			WakeUpLogic.wakeUpLogic(core, OperandType.floatRegister, phyReg);
		}
		
		//attempt to write-back
		WriteBackLogic.writeBack(reorderBufferEntry, 1, tempRF, tempRN, phyReg, core);
		
		
		
		tempRF = null;
		tempRN = null;
		//operand 2
		phyReg = reorderBufferEntry.getOperand2PhyReg1();
		if(reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.machineSpecificRegister)
		{
			tempRF = core.getExecEngine().getMachineSpecificRegisterFile(threadID);
			WakeUpLogic.wakeUpLogic(core, OperandType.machineSpecificRegister, phyReg);
		}
		else if(reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.integerRegister)
		{
			tempRF = core.getExecEngine().getIntegerRegisterFile();
			tempRN = core.getExecEngine().getIntegerRenameTable();
			WakeUpLogic.wakeUpLogic(core, OperandType.integerRegister, phyReg);
		}
		else if(reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.floatRegister)
		{
			tempRF = core.getExecEngine().getFloatingPointRegisterFile();
			tempRN = core.getExecEngine().getFloatingPointRenameTable();
			WakeUpLogic.wakeUpLogic(core, OperandType.floatRegister, phyReg);
		}
		
		//attempt to write-back
		WriteBackLogic.writeBack(reorderBufferEntry, 2, tempRF, tempRN, phyReg, core);
		
	}
	
	void writeBackForOthers(RegisterFile tempRF, RenameTable tempRN)
	{
		//check if the execution has completed
		long time_of_completion = 0;
		if(reorderBufferEntry.getInstruction().getOperationType() != OperationType.load &&
				reorderBufferEntry.getInstruction().getOperationType() != OperationType.store)
		{
			time_of_completion = core.getExecEngine().getFunctionalUnitSet().getTimeWhenFUAvailable(
				OpTypeToFUTypeMapping.getFUType(reorderBufferEntry.getInstruction().getOperationType()),
				FUInstance );
		}
		
		if(time_of_completion <= GlobalClock.getCurrentTime())
			//this condition will always evaluate to true, if instruction is a load or a store
			//actually, stores don't have executioncompleteEvents scheduled - they are included just in case
		{
			//execution complete
			reorderBufferEntry.setExecuted(true);
			
			int tempDestPhyReg = reorderBufferEntry.getPhysicalDestinationRegister();
			//wakeup waiting IW entries
			if(tempRF != null || tempRN != null)
			{
				//there may some instruction that needs to be woken up
				OperandType tempOpndType = reorderBufferEntry.getInstruction().getDestinationOperand().getOperandType(); 
				WakeUpLogic.wakeUpLogic(core, tempOpndType, tempDestPhyReg);
			}
			
			//attempt to write-back
			WriteBackLogic.writeBack(reorderBufferEntry, 3, tempRF, tempRN, tempDestPhyReg, core);			
			
		}
		
		else
		{			
			reorderBufferEntry.setReadyAtTime(time_of_completion);
			
			//schedule new ExecutionCompleteEvent
			/*this.eventQueue.addEvent(
					new ExecutionCompleteEvent(
							reorderBufferEntry,
							FUInstance,
							core,
							time_of_completion ) );
			*/
			this.setEventTime(new Time_t(time_of_completion));
			this.eventQueue.addEvent(this);
		}
	}
	
		
	

}