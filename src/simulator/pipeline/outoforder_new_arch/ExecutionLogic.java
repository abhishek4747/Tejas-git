package pipeline.outoforder_new_arch;

import config.SimulationConfig;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.ExecCompleteEvent;
import generic.GlobalClock;
import generic.Instruction;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;

public class ExecutionLogic extends SimulationElement {
	
	Core core;
	Instruction instruction;
	int threadID = 0;
	int FUInstance;
	EventQueue eventQueue;
	Operand tempDestOpnd;
	OperandType tempDestOpndType;
	ReorderBufferEntry reorderBufferEntry;
	Event tempEvent;
	
	public ExecutionLogic(Core core)
	{
		super(PortType.Unlimited, -1, -1, core.getEventQueue(), -1, -1);
		
		this.core = core;
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
		tempEvent = event;
		instruction = reorderBufferEntry.getInstruction();
		threadID = 0;	//TODO instruction.getThreadID()
		FUInstance = reorderBufferEntry.getFUInstance();
		eventQueue = core.getEventQueue();
		tempDestOpnd = instruction.getDestinationOperand();
		if(tempDestOpnd != null)
		{
			tempDestOpndType = tempDestOpnd.getOperandType();
		}
		else
		{
			tempDestOpndType = null;
		}
		
		if(event.getRequestType() == RequestType.EXEC_COMPLETE)
		{
			this.reorderBufferEntry = ((ExecCompleteEvent)event).getROBEntry();
			handleExecutionCompletion();
		}
		else if(event.getRequestType() == RequestType.BROADCAST_1)
		{
			this.reorderBufferEntry = ((BroadCast1Event)event).getROBEntry();
			performBroadCast1();
		}
	}
	
	public void handleExecutionCompletion()
	{
			
		if(SimulationConfig.debugMode)
		{
			System.out.println("executed : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + reorderBufferEntry.getInstruction());
		}
		
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
		
		if(tempDestOpnd == null)
		{
			if(instruction.getOperationType() == OperationType.xchg)
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
			if(tempDestOpndType == OperandType.machineSpecificRegister)
			{
				tempRF = core.getExecEngine().getMachineSpecificRegisterFile(threadID);
				tempRN = null;
			}
			else if(tempDestOpndType == OperandType.integerRegister)
			{
				tempRF = core.getExecEngine().getIntegerRegisterFile();
				tempRN = core.getExecEngine().getIntegerRenameTable();
			}
			else if(tempDestOpndType == OperandType.floatRegister)
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
			
			if(instruction.getOperationType() == OperationType.mov)
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
		WakeUpLogic.wakeUpLogic(core, tempDestOpndType, tempDestPhyReg, reorderBufferEntry.threadID, -1);
		
		//attempt to write-back
		//WriteBackLogic.writeBack(reorderBufferEntry, 3, tempRF, tempRN, tempDestPhyReg, core);
		
	}
	
	void writeBackForXchg()
	{
		RegisterFile tempRF = null;
		RenameTable tempRN = null;
		int phyReg;
		
		//operand 1
		phyReg = reorderBufferEntry.getOperand1PhyReg1();
		OperandType tempOpndType = instruction.getSourceOperand1().getOperandType();
		
		if(tempOpndType == OperandType.machineSpecificRegister)
		{
			tempRF = core.getExecEngine().getMachineSpecificRegisterFile(threadID);
			WakeUpLogic.wakeUpLogic(core, OperandType.machineSpecificRegister, phyReg, reorderBufferEntry.threadID, -1);
		}
		else if(tempOpndType == OperandType.integerRegister)
		{
			tempRF = core.getExecEngine().getIntegerRegisterFile();
			tempRN = core.getExecEngine().getIntegerRenameTable();
			WakeUpLogic.wakeUpLogic(core, OperandType.integerRegister, phyReg, reorderBufferEntry.threadID, -1);
		}
		else if(tempOpndType == OperandType.floatRegister)
		{
			tempRF = core.getExecEngine().getFloatingPointRegisterFile();
			tempRN = core.getExecEngine().getFloatingPointRenameTable();
			WakeUpLogic.wakeUpLogic(core, OperandType.floatRegister, phyReg, reorderBufferEntry.threadID, -1);
		}
		
		//attempt to write-back
		//WriteBackLogic.writeBack(reorderBufferEntry, 1, tempRF, tempRN, phyReg, core);
		
		
		
		tempRF = null;
		tempRN = null;
		//operand 2
		phyReg = reorderBufferEntry.getOperand2PhyReg1();
		tempOpndType = instruction.getSourceOperand2().getOperandType();
		
		if(tempOpndType == OperandType.machineSpecificRegister)
		{
			tempRF = core.getExecEngine().getMachineSpecificRegisterFile(threadID);
			WakeUpLogic.wakeUpLogic(core, OperandType.machineSpecificRegister, phyReg, reorderBufferEntry.threadID, -1);
		}
		else if(tempOpndType == OperandType.integerRegister)
		{
			tempRF = core.getExecEngine().getIntegerRegisterFile();
			tempRN = core.getExecEngine().getIntegerRenameTable();
			WakeUpLogic.wakeUpLogic(core, OperandType.integerRegister, phyReg, reorderBufferEntry.threadID, -1);
		}
		else if(tempOpndType == OperandType.floatRegister)
		{
			tempRF = core.getExecEngine().getFloatingPointRegisterFile();
			tempRN = core.getExecEngine().getFloatingPointRenameTable();
			WakeUpLogic.wakeUpLogic(core, OperandType.floatRegister, phyReg, reorderBufferEntry.threadID, -1);
		}
		
		//attempt to write-back
		//WriteBackLogic.writeBack(reorderBufferEntry, 2, tempRF, tempRN, phyReg, core);
		
	}
	
	void writeBackForOthers(RegisterFile tempRF, RenameTable tempRN)
	{
		//check if the execution has completed
		long time_of_completion = 0;
		OperationType tempOpType = instruction.getOperationType();
		
		if(tempOpType != OperationType.load &&
				tempOpType != OperationType.store)
		{
			time_of_completion = core.getExecEngine().getFunctionalUnitSet().getTimeWhenFUAvailable(
				OpTypeToFUTypeMapping.getFUType(reorderBufferEntry.getInstruction().getOperationType()),
				FUInstance );
		}
		
		//execution complete
		reorderBufferEntry.setExecuted(true);
		
		int tempDestPhyReg = reorderBufferEntry.getPhysicalDestinationRegister();
		//wakeup waiting IW entries
		if(tempRF != null || tempRN != null)
		{
			//there may some instruction that needs to be woken up
			WakeUpLogic.wakeUpLogic(core, tempDestOpndType, tempDestPhyReg, reorderBufferEntry.threadID, -1);
		}
		
		//attempt to write-back
		//WriteBackLogic.writeBack(reorderBufferEntry, 3, tempRF, tempRN, tempDestPhyReg, core);
	}
	
	void performBroadCast1()
	{
		if(tempDestOpnd != null)
		{
			WakeUpLogic.wakeUpLogic(core, tempDestOpndType, reorderBufferEntry.getPhysicalDestinationRegister(), reorderBufferEntry.threadID, -1);
		}
		else if(instruction.getOperationType() == OperationType.xchg)
		{
			WakeUpLogic.wakeUpLogic(core, instruction.getSourceOperand1().getOperandType(), reorderBufferEntry.getOperand1PhyReg1(), reorderBufferEntry.threadID, -1);
			WakeUpLogic.wakeUpLogic(core, instruction.getSourceOperand2().getOperandType(), reorderBufferEntry.getOperand2PhyReg1(), reorderBufferEntry.threadID, -1);
		}
	}
	


}
