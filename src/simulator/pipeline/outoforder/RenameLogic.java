package pipeline.outoforder;

import config.SimulationConfig;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class RenameLogic extends SimulationElement {
	
	Core core;
	OutOrderExecutionEngine execEngine;
	ReorderBufferEntry[] decodeBuffer;
	ReorderBufferEntry[] renameBuffer;
	int decodeWidth;
	
	int threadID;
	Instruction instruction;
	ReorderBufferEntry reorderBufferEntry;
	OperationType opType;
	
	public RenameLogic(Core core, OutOrderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		this.execEngine = execEngine;
		decodeBuffer = execEngine.getDecodeBuffer();
		renameBuffer = execEngine.getRenameBuffer();
		decodeWidth = core.getDecodeWidth();
	}
	
	public void performRename()
	{
		if(execEngine.isToStall5() == true)
		{
			//pipeline stalled due to branch mis-prediction
			return;
		}
		
		if(!execEngine.isToStall1())
		{
			for(int i = 0; i < decodeWidth; i++)
			{
				if(decodeBuffer[i] != null)
				{
					reorderBufferEntry = decodeBuffer[i];
					threadID = 0;						//TODO threadID should be newROBEntry.getInstruction().getThreadID()
					if(processDestOperand(reorderBufferEntry))
					{
						processOperand1(reorderBufferEntry);
						processOperand2(reorderBufferEntry);
						
						instruction = reorderBufferEntry.getInstruction();
						opType = instruction.getOperationType();
						//TODO
						//Everytime the operand becomes available - inside the methods checkOperand1Availability and checkOperand2Availability
						//We are incrementing windowAccess and windowPregAccess. Verify the correctness - particularly for machineSpecificRegfile operands
						
						checkOperand1Availability();
						if(reorderBufferEntry.isOperand2Available() == false)
						{
							checkOperand2Availability();
						}
						
						renameBuffer[i] = decodeBuffer[i];
						decodeBuffer[i] = null;
						reorderBufferEntry.isRenameDone = true;
						//FIXME For now, for every renaming, only one kind of rename accesses are incremented
						this.core.powerCounters.incrementRenameAccess(1);
						execEngine.setToStall2(false);
						
						if(SimulationConfig.debugMode)
						{
							System.out.println("renamed : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + reorderBufferEntry.getInstruction());
							if(instruction.getRISCProgramCounter() == Integer.parseInt("808bf86", 16))
							{
								System.out.println("\n\n" + reorderBufferEntry + "\n\n");
							}
						}
						
						if(renameBuffer[i].getInstruction() == null)
						{
							System.out.println("rename buffer - inst is null");
						}
					}
					else
					{
						break;
					}
				}
			}
		}
	}

	private void processOperand1(ReorderBufferEntry reorderBufferEntry)
	{
		Operand tempOpnd = reorderBufferEntry.getInstruction().getSourceOperand1();
		if(tempOpnd == null ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.inValid ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.nop)
		{
			reorderBufferEntry.setOperand1PhyReg1(-1);
			reorderBufferEntry.setOperand1PhyReg2(-1);
			return;
		}

		OperandType tempOpndType = tempOpnd.getOperandType();
		int archReg = (int) tempOpnd.getValue();
		if(tempOpndType == OperandType.integerRegister)
		{
			//Increment counters for power calculations  FIXME is this correct ? on every process operand, rename table is accessed!
			this.core.powerCounters.incrementIntegerRenameAccess(1);
			reorderBufferEntry.setOperand1PhyReg1(execEngine.getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
			reorderBufferEntry.setOperand1PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.floatRegister)
		{
			//Increment counters for power calculations 
			this.core.powerCounters.incrementFloatRenameAccess(1);
			reorderBufferEntry.setOperand1PhyReg1(execEngine.getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
			reorderBufferEntry.setOperand1PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.machineSpecificRegister)
		{
			reorderBufferEntry.setOperand1PhyReg1(archReg);
			reorderBufferEntry.setOperand1PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.memory)
		{
			Operand memLocOpnd1 = tempOpnd.getMemoryLocationFirstOperand();
			Operand memLocOpnd2 = tempOpnd.getMemoryLocationSecondOperand();
			
			//processing memoryLocationFirstOperand
			if(memLocOpnd1 == null)
			{
				reorderBufferEntry.setOperand1PhyReg1(-1);
			}
			else
			{
				archReg = (int)memLocOpnd1.getValue();
				tempOpndType = memLocOpnd1.getOperandType();
				
				if(tempOpndType == OperandType.integerRegister)
				{
					//Increment counters for power calculations 
					this.core.powerCounters.incrementIntegerRenameAccess(1);
					reorderBufferEntry.setOperand1PhyReg1(execEngine.getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.floatRegister)
				{
					//Increment counters for power calculations 
					this.core.powerCounters.incrementFloatRenameAccess(1);
					reorderBufferEntry.setOperand1PhyReg1(execEngine.getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.machineSpecificRegister)
				{
					reorderBufferEntry.setOperand1PhyReg1(archReg);
				}
				else
				{
					reorderBufferEntry.setOperand1PhyReg1(-1);
				}
			}
			
			//processing memoryLocationSecondOperand
			if(memLocOpnd2 == null)
			{
				reorderBufferEntry.setOperand1PhyReg2(-1);
			}
			else
			{
				archReg = (int)memLocOpnd2.getValue();
				tempOpndType = memLocOpnd2.getOperandType();
				
				if(tempOpndType == OperandType.integerRegister)
				{
					//Increment counters for power calculations  FIXME is this correct ? on every process operand, rename table is accessed!
					this.core.powerCounters.incrementIntegerRenameAccess(1);
					reorderBufferEntry.setOperand1PhyReg2(execEngine.getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.floatRegister)
				{
					//Increment counters for power calculations 
					this.core.powerCounters.incrementFloatRenameAccess(1);
					reorderBufferEntry.setOperand1PhyReg2(execEngine.getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.machineSpecificRegister)
				{
					reorderBufferEntry.setOperand1PhyReg2(archReg);
				}
				else
				{
					reorderBufferEntry.setOperand1PhyReg2(-1);
				}
			}
		}
		else
		{
			reorderBufferEntry.setOperand1PhyReg1(-1);
			reorderBufferEntry.setOperand1PhyReg2(-1);
		}
	}
	
	private void processOperand2(ReorderBufferEntry reorderBufferEntry)
	{
		Operand tempOpnd = reorderBufferEntry.getInstruction().getSourceOperand2();
		
		if(tempOpnd == null ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.inValid ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.nop)
		{
			reorderBufferEntry.setOperand2PhyReg1(-1);
			reorderBufferEntry.setOperand2PhyReg2(-1);
			return;
		}

		OperandType tempOpndType = tempOpnd.getOperandType();
		int archReg = (int) tempOpnd.getValue();
		if(tempOpndType == OperandType.integerRegister)
		{			
			//Increment counters for power calculations  FIXME is this correct ? on every process operand, rename table is accessed!
			this.core.powerCounters.incrementIntegerRenameAccess(1);
			reorderBufferEntry.setOperand2PhyReg1(execEngine.getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
			reorderBufferEntry.setOperand2PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.floatRegister)
		{
			//Increment counters for power calculations 
			this.core.powerCounters.incrementFloatRenameAccess(1);
			reorderBufferEntry.setOperand2PhyReg1(execEngine.getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
			reorderBufferEntry.setOperand2PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.machineSpecificRegister)
		{
			reorderBufferEntry.setOperand2PhyReg1(archReg);
			reorderBufferEntry.setOperand2PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.memory)
		{
			Operand memLocOpnd1 = tempOpnd.getMemoryLocationFirstOperand();
			Operand memLocOpnd2 = tempOpnd.getMemoryLocationSecondOperand();
			
			//processing memoryLocationFirstOperand
			if(memLocOpnd1 == null)
			{
				reorderBufferEntry.setOperand2PhyReg1(-1);
			}
			else
			{
				archReg = (int)memLocOpnd1.getValue();
				tempOpndType = memLocOpnd1.getOperandType();
				
				if(tempOpndType == OperandType.integerRegister)
				{
					//Increment counters for power calculations  FIXME is this correct ? on every process operand, rename table is accessed!
					this.core.powerCounters.incrementIntegerRenameAccess(1);
					reorderBufferEntry.setOperand2PhyReg1(execEngine.getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.floatRegister)
				{
					//Increment counters for power calculations 
					this.core.powerCounters.incrementFloatRenameAccess(1);
					reorderBufferEntry.setOperand2PhyReg1(execEngine.getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
					reorderBufferEntry.setOperand2PhyReg1(execEngine.getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.machineSpecificRegister)
				{
					reorderBufferEntry.setOperand2PhyReg1(archReg);
				}
				else
				{
					reorderBufferEntry.setOperand2PhyReg1(-1);
				}
			}
			
			//processing memoryLocationSecondOperand
			if(memLocOpnd2 == null)
			{
				reorderBufferEntry.setOperand2PhyReg2(-1);
			}
			else
			{
				archReg = (int)memLocOpnd2.getValue();
				tempOpndType = memLocOpnd2.getOperandType();
				
				if(tempOpndType == OperandType.integerRegister)
				{
					//Increment counters for power calculations 
					this.core.powerCounters.incrementIntegerRenameAccess(1);
					reorderBufferEntry.setOperand2PhyReg2(execEngine.getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.floatRegister)
				{
					//Increment counters for power calculations 
					this.core.powerCounters.incrementFloatRenameAccess(1);
					reorderBufferEntry.setOperand2PhyReg2(execEngine.getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.machineSpecificRegister)
				{
					reorderBufferEntry.setOperand2PhyReg2(archReg);
				}
				else
				{
					reorderBufferEntry.setOperand2PhyReg2(-1);
				}
			}
		}
		else
		{
			reorderBufferEntry.setOperand2PhyReg1(-1);
			reorderBufferEntry.setOperand2PhyReg2(-1);
		}
	}
	
	private boolean processDestOperand(ReorderBufferEntry reorderBufferEntry)
	{
		Operand tempOpnd = reorderBufferEntry.getInstruction().getDestinationOperand();
		if(tempOpnd == null ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.inValid ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.nop)
		{
			return true;
		}
		
		OperandType tempOpndType = tempOpnd.getOperandType(); 
		
		if(tempOpndType != OperandType.integerRegister &&
				tempOpndType != OperandType.floatRegister &&
				tempOpndType != OperandType.machineSpecificRegister)
		{
			return true;
		}		
		else
		{
			if(tempOpndType == OperandType.machineSpecificRegister)
			{
				return handleMSR(reorderBufferEntry);				
			}			
			else
			{
				return handleIntFloat(reorderBufferEntry);				
			}
		}
	}
	
	boolean handleMSR(ReorderBufferEntry reorderBufferEntry)
	{
		RegisterFile tempRF = execEngine.getMachineSpecificRegisterFile(threadID);
		Operand tempOpnd = reorderBufferEntry.getInstruction().getDestinationOperand();
		
		int destPhyReg = (int) tempOpnd.getValue();
		reorderBufferEntry.setPhysicalDestinationRegister(destPhyReg);
		
		if(tempRF.getValueValid(destPhyReg) == true)
		{
			//destination MSR available
			
			tempRF.setProducerROBEntry(reorderBufferEntry, destPhyReg);
			tempRF.setValueValid(false, destPhyReg);
			
			return true;
		}
		
		else
		{
			//stall decode because physical register for destination was not allocated
			execEngine.setToStall2(true);
			return false;
		}
	}
	
	boolean handleIntFloat(ReorderBufferEntry reorderBufferEntry)
	{
		RenameTable tempRN;
		OperandType tempOpndType = reorderBufferEntry.getInstruction().
									getDestinationOperand().getOperandType();
		if(tempOpndType == OperandType.integerRegister)
		{
			tempRN = execEngine.getIntegerRenameTable();
			//Increment counters for Power calculations
			this.core.powerCounters.incrementIntegerRenameAccess(1);
		}
		else
		{
			tempRN = execEngine.getFloatingPointRenameTable();
			//Increment counters for power calculations 
			this.core.powerCounters.incrementFloatRenameAccess(1);
		}
		
		int r = tempRN.allocatePhysicalRegister(threadID, (int) reorderBufferEntry.getInstruction().getDestinationOperand().getValue());
		if(r >= 0)
		{
			//physical register found
			
			reorderBufferEntry.setPhysicalDestinationRegister(r);
			tempRN.setValueValid(false, r);
			tempRN.setProducerROBEntry(reorderBufferEntry, r);
			
			return true;
		}
		else
		{
			//look for a physical register in the next clock cycle
			//stall decode because physical register for destination was not allocated
			execEngine.setToStall2(true);
			return false;
		}
	}
	
	void checkOperand1Availability()
	{
		Operand tempOpnd = instruction.getSourceOperand1();
		if(tempOpnd == null ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.inValid ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.nop)
		{
			//Increment counters for power calculation
			this.core.powerCounters.incrementWindowAccess(1);
			this.core.powerCounters.incrementWindowPregAccess(1);

			reorderBufferEntry.setOperand1Available(true);
			return;
		}
		
		OperandType tempOpndType = tempOpnd.getOperandType();
		
		if(tempOpndType == OperandType.immediate)
		{
			//Increment counters for power calculation
			this.core.powerCounters.incrementWindowAccess(1);
			this.core.powerCounters.incrementWindowPregAccess(1);
			
			reorderBufferEntry.setOperand1Available(true);
			return;
		}
		
		int tempOpndPhyReg1 = reorderBufferEntry.getOperand1PhyReg1();
		int tempOpndPhyReg2 = reorderBufferEntry.getOperand1PhyReg2();
		boolean[] opndAvailable = OperandAvailabilityChecker.isAvailable(reorderBufferEntry, tempOpnd, tempOpndPhyReg1, tempOpndPhyReg2, core);
		
		if(tempOpndType == OperandType.integerRegister ||
				tempOpndType == OperandType.floatRegister ||
				tempOpndType == OperandType.machineSpecificRegister)
		{
		
			if(tempOpndType == OperandType.machineSpecificRegister)
			{
				RegisterFile tempRF = execEngine.getMachineSpecificRegisterFile(threadID);
				if(opndAvailable[0] == true)
				{
					//Increment counters for power calculation
					this.core.powerCounters.incrementWindowAccess(1);
					this.core.powerCounters.incrementWindowPregAccess(1);

					reorderBufferEntry.setOperand1Available(true);
					if(opType == OperationType.xchg)
					{
						tempRF.setValueValid(false, tempOpndPhyReg1);
						tempRF.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg1);
						//tempRF.incrementNoOfActiveWriters(tempOpndPhyReg1);
						//2nd operand may be the same register as 1st operand
						if(tempOpndType == instruction.getSourceOperand2().getOperandType()
								&& tempOpndPhyReg1 == reorderBufferEntry.getOperand2PhyReg1())
						{
							reorderBufferEntry.setOperand2Available(true);							
						}
					}
				}
			}
			else if(tempOpndType == OperandType.integerRegister ||
					tempOpndType == OperandType.floatRegister)
			{
				RenameTable tempRN;
				if(tempOpndType	== OperandType.integerRegister)
				{
					tempRN = execEngine.getIntegerRenameTable();
				}
				else
				{
					tempRN = execEngine.getFloatingPointRenameTable();
				}
				
				if(opndAvailable[0] == true)
				{
					//Increment counters for power calculation
					this.core.powerCounters.incrementWindowAccess(1);
					this.core.powerCounters.incrementWindowPregAccess(1);

					reorderBufferEntry.setOperand1Available(true);
						
					if(opType == OperationType.xchg)
					{
						tempRN.setValueValid(false, tempOpndPhyReg1);
						tempRN.getAssociatedRegisterFile().setValueValid(false, tempOpndPhyReg1);
						tempRN.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg1);
						//2nd operand may be the same register as 1st operand
						if(tempOpndType == reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType()
								&& tempOpndPhyReg1 == reorderBufferEntry.getOperand2PhyReg1())
						{
							reorderBufferEntry.setOperand2Available(true);							
						}
					}
				}
				
				if(opType == OperationType.xchg)
				{
					tempRN.getAssociatedRegisterFile().incrementNoOfActiveWriters(tempOpndPhyReg1);					
				}
			}
		}
		
		else if(tempOpndType == OperandType.memory)
		{
			if(opndAvailable[0] == true && opndAvailable[1] == true)
			{
				//Increment counters for power calculation
				this.core.powerCounters.incrementWindowAccess(1);
				this.core.powerCounters.incrementWindowPregAccess(1);

				reorderBufferEntry.setOperand11Available(true);
				reorderBufferEntry.setOperand12Available(true);
				reorderBufferEntry.setOperand1Available(true);
			}
			else
			{
				//reorderBufferEntry.setOperand1Available(false);
				
				if(opndAvailable[0] == true)
				{
					reorderBufferEntry.setOperand11Available(true);
				}
				else
				{
					//reorderBufferEntry.setOperand11Available(false);
				}
				
				if(opndAvailable[1] == true)
				{
					reorderBufferEntry.setOperand12Available(true);
				}
				else
				{
					//reorderBufferEntry.setOperand12Available(false);
				}
			}
		}
	}
	
	void checkOperand2Availability()
	{
		Operand tempOpnd = reorderBufferEntry.getInstruction().getSourceOperand2();
		if(tempOpnd == null ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.inValid ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.nop)
		{
			//Increment counters for power calculation
			this.core.powerCounters.incrementWindowAccess(1);
			this.core.powerCounters.incrementWindowPregAccess(1);

			reorderBufferEntry.setOperand2Available(true);
			return;
		}
		
		OperandType tempOpndType = tempOpnd.getOperandType();
		
		if(tempOpndType == OperandType.immediate)
		{
			//Increment counters for power calculation
			this.core.powerCounters.incrementWindowAccess(1);
			this.core.powerCounters.incrementWindowPregAccess(1);

			reorderBufferEntry.setOperand2Available(true);
			return;
		}
		
		int tempOpndPhyReg1 = reorderBufferEntry.getOperand2PhyReg1();
		int tempOpndPhyReg2 = reorderBufferEntry.getOperand2PhyReg2();
		boolean[] opndAvailable = OperandAvailabilityChecker.isAvailable(reorderBufferEntry, tempOpnd, tempOpndPhyReg1, tempOpndPhyReg2, core);
		
		if(tempOpndType == OperandType.integerRegister ||
				tempOpndType == OperandType.floatRegister ||
				tempOpndType == OperandType.machineSpecificRegister)
		{
		
			if(tempOpndType == OperandType.machineSpecificRegister)
			{
				RegisterFile tempRF = execEngine.getMachineSpecificRegisterFile(threadID);
				if(opndAvailable[0] == true)
				{
					//Increment counters for power calculation
					this.core.powerCounters.incrementWindowAccess(1);
					this.core.powerCounters.incrementWindowPregAccess(1);
					
					reorderBufferEntry.setOperand2Available(true);
					if(opType == OperationType.xchg)
							//|| reorderBufferEntry.getInstruction().getDestinationOperand() != null &&
							//reorderBufferEntry.getInstruction().getDestinationOperand().getValue() == tempOpndPhyReg1)
					{
						tempRF.setValueValid(false, tempOpndPhyReg1);
						tempRF.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg1);
						//tempRF.incrementNoOfActiveWriters(tempOpndPhyReg2);
					}
				}
			}
			else if(tempOpndType == OperandType.integerRegister ||
					tempOpndType == OperandType.floatRegister)
			{
				RenameTable tempRN;
				if(tempOpndType	== OperandType.integerRegister)
				{
					tempRN = execEngine.getIntegerRenameTable();
				}
				else
				{
					tempRN = execEngine.getFloatingPointRenameTable();
				}
				
				if(opndAvailable[0] == true)
				{

					//Increment counters for power calculation
					this.core.powerCounters.incrementWindowAccess(1);
					this.core.powerCounters.incrementWindowPregAccess(1);

					reorderBufferEntry.setOperand2Available(true);
					
					if(opType == OperationType.xchg)
					{
						tempRN.setValueValid(false, tempOpndPhyReg1);
						tempRN.getAssociatedRegisterFile().setValueValid(false, tempOpndPhyReg1);
						tempRN.setProducerROBEntry(reorderBufferEntry, tempOpndPhyReg1);
					}
				}
				
				if(opType == OperationType.xchg &&
						(tempOpndPhyReg1 != reorderBufferEntry.operand1PhyReg1 ||
						tempOpndType != reorderBufferEntry.getInstruction().getOperand1().getOperandType()))
				{
					tempRN.getAssociatedRegisterFile().incrementNoOfActiveWriters(tempOpndPhyReg1);					
				}
			}
		}
		
		else if(tempOpndType == OperandType.memory)
		{
			if(opndAvailable[0] == true && opndAvailable[1] == true)
			{
				//Increment counters for power calculation
				this.core.powerCounters.incrementWindowAccess(1);
				this.core.powerCounters.incrementWindowPregAccess(1);

				reorderBufferEntry.setOperand21Available(true);
				reorderBufferEntry.setOperand22Available(true);
				reorderBufferEntry.setOperand2Available(true);
			}
			else
			{
				//reorderBufferEntry.setOperand2Available(false);
				
				if(opndAvailable[0] == true)
				{
					reorderBufferEntry.setOperand21Available(true);
				}
				else
				{
					//reorderBufferEntry.setOperand21Available(false);
				}
				
				if(opndAvailable[1] == false)
				{
					reorderBufferEntry.setOperand22Available(true);
				}
				else
				{
					//reorderBufferEntry.setOperand22Available(false);
				}
			}
		}
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
	}
	


}
