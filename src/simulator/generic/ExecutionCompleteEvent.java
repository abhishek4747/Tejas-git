package generic;

import pipeline.outoforder.ReorderBufferEntry;
import pipeline.outoforder.OpTypeToFUTypeMapping;
import pipeline.outoforder.RegisterFile;
import pipeline.outoforder.RenameTable;
import java.util.LinkedList;
import pipeline.outoforder.IWEntry;

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
				core.getExecEngine().getReorderBuffer()
					.getROB().indexOf(reorderBufferEntry),
				RequestType.EXEC_COMPLETE	);
		
		this.reorderBufferEntry = reorderBufferEntry;
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
				writeBackForOthers(tempRF, tempRN);
			}
		}
		
		else
		{
			OperandType tempOpndType = tempOpnd.getOperandType(); 
			if(tempOpndType == OperandType.machineSpecificRegister)
			{
				tempRF = core.getExecEngine().getMachineSpecificRegisterFile();
			}
			else if(tempOpndType == OperandType.integerRegister)
			{
				tempRN = core.getExecEngine().getIntegerRenameTable();
			}
			else if(tempOpndType == OperandType.floatRegister)
			{
				tempRN = core.getExecEngine().getFloatingPointRenameTable();
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
		int tempDestPhyReg = reorderBufferEntry.getPhysicalDestinationRegister();
		if(tempRF != null)
		{
			tempRF.setValueValid(true, tempDestPhyReg);
		}
		else if(tempRN != null)
		{
			tempRN.setValueValid(true, tempDestPhyReg);
		}
		OperandType tempOpndType = reorderBufferEntry.getInstruction().getDestinationOperand().getOperandType(); 
		wakeUpLogic(tempOpndType, tempDestPhyReg);
	}
	
	void writeBackForXchg()
	{
		//operand 1
		int phyReg = reorderBufferEntry.getOperand1PhyReg1();
		if(reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.machineSpecificRegister)
		{
			core.getExecEngine().getMachineSpecificRegisterFile().setValueValid(true, phyReg);
			wakeUpLogic(OperandType.machineSpecificRegister, phyReg);
		}
		else if(reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.integerRegister)
		{
			core.getExecEngine().getIntegerRenameTable().setValueValid(true, phyReg);
			wakeUpLogic(OperandType.integerRegister, phyReg);
		}
		else if(reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.floatRegister)
		{
			core.getExecEngine().getFloatingPointRenameTable().setValueValid(true, phyReg);
			wakeUpLogic(OperandType.floatRegister, phyReg);
		}
		
		//operand 2
		phyReg = reorderBufferEntry.getOperand2PhyReg1();
		if(reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.machineSpecificRegister)
		{
			core.getExecEngine().getMachineSpecificRegisterFile().setValueValid(true, phyReg);
			wakeUpLogic(OperandType.machineSpecificRegister, phyReg);
		}
		else if(reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.integerRegister)
		{
			core.getExecEngine().getIntegerRenameTable().setValueValid(true, phyReg);
			wakeUpLogic(OperandType.integerRegister, phyReg);
		}
		else if(reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.floatRegister)
		{
			core.getExecEngine().getFloatingPointRenameTable().setValueValid(true, phyReg);
			wakeUpLogic(OperandType.floatRegister, phyReg);
		}
	}
	
	void writeBackForOthers(RegisterFile tempRF, RenameTable tempRN)
	{
		//check if the execution has completed
		long time_of_completion = core.getExecEngine().getFunctionalUnitSet().getTimeWhenFUAvailable(
				OpTypeToFUTypeMapping.getFUType(reorderBufferEntry.getInstruction().getOperationType()),
				FUInstance );
		
		if(time_of_completion <= GlobalClock.getCurrentTime())
		{
			//execution complete
			reorderBufferEntry.setExecuted(true);
			int tempDestPhyReg = reorderBufferEntry.getPhysicalDestinationRegister();
			if(tempRF != null)
			{
				tempRF.setValueValid(true, tempDestPhyReg);
			}
			else if(tempRN != null)
			{
				tempRN.setValueValid(true, tempDestPhyReg);
			}
			
			if(tempRF != null || tempRN != null)
			{
				//there may some instruction that needs to be woken up
				OperandType tempOpndType = reorderBufferEntry.getInstruction().getDestinationOperand().getOperandType(); 
				wakeUpLogic(tempOpndType, tempDestPhyReg);
			}
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
	
	void wakeUpLogic(OperandType opndType, int physicalRegister)
	{
		LinkedList<IWEntry> IW = core.getExecEngine().getInstructionWindow().getIW();
		LinkedList<IWEntry> tempList = new LinkedList<IWEntry>();
		int i = 0;
		boolean toWakeUp;
		ReorderBufferEntry ROBEntry;
				
		while(i < IW.size())
		{
			IWEntry IWentry = IW.get(i);
			ROBEntry = IWentry.getAssociatedROBEntry();
			toWakeUp = false;
			
			if(IWentry.isOperand1Available() == false)
			{
				if(ROBEntry.getInstruction().getSourceOperand1().getOperandType() == opndType
						&& ROBEntry.getOperand1PhyReg1() == physicalRegister)
				{
					IWentry.setOperand1Available(true);
					toWakeUp = true;
				}
				if(ROBEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.memory)
				{
					if(ROBEntry.getInstruction().getSourceOperand1().getMemoryLocationFirstOperand() != null &&
							ROBEntry.getInstruction().getSourceOperand1().getMemoryLocationFirstOperand().getOperandType() == opndType
							&& ROBEntry.getOperand1PhyReg1() == physicalRegister)
					{
						IWentry.setOperand11Available(true);
					}
					if(ROBEntry.getInstruction().getSourceOperand1().getMemoryLocationSecondOperand() != null &&
							ROBEntry.getInstruction().getSourceOperand1().getMemoryLocationSecondOperand().getOperandType() == opndType
							&& ROBEntry.getOperand1PhyReg2() == physicalRegister)
					{
						IWentry.setOperand12Available(true);
					}
					if(IWentry.isOperand11Available() && IWentry.isOperand12Available())
					{
						IWentry.setOperand1Available(true);
						toWakeUp = true;
					}
				}
			}
			
			if(IWentry.isOperand2Available() == false)
			{
				if(ROBEntry.getInstruction().getSourceOperand2().getOperandType() == opndType
						&& ROBEntry.getOperand2PhyReg1() == physicalRegister)
				{
					IWentry.setOperand2Available(true);
					toWakeUp = true;
				}
				if(ROBEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.memory)
				{
					if(ROBEntry.getInstruction().getSourceOperand2().getMemoryLocationFirstOperand() != null &&
							ROBEntry.getInstruction().getSourceOperand2().getMemoryLocationFirstOperand().getOperandType() == opndType
							&& ROBEntry.getOperand2PhyReg1() == physicalRegister)
					{
						IWentry.setOperand21Available(true);
					}
					if(ROBEntry.getInstruction().getSourceOperand2().getMemoryLocationSecondOperand() != null &&
							ROBEntry.getInstruction().getSourceOperand2().getMemoryLocationSecondOperand().getOperandType() == opndType
							&& ROBEntry.getOperand2PhyReg2() == physicalRegister)
					{
						IWentry.setOperand22Available(true);
					}
					if(IWentry.isOperand21Available() && IWentry.isOperand22Available())
					{
						IWentry.setOperand2Available(true);
						toWakeUp = true;
					}
				}
			}
			
			if(toWakeUp == true)
			{
				tempList.addLast(IWentry);
			}
			
			i++;
		}
		
		i = 0;
		while(i < tempList.size())
		{
			tempList.get(i).issueInstruction();
			i++;
		}
	}

}