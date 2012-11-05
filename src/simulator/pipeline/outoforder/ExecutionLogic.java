package pipeline.outoforder;

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
	OutOrderExecutionEngine execEngine;
	Instruction instruction;
	int threadID = 0;
	int FUInstance;
	EventQueue eventQueue;
	Operand tempDestOpnd;
	OperandType tempDestOpndType;
	ReorderBufferEntry reorderBufferEntry;
	Event tempEvent;
	ReorderBuffer ROB;
	
	public ExecutionLogic(Core core, OutOrderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1, core.getEventQueue(), -1, -1);
		
		this.core = core;
		this.execEngine = execEngine;
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
				
		tempEvent = event;
		ROB = execEngine.getReorderBuffer();
		
		if(event.getRequestType() == RequestType.EXEC_COMPLETE)
		{
			this.reorderBufferEntry = ((ExecCompleteEvent)event).getROBEntry();
		}
		else if(event.getRequestType() == RequestType.BROADCAST_1)
		{
			this.reorderBufferEntry = ((BroadCast1Event)event).getROBEntry();
		}
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
			handleExecutionCompletion();
		}
		else if(event.getRequestType() == RequestType.BROADCAST_1)
		{
			performBroadCast1();
		}
	}
	
	public void handleExecutionCompletion()
	{
			
		if(SimulationConfig.debugMode)
		{
			System.out.println("executed : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + reorderBufferEntry.getInstruction());
		}
		
		if(reorderBufferEntry.getExecuted() == true ||
				reorderBufferEntry.isRenameDone == false ||
				reorderBufferEntry.isIssued == false)
		{
			System.out.println("cannot complete execution of this instruction");
			return;
		}
		
		if(reorderBufferEntry.getIssued() == false)
		{
			System.out.println("not yet issued, but execution complete");
			//return;
		}
		
		if(tempDestOpnd == null)
		{
			if(instruction.getOperationType() == OperationType.xchg)
			{
				//doesn't use an FU				
				reorderBufferEntry.setExecuted(true);
				wakeUpForXchg();
			}
			else
			{
				reorderBufferEntry.setExecuted(true);
				reorderBufferEntry.setWriteBackDone1(true);
				reorderBufferEntry.setWriteBackDone2(true);
			}
		}
		
		else
		{
			if(instruction.getOperationType() == OperationType.mov)
			{
				//doesn't use an FU			
				reorderBufferEntry.setExecuted(true);			
				wakeUpForMov();
			}		
			else
			{
				wakeUpForOthers();
			}
		}
		
	}
	
	void wakeUpForMov()
	{
		//wake up waiting IW entries
		int tempDestPhyReg = reorderBufferEntry.getPhysicalDestinationRegister();
		WakeUpLogic.wakeUpLogic(core, tempDestOpndType, tempDestPhyReg, reorderBufferEntry.threadID, (reorderBufferEntry.pos + 1)%ROB.MaxROBSize);//(ROB.indexOf(reorderBufferEntry) + 1) % ROB.MaxROBSize);
		
	}
	
	void wakeUpForXchg()
	{
		int phyReg;
		
		//operand 1
		phyReg = reorderBufferEntry.getOperand1PhyReg1();
		OperandType tempOpndType = instruction.getSourceOperand1().getOperandType();
		
		if(tempOpndType == OperandType.machineSpecificRegister)
		{
			WakeUpLogic.wakeUpLogic(core, OperandType.machineSpecificRegister, phyReg, reorderBufferEntry.threadID, (reorderBufferEntry.pos + 1)%ROB.MaxROBSize);//(ROB.indexOf(reorderBufferEntry) + 1) % ROB.MaxROBSize);
		}
		else if(tempOpndType == OperandType.integerRegister)
		{
			WakeUpLogic.wakeUpLogic(core, OperandType.integerRegister, phyReg, reorderBufferEntry.threadID, (reorderBufferEntry.pos + 1)%ROB.MaxROBSize);//(ROB.indexOf(reorderBufferEntry) + 1) % ROB.MaxROBSize);
		}
		else if(tempOpndType == OperandType.floatRegister)
		{
			WakeUpLogic.wakeUpLogic(core, OperandType.floatRegister, phyReg, reorderBufferEntry.threadID, (reorderBufferEntry.pos + 1)%ROB.MaxROBSize);//(ROB.indexOf(reorderBufferEntry) + 1) % ROB.MaxROBSize);
		}	
		
		
		//operand 2
		phyReg = reorderBufferEntry.getOperand2PhyReg1();
		tempOpndType = instruction.getSourceOperand2().getOperandType();
		
		if(tempOpndType == OperandType.machineSpecificRegister)
		{
			WakeUpLogic.wakeUpLogic(core, OperandType.machineSpecificRegister, phyReg, reorderBufferEntry.threadID, (reorderBufferEntry.pos + 1)%ROB.MaxROBSize);//(ROB.indexOf(reorderBufferEntry) + 1) % ROB.MaxROBSize);
		}
		else if(tempOpndType == OperandType.integerRegister)
		{
			WakeUpLogic.wakeUpLogic(core, OperandType.integerRegister, phyReg, reorderBufferEntry.threadID, (reorderBufferEntry.pos + 1)%ROB.MaxROBSize);//(ROB.indexOf(reorderBufferEntry) + 1) % ROB.MaxROBSize);
		}
		else if(tempOpndType == OperandType.floatRegister)
		{
			WakeUpLogic.wakeUpLogic(core, OperandType.floatRegister, phyReg, reorderBufferEntry.threadID, (reorderBufferEntry.pos + 1)%ROB.MaxROBSize);//(ROB.indexOf(reorderBufferEntry) + 1) % ROB.MaxROBSize);
		}
		
	}
	
	void wakeUpForOthers()
	{
		if(reorderBufferEntry.instruction.getOperationType() == OperationType.load)
		{
			if(reorderBufferEntry.lsqEntry.isValid() == false)
			{
				System.out.println("invalid load has completed execution");
			}
			if(reorderBufferEntry.lsqEntry.isForwarded() == false)
			{
				System.out.println("unforwarded load has completed execution");
			}
		}
		reorderBufferEntry.setExecuted(true);
		
		int tempDestPhyReg = reorderBufferEntry.getPhysicalDestinationRegister();
		
		//wakeup waiting IW entries
		if(tempDestOpnd != null)
		{
			//there may some instruction that needs to be woken up
			WakeUpLogic.wakeUpLogic(core, tempDestOpndType, tempDestPhyReg, reorderBufferEntry.threadID, (reorderBufferEntry.pos + 1)%ROB.MaxROBSize);//(ROB.indexOf(reorderBufferEntry) + 1) % ROB.MaxROBSize);
		}
	}
	
	void performBroadCast1()
	{
		if(tempDestOpnd != null)
		{
			WakeUpLogic.wakeUpLogic(core, tempDestOpndType, reorderBufferEntry.getPhysicalDestinationRegister(), reorderBufferEntry.threadID, (reorderBufferEntry.pos + 1)%ROB.MaxROBSize);//(ROB.indexOf(reorderBufferEntry) + 1) % ROB.MaxROBSize);
		}
		else if(instruction.getOperationType() == OperationType.xchg)
		{
			WakeUpLogic.wakeUpLogic(core, instruction.getSourceOperand1().getOperandType(), reorderBufferEntry.getOperand1PhyReg1(), reorderBufferEntry.threadID, (reorderBufferEntry.pos + 1)%ROB.MaxROBSize);//(ROB.indexOf(reorderBufferEntry) + 1) % ROB.MaxROBSize);
			WakeUpLogic.wakeUpLogic(core, instruction.getSourceOperand2().getOperandType(), reorderBufferEntry.getOperand2PhyReg1(), reorderBufferEntry.threadID, (reorderBufferEntry.pos + 1)%ROB.MaxROBSize);//(ROB.indexOf(reorderBufferEntry) + 1) % ROB.MaxROBSize);
		}
	}
	
}
