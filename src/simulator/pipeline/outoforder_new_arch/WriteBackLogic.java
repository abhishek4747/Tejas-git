package pipeline.outoforder_new_arch;

import config.SimulationConfig;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Operand;
import generic.OperandType;
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
				
				//TODO is a better solution possible?
				if(buffer[i].instruction.getOperationType() == OperationType.load)
				{
					WakeUpLogic.wakeUpLogic(core, buffer[i].getInstruction().getDestinationOperand().getOperandType(), buffer[i].physicalDestinationRegister, buffer[i].threadID, (buffer[i].pos + 1)%ROB.MaxROBSize);//(ROB.indexOf(buffer[i]) + 1) % ROB.MaxROBSize);
				}

				if(SimulationConfig.debugMode)
				{
					System.out.println("writeback : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : " + buffer[i].getInstruction());
				}
				
				RenameTable tempRN = null;
				
				//add to available list
				Operand tempDestOpnd = buffer[i].getInstruction().getDestinationOperand();
				if(tempDestOpnd != null)
				{
					if(tempDestOpnd.isIntegerRegisterOperand())
					{
						tempRN = execEngine.getIntegerRenameTable();
						if(tempRN.getMappingValid(buffer[i].getPhysicalDestinationRegister()) == false)
						{
							tempRN.addToAvailableList(buffer[i].getPhysicalDestinationRegister());
						}
						tempRN.setValueValid(true, buffer[i].getPhysicalDestinationRegister());
					}
					else if(tempDestOpnd.isFloatRegisterOperand())
					{
						tempRN = execEngine.getFloatingPointRenameTable();
						if(tempRN.getMappingValid(buffer[i].getPhysicalDestinationRegister()) == false)
						{
							tempRN.addToAvailableList(buffer[i].getPhysicalDestinationRegister());
						}
						tempRN.setValueValid(true, buffer[i].getPhysicalDestinationRegister());
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
						tempRN = execEngine.getIntegerRenameTable();
						if(tempRN.getMappingValid(buffer[i].operand1PhyReg1) == false)
						{
							tempRN.addToAvailableList(buffer[i].operand1PhyReg1);
						}
						tempRN.setValueValid(true, buffer[i].operand1PhyReg1);
					}
					else if(tempDestOpnd.isFloatRegisterOperand())
					{
						tempRN = execEngine.getFloatingPointRenameTable();
						if(tempRN.getMappingValid(buffer[i].operand1PhyReg1) == false)
						{
							tempRN.addToAvailableList(buffer[i].operand1PhyReg1);
						}
						tempRN.setValueValid(true, buffer[i].operand1PhyReg1);
					}
					else
					{
						execEngine.getMachineSpecificRegisterFile(buffer[i].getThreadID()).setValueValid(true, buffer[i].operand1PhyReg1);
					}
					tempDestOpnd = buffer[i].getInstruction().getSourceOperand2();
					if(tempDestOpnd.isIntegerRegisterOperand())
					{
						tempRN = execEngine.getIntegerRenameTable();
						if(tempRN.getMappingValid(buffer[i].operand2PhyReg1) == false)
						{
							tempRN.addToAvailableList(buffer[i].operand2PhyReg1);
						}
						tempRN.setValueValid(true, buffer[i].operand2PhyReg1);
					}
					else if(tempDestOpnd.isFloatRegisterOperand())
					{
						tempRN = execEngine.getFloatingPointRenameTable();
						if(tempRN.getMappingValid(buffer[i].operand2PhyReg1) == false)
						{
							tempRN.addToAvailableList(buffer[i].operand2PhyReg1);
						}
						tempRN.setValueValid(true, buffer[i].operand2PhyReg1);
					}
					else
					{
						execEngine.getMachineSpecificRegisterFile(buffer[i].getThreadID()).setValueValid(true, buffer[i].operand2PhyReg1);
					}
				}
			}
			
			i = (i+1)%ROB.getMaxROBSize();
			
		}while(i != ROB.tail);
	}

}
