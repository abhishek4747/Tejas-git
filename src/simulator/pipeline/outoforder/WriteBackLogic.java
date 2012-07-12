package pipeline.outoforder;

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
		
		if(execEngine.isToStall5() == true)
		{
			//pipeline stalled due to branch mis-prediction
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
				
				this.core.powerCounters.incrementWindowAccess(1);
				this.core.powerCounters.incrementWindowPregAccess(1);
				this.core.powerCounters.incrementWindowWakeupAccess(1);
				this.core.powerCounters.incrementResultbusAccess(1);
				
				
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
						if(tempRN.getMappingValid(buffer[i].getPhysicalDestinationRegister()) == false &&
								tempRN.getAssociatedRegisterFile().getNoOfActiveWriters(buffer[i].getPhysicalDestinationRegister()) == 1)
						{
							tempRN.addToAvailableList(buffer[i].getPhysicalDestinationRegister());
						}
						tempRN.setValueValid(true, buffer[i].getPhysicalDestinationRegister());
						execEngine.getIntegerRegisterFile().setValueValid(true, buffer[i].getPhysicalDestinationRegister());
						tempRN.getAssociatedRegisterFile().decrementNoOfActiveWriters(buffer[i].getPhysicalDestinationRegister());
						
						//Update counters for power calculation. For now, only integer register file assumed.
						this.core.powerCounters.incrementRegfileAccess(1);
					}
					else if(tempDestOpnd.isFloatRegisterOperand())
					{
						tempRN = execEngine.getFloatingPointRenameTable();
						if(tempRN.getMappingValid(buffer[i].getPhysicalDestinationRegister()) == false &&
								tempRN.getAssociatedRegisterFile().getNoOfActiveWriters(buffer[i].getPhysicalDestinationRegister()) == 1)
						{
							tempRN.addToAvailableList(buffer[i].getPhysicalDestinationRegister());
						}
						tempRN.setValueValid(true, buffer[i].getPhysicalDestinationRegister());
						execEngine.getFloatingPointRegisterFile().setValueValid(true, buffer[i].getPhysicalDestinationRegister());
						tempRN.getAssociatedRegisterFile().decrementNoOfActiveWriters(buffer[i].getPhysicalDestinationRegister());
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
						if(tempRN.getMappingValid(buffer[i].operand1PhyReg1) == false &&
								tempRN.getAssociatedRegisterFile().getNoOfActiveWriters(buffer[i].operand1PhyReg1) == 1)
						{
							tempRN.addToAvailableList(buffer[i].operand1PhyReg1);
						}
						if(tempRN.getAssociatedRegisterFile().getNoOfActiveWriters(buffer[i].operand1PhyReg1) < 1)
						{
							System.out.println("no of active writers < 1 (at writeback) !!");
						}
						tempRN.setValueValid(true, buffer[i].operand1PhyReg1);
						execEngine.getIntegerRegisterFile().setValueValid(true, buffer[i].operand1PhyReg1);
						tempRN.getAssociatedRegisterFile().decrementNoOfActiveWriters(buffer[i].operand1PhyReg1);
					}
					else if(tempDestOpnd.isFloatRegisterOperand())
					{
						tempRN = execEngine.getFloatingPointRenameTable();
						if(tempRN.getMappingValid(buffer[i].operand1PhyReg1) == false &&
								tempRN.getAssociatedRegisterFile().getNoOfActiveWriters(buffer[i].operand1PhyReg1) == 1)
						{
							tempRN.addToAvailableList(buffer[i].operand1PhyReg1);
						}
						tempRN.setValueValid(true, buffer[i].operand1PhyReg1);
						execEngine.getFloatingPointRegisterFile().setValueValid(true, buffer[i].operand1PhyReg1);
						tempRN.getAssociatedRegisterFile().decrementNoOfActiveWriters(buffer[i].operand1PhyReg1);
					}
					else
					{
						execEngine.getMachineSpecificRegisterFile(buffer[i].getThreadID()).setValueValid(true, buffer[i].operand1PhyReg1);
					}
					
					if(buffer[i].getInstruction().getSourceOperand1().getOperandType() != buffer[i].getInstruction().getSourceOperand2().getOperandType() ||
							buffer[i].operand1PhyReg1 != buffer[i].operand2PhyReg1)
					{
						tempDestOpnd = buffer[i].getInstruction().getSourceOperand2();
						if(tempDestOpnd.isIntegerRegisterOperand())
						{
							tempRN = execEngine.getIntegerRenameTable();
							if(tempRN.getMappingValid(buffer[i].operand2PhyReg1) == false &&
									tempRN.getAssociatedRegisterFile().getNoOfActiveWriters(buffer[i].operand2PhyReg1) == 1)
							{
								tempRN.addToAvailableList(buffer[i].operand2PhyReg1);
							}
							tempRN.setValueValid(true, buffer[i].operand2PhyReg1);
							execEngine.getIntegerRegisterFile().setValueValid(true, buffer[i].operand2PhyReg1);
							tempRN.getAssociatedRegisterFile().decrementNoOfActiveWriters(buffer[i].operand2PhyReg1);
						}
						else if(tempDestOpnd.isFloatRegisterOperand())
						{
							tempRN = execEngine.getFloatingPointRenameTable();
							if(tempRN.getMappingValid(buffer[i].operand2PhyReg1) == false &&
									tempRN.getAssociatedRegisterFile().getNoOfActiveWriters(buffer[i].operand2PhyReg1) == 1)
							{
								tempRN.addToAvailableList(buffer[i].operand2PhyReg1);
							}
							tempRN.setValueValid(true, buffer[i].operand2PhyReg1);
							execEngine.getFloatingPointRegisterFile().setValueValid(true, buffer[i].operand2PhyReg1);
							tempRN.getAssociatedRegisterFile().decrementNoOfActiveWriters(buffer[i].operand2PhyReg1);
						}
						else
						{
							execEngine.getMachineSpecificRegisterFile(buffer[i].getThreadID()).setValueValid(true, buffer[i].operand2PhyReg1);
						}
					}
				}
			}
			
			i = (i+1)%ROB.getMaxROBSize();
			
		}while(i != ROB.tail);
	}

}
