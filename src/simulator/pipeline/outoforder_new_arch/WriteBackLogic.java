package pipeline.outoforder_new_arch;

import config.SimulationConfig;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Operand;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class WriteBackLogic extends SimulationElement {
	
	Core core;
	ExecutionEngine execEngine;
	ReorderBuffer ROB;
	
	public WriteBackLogic(Core core, ExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1, null, -1, -1);
		this.core = core;
		this.execEngine = execEngine;
		ROB = execEngine.getReorderBuffer();
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub

	}
	
	public void performWriteBack()
	{
		if(ROB.head == -1)
		{
			//ROB empty
			return;
		}
		
		int i = ROB.head;
		
		ReorderBufferEntry[] buffer = ROB.getROB();
		do
		{
			if(buffer[i].getExecuted() == true &&
					buffer[i].isWriteBackDone() == false)
			{
				buffer[i].setWriteBackDone1(true);
				buffer[i].setWriteBackDone2(true);

				if(SimulationConfig.debugMode)
				{
					System.out.println("writeback : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : " + buffer[i].getInstruction());
				}
				
				//add to available list
				Operand tempDestOpnd = buffer[i].getInstruction().getDestinationOperand();
				if(tempDestOpnd != null)
				{
					if(tempDestOpnd.isIntegerRegisterOperand())
					{
						execEngine.getIntegerRenameTable().addToAvailableList(buffer[i].getPhysicalDestinationRegister());
						execEngine.getIntegerRenameTable().setValueValid(true, buffer[i].getPhysicalDestinationRegister());
					}
					else if(tempDestOpnd.isFloatRegisterOperand())
					{
						execEngine.getFloatingPointRenameTable().addToAvailableList(buffer[i].getPhysicalDestinationRegister());
						execEngine.getFloatingPointRenameTable().setValueValid(true, buffer[i].getPhysicalDestinationRegister());
					}
					else
					{
						execEngine.getMachineSpecificRegisterFile(buffer[i].getThreadID()).setValueValid(true, buffer[i].getPhysicalDestinationRegister());
					}
				}
				else if(buffer[i].getInstruction().getOperationType() == OperationType.xchg)
				{
					tempDestOpnd = buffer[i].getInstruction().getSourceOperand1();
					if(tempDestOpnd.isIntegerRegisterOperand())
					{
						execEngine.getIntegerRenameTable().addToAvailableList(buffer[i].getOperand1PhyReg1());
						execEngine.getIntegerRenameTable().setValueValid(true, buffer[i].getOperand1PhyReg1());
					}
					else if(tempDestOpnd.isFloatRegisterOperand())
					{
						execEngine.getFloatingPointRenameTable().addToAvailableList(buffer[i].getOperand1PhyReg1());
						execEngine.getFloatingPointRenameTable().setValueValid(true, buffer[i].getOperand1PhyReg1());
					}
					tempDestOpnd = buffer[i].getInstruction().getSourceOperand2();
					if(tempDestOpnd.isIntegerRegisterOperand())
					{
						execEngine.getIntegerRenameTable().addToAvailableList(buffer[i].getOperand2PhyReg1());
						execEngine.getIntegerRenameTable().setValueValid(true, buffer[i].getOperand2PhyReg1());
					}
					else if(tempDestOpnd.isFloatRegisterOperand())
					{
						execEngine.getFloatingPointRenameTable().addToAvailableList(buffer[i].getOperand2PhyReg1());
						execEngine.getFloatingPointRenameTable().setValueValid(true, buffer[i].getOperand2PhyReg1());
					}
				}
			}
			
			i = (i+1)%ROB.getMaxROBSize();
			
		}while(i != ROB.tail);
	}

}
