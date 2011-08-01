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
	
	public ExecutionCompleteEvent(ReorderBufferEntry reorderBufferEntry,
									int FUInstance,
									Core core,
									long eventTime)
	{
		super(eventTime,
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
	public NewEvent handleEvent() {
		
		if(reorderBufferEntry.getExecuted() == true)
		{
			System.out.println("already executed!");
			return null;
		}
		
		RegisterFile tempRF = null;
		RenameTable tempRN = null;
		
		OperandType tempOpndType = reorderBufferEntry.getInstruction().getDestinationOperand().getOperandType(); 
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
		
		if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.mov ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg)
		{
			//doesn't use an FU
			
			reorderBufferEntry.setExecuted(true);
			
			if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.mov)
			{
				writeBackForMov(tempRF, tempRN);
			}
			else
			{
				//xchg operation
				writeBackForXchg(tempRF, tempRN);
			}
		}
		
		else
		{
			return writeBackForOthers(tempRF, tempRN);
		}
		
		return null;
		
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
	
	void writeBackForXchg(RegisterFile tempRF, RenameTable tempRN)
	{
		//operand 1
		int phyReg = reorderBufferEntry.getOperand1PhyReg();
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
		phyReg = reorderBufferEntry.getOperand2PhyReg();
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
	
	NewEvent writeBackForOthers(RegisterFile tempRF, RenameTable tempRN)
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
			OperandType tempOpndType = reorderBufferEntry.getInstruction().getDestinationOperand().getOperandType(); 
			wakeUpLogic(tempOpndType, tempDestPhyReg);
			
			return null;
		}
		
		else
		{			
			reorderBufferEntry.setReadyAtTime(time_of_completion);
			
			//schedule new ExecutionCompleteEvent
			return(
					new ExecutionCompleteEvent(
							reorderBufferEntry,
							FUInstance,
							core,
							time_of_completion ) );
		}
	}
	
	void wakeUpLogic(OperandType opndType, int physicalRegister)
	{
		LinkedList<IWEntry> IW = core.getExecEngine().getInstructionWindow().getIW();
		LinkedList<IWEntry> tempList = new LinkedList<IWEntry>();
		int i = 0;
		boolean toWakeUp;
				
		while(i < IW.size())
		{
			IWEntry IWentry = IW.get(i);
			ReorderBufferEntry ROBEntry = IWentry.getAssociatedROBEntry();
			toWakeUp = false;
			
			if(IWentry.isOperand1Available() == false)
			{
				if(opndType == ROBEntry.getInstruction().getSourceOperand1().getOperandType()
						&& physicalRegister == ROBEntry.getOperand1PhyReg())
				{
						IWentry.setOperand1Available(true);
						toWakeUp = true;
				}
			}
			
			if(IWentry.isOperand2Available() == false)
			{
				if(opndType == ROBEntry.getInstruction().getSourceOperand2().getOperandType()
						&& physicalRegister == ROBEntry.getOperand2PhyReg())
				{
						IWentry.setOperand2Available(true);
						toWakeUp = true;
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