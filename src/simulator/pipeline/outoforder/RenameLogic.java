package pipeline.outoforder;

import java.io.FileWriter;
import java.io.IOException;

import config.PowerConfigNew;
import config.SimulationConfig;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GenericCircularQueue;
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
	GenericCircularQueue<ReorderBufferEntry> decodeBuffer;
	GenericCircularQueue<ReorderBufferEntry> renameBuffer;
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
				if(renameBuffer.isFull() == true)
				{
					break;
				}
				
				if(decodeBuffer.peek(0) != null)
				{
					reorderBufferEntry = decodeBuffer.peek(0);
					instruction = reorderBufferEntry.getInstruction();
					threadID = reorderBufferEntry.getThreadID();
					
					//check if the instruction can be assigned a destination register
					if(canDestOperandBeProcessed(reorderBufferEntry))
					{
						//find out which physical registers correspond
						//to the source operands
						processOperand1(reorderBufferEntry);
						processOperand2(reorderBufferEntry);
						
						//check for availability of source operands
						checkOperand1Availability();
						if(reorderBufferEntry.isOperand2Available() == false)
						{
							checkOperand2Availability();
						}
						
						//assign register for destination operand(s)
						processDestOperand(reorderBufferEntry);
						
						renameBuffer.enqueue(decodeBuffer.dequeue());
						reorderBufferEntry.setRenameDone(true);
						
						execEngine.setToStall2(false);
						
						if(SimulationConfig.debugMode)
						{
							System.out.println("renamed : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + reorderBufferEntry.getInstruction());
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

	/*
	 * find the physical register(s) corresponding to operand 1
	 */
	private void processOperand1(ReorderBufferEntry reorderBufferEntry)
	{
		if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg)
		{
			//handled in processDestOperand()
			return;
		}
		
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
		int archReg;
		if(tempOpndType == OperandType.integerRegister)
		{
			archReg = (int) tempOpnd.getValue();
			reorderBufferEntry.setOperand1PhyReg1(execEngine.getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
			reorderBufferEntry.setOperand1PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.floatRegister)
		{
			archReg = (int) tempOpnd.getValue();
			reorderBufferEntry.setOperand1PhyReg1(execEngine.getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
			reorderBufferEntry.setOperand1PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.machineSpecificRegister)
		{
			archReg = (int) tempOpnd.getValue();
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
					reorderBufferEntry.setOperand1PhyReg1(execEngine.getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.floatRegister)
				{
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
					reorderBufferEntry.setOperand1PhyReg2(execEngine.getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.floatRegister)
				{
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
	
	/*
	 * find the physical register(s) corresponding to operand 2
	 */
	private void processOperand2(ReorderBufferEntry reorderBufferEntry)
	{
		if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg)
		{
			//handled in processDestOperand()
			return;
		}
		
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
		int archReg;
		if(tempOpndType == OperandType.integerRegister)
		{
			archReg = (int) tempOpnd.getValue();
			reorderBufferEntry.setOperand2PhyReg1(execEngine.getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
			reorderBufferEntry.setOperand2PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.floatRegister)
		{
			archReg = (int) tempOpnd.getValue();
			reorderBufferEntry.setOperand2PhyReg1(execEngine.getFloatingPointRenameTable().getPhysicalRegister(threadID, archReg));
			reorderBufferEntry.setOperand2PhyReg2(-1);
		}
		else if(tempOpndType == OperandType.machineSpecificRegister)
		{
			archReg = (int) tempOpnd.getValue();
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
					reorderBufferEntry.setOperand2PhyReg1(execEngine.getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.floatRegister)
				{
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
					reorderBufferEntry.setOperand2PhyReg2(execEngine.getIntegerRenameTable().getPhysicalRegister(threadID, archReg));
				}
				else if(tempOpndType == OperandType.floatRegister)
				{
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
	
	/*
	 * find if registers are available for the destination operand(s)
	 * note : actual allocation isn't done at this point
	 */
	private boolean canDestOperandBeProcessed(ReorderBufferEntry reorderBufferEntry)
	{
		Operand tempOpnd = reorderBufferEntry.getInstruction().getDestinationOperand();
		if(tempOpnd == null ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.inValid ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.nop)
		{
			return true;
		}
		
		if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg)
		{
			//renaming should succeed for both operands or none at all
			boolean bothOpndsPossible = true;
			int numMSRsRequired = 0;
			int numIntRegsRequired = 0;
			int numFloatRegsRequired = 0;
			
			if(reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.machineSpecificRegister)
			{
				numMSRsRequired++;
			}
			else if(reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.integerRegister)
			{
				numIntRegsRequired++;
			}
			else if(reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.floatRegister)
			{
				numFloatRegsRequired++;
			}
			if(reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.machineSpecificRegister)
			{
				numMSRsRequired++;
			}
			else if(reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.integerRegister)
			{
				numIntRegsRequired++;
			}
			else if(reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.floatRegister)
			{
				numFloatRegsRequired++;
			}
			
			if(numMSRsRequired > 0)
			{
				if(reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType() == OperandType.machineSpecificRegister)
				{
					if(execEngine.getMachineSpecificRegisterFile(threadID).getValueValid((int)reorderBufferEntry.getInstruction().getSourceOperand1().getValue()) == false
							&& execEngine.getMachineSpecificRegisterFile(threadID).getProducerROBEntry((int)reorderBufferEntry.getInstruction().getSourceOperand1().getValue()) != reorderBufferEntry)
					{
						bothOpndsPossible = false;
					}
				}
				if(reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType() == OperandType.machineSpecificRegister)
				{
					if(execEngine.getMachineSpecificRegisterFile(threadID).getValueValid((int)reorderBufferEntry.getInstruction().getSourceOperand2().getValue()) == false
							&& execEngine.getMachineSpecificRegisterFile(threadID).getProducerROBEntry((int)reorderBufferEntry.getInstruction().getSourceOperand2().getValue()) != reorderBufferEntry)
					{
						bothOpndsPossible = false;
					}
				}
			}
			
			if(execEngine.getIntegerRenameTable().getAvailableListSize() < numIntRegsRequired)
			{
				bothOpndsPossible = false;
			}
			
			if(execEngine.getFloatingPointRenameTable().getAvailableListSize() < numFloatRegsRequired)
			{
				bothOpndsPossible = false;
			}
			
			if(bothOpndsPossible == false)
			{
				return false;
			}
			else
			{
				return true;
			}
		}
		
		if(tempOpnd.getOperandType() == OperandType.machineSpecificRegister)
		{
			if(execEngine.getMachineSpecificRegisterFile(threadID).getValueValid((int)tempOpnd.getValue()) == true
					/*|| execEngine.getMachineSpecificRegisterFile(threadID).getProducerROBEntry((int)tempOpnd.getValue()) == reorderBufferEntry*/)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		
		if(tempOpnd.getOperandType() == OperandType.integerRegister)
		{
			if(execEngine.getIntegerRenameTable().getAvailableListSize() >= 1)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		
		if(tempOpnd.getOperandType() == OperandType.floatRegister)
		{
			if(execEngine.getFloatingPointRenameTable().getAvailableListSize() >= 1)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		
		return true;		
	}
	
	/*
	 * perform renaming to obtain physical register(s) for the destination operand(s)
	 */
	private boolean processDestOperand(ReorderBufferEntry reorderBufferEntry)
	{
		Operand tempOpnd;
		OperandType tempOpndType;
		
		if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg)
		{
			tempOpnd = reorderBufferEntry.getInstruction().getSourceOperand1();
			tempOpndType = tempOpnd.getOperandType();
			
			boolean op1handled = false;
			
			if(tempOpndType == OperandType.machineSpecificRegister)
			{
				op1handled = handleMSR(reorderBufferEntry, 2);
			}
			else
			{
				op1handled = handleIntFloat(reorderBufferEntry, 2);
			}
			
			if(op1handled == true)
			{
				tempOpnd = reorderBufferEntry.getInstruction().getSourceOperand2();
				tempOpndType = tempOpnd.getOperandType();
				
				if(tempOpndType == reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType() &&
						tempOpnd.getValue() == reorderBufferEntry.getInstruction().getSourceOperand1().getValue())
				{
					//of the form xchg rx, rx
					reorderBufferEntry.setOperand2PhyReg1(reorderBufferEntry.getOperand1PhyReg1());
					return true;
				}
				
				if(tempOpndType == OperandType.machineSpecificRegister)
				{
					return handleMSR(reorderBufferEntry, 3);
				}
				else
				{
					return handleIntFloat(reorderBufferEntry, 3);
				}
			}
			else
			{
				return false;
			}
		}
		
		
		tempOpnd = reorderBufferEntry.getInstruction().getDestinationOperand();
		if(tempOpnd == null ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.inValid ||
				reorderBufferEntry.getInstruction().getOperationType() == OperationType.nop)
		{
			return true;
		}

		tempOpndType = tempOpnd.getOperandType();
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
				return handleMSR(reorderBufferEntry, 1);				
			}			
			else
			{
				return handleIntFloat(reorderBufferEntry, 1);				
			}
		}
	}
	
	boolean handleMSR(ReorderBufferEntry reorderBufferEntry, int whichOperand)		//whichOperand : 1 = dest; 2 = srcOp1; 3 = srcOp2
	{
		RegisterFile tempRF = execEngine.getMachineSpecificRegisterFile(threadID);
		Operand tempOpnd;
		int destPhyReg;
		
		if(whichOperand == 1)
		{
			tempOpnd = reorderBufferEntry.getInstruction().getDestinationOperand();
			destPhyReg = (int) tempOpnd.getValue();
		}
		else if(whichOperand == 2)
		{
			tempOpnd = reorderBufferEntry.getInstruction().getSourceOperand1();
			destPhyReg = (int) tempOpnd.getValue();
		}
		else if(whichOperand == 3)
		{
			tempOpnd = reorderBufferEntry.getInstruction().getSourceOperand2();
			destPhyReg = (int) tempOpnd.getValue();			
		}
		else
		{
			System.err.println("invalid whichoperand!");
			return true;
		}
		
		
		if(tempRF.getValueValid(destPhyReg) == true
				/*|| tempRF.getProducerROBEntry(destPhyReg) == reorderBufferEntry*/)
		{
			//destination MSR available
			if(whichOperand == 1)
			{
				reorderBufferEntry.setPhysicalDestinationRegister(destPhyReg);				
			}
			else if(whichOperand == 2)
			{
				reorderBufferEntry.setOperand1PhyReg1(destPhyReg);				
			}
			else if(whichOperand == 3)
			{
				reorderBufferEntry.setOperand2PhyReg1(destPhyReg);
			}
			else
			{
				System.err.println("invalid whichoperand!");
				return true;
			}
			tempRF.setProducerROBEntry(reorderBufferEntry, destPhyReg);
			tempRF.setValueValid(false, destPhyReg);
			execEngine.setToStall6(false);
			
			return true;
		}
		
		else
		{
			//stall decode because physical register for destination was not allocated
			execEngine.setToStall6(true);
			return false;
		}
	}
	
	boolean handleIntFloat(ReorderBufferEntry reorderBufferEntry, int whichOperand)
	{
		RenameTable tempRN;
		OperandType tempOpndType;
		int registerNumber;
		
		if(whichOperand == 1)
		{
			tempOpndType = reorderBufferEntry.getInstruction().
									getDestinationOperand().getOperandType();
			registerNumber = (int) reorderBufferEntry.getInstruction().getDestinationOperand().getValue();
		}
		else if(whichOperand == 2)
		{
			tempOpndType = reorderBufferEntry.getInstruction().getSourceOperand1().getOperandType();
			registerNumber = (int) reorderBufferEntry.getInstruction().getSourceOperand1().getValue();
		}
		else if(whichOperand == 3)
		{
			tempOpndType = reorderBufferEntry.getInstruction().getSourceOperand2().getOperandType();
			registerNumber = (int) reorderBufferEntry.getInstruction().getSourceOperand2().getValue();
		}
		else
		{
			System.err.println("invalid whichOperand!");
			return true;
		}
		
		if(tempOpndType == OperandType.integerRegister)
		{
			tempRN = execEngine.getIntegerRenameTable();
		}
		else
		{
			tempRN = execEngine.getFloatingPointRenameTable();
		}
		
		int r = tempRN.allocatePhysicalRegister(threadID, registerNumber);
		if(r >= 0)
		{
			//physical register found
			
			if(whichOperand == 1)
			{
				reorderBufferEntry.setPhysicalDestinationRegister(r);
			}
			else if(whichOperand == 2)
			{
				reorderBufferEntry.setOperand1PhyReg1(r);
			}
			else if(whichOperand == 3)
			{
				reorderBufferEntry.setOperand2PhyReg1(r);
			}
			else
			{
				System.err.println("invalid whichOperand!");
				return true;
			}
			tempRN.setValueValid(false, r);
			tempRN.setProducerROBEntry(reorderBufferEntry, r);
			execEngine.setToStall2(false);
			
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
		if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg)
		{
			//handled in processDestOperand()
			reorderBufferEntry.setOperand1Available(true);
			return;
		}
		
		Operand tempOpnd = instruction.getSourceOperand1();
		
		if(tempOpnd == null
				|| reorderBufferEntry.getInstruction().getOperationType() == OperationType.inValid
				|| reorderBufferEntry.getInstruction().getOperationType() == OperationType.nop
				|| tempOpnd.getOperandType() == OperandType.immediate)
		{
			reorderBufferEntry.setOperand1Available(true);
			return;
		}
		
		int tempOpndPhyReg1 = reorderBufferEntry.getOperand1PhyReg1();
		int tempOpndPhyReg2 = reorderBufferEntry.getOperand1PhyReg2();
		boolean[] opndAvailable = OperandAvailabilityChecker.isAvailable(reorderBufferEntry, tempOpnd, tempOpndPhyReg1, tempOpndPhyReg2, core);
		
		OperandType tempOpndType = tempOpnd.getOperandType();
		if(tempOpndType == OperandType.integerRegister ||
				tempOpndType == OperandType.floatRegister ||
				tempOpndType == OperandType.machineSpecificRegister)
		{		
			if(opndAvailable[0] == true)
			{
				reorderBufferEntry.setOperand1Available(true);
			}
		}
		
		else if(tempOpndType == OperandType.memory)
		{
			reorderBufferEntry.setOperand11Available(opndAvailable[0]);
			reorderBufferEntry.setOperand12Available(opndAvailable[1]);
			reorderBufferEntry.setOperand1Available(opndAvailable[0] && opndAvailable[1]);
		}
	}
	
	void checkOperand2Availability()
	{
		if(reorderBufferEntry.getInstruction().getOperationType() == OperationType.xchg)
		{
			//handled in processDestOperand()
			reorderBufferEntry.setOperand2Available(true);
			return;
		}
		
		Operand tempOpnd = reorderBufferEntry.getInstruction().getSourceOperand2();
		
		if(tempOpnd == null
				|| reorderBufferEntry.getInstruction().getOperationType() == OperationType.inValid
				|| reorderBufferEntry.getInstruction().getOperationType() == OperationType.nop
				|| tempOpnd.getOperandType() == OperandType.immediate)
		{
			reorderBufferEntry.setOperand2Available(true);
			return;
		}
		
		int tempOpndPhyReg1 = reorderBufferEntry.getOperand2PhyReg1();
		int tempOpndPhyReg2 = reorderBufferEntry.getOperand2PhyReg2();
		boolean[] opndAvailable = OperandAvailabilityChecker.isAvailable(reorderBufferEntry, tempOpnd, tempOpndPhyReg1, tempOpndPhyReg2, core);
		
		OperandType tempOpndType = tempOpnd.getOperandType();
		if(tempOpndType == OperandType.integerRegister ||
				tempOpndType == OperandType.floatRegister ||
				tempOpndType == OperandType.machineSpecificRegister)
		{
			if(opndAvailable[0] == true)
			{
				reorderBufferEntry.setOperand2Available(true);
			}
		}
		
		else if(tempOpndType == OperandType.memory)
		{
			reorderBufferEntry.setOperand21Available(opndAvailable[0]);
			reorderBufferEntry.setOperand22Available(opndAvailable[1]);
			reorderBufferEntry.setOperand2Available(opndAvailable[0] && opndAvailable[1]);
		}
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
	}
	
	public PowerConfigNew calculateAndPrintPower(FileWriter outputFileWriter, String componentName) throws IOException
	{
		PowerConfigNew intRenamePower = execEngine.getIntegerRenameTable().calculateAndPrintPower(outputFileWriter, "Integer Rename");
		PowerConfigNew floatRenamePower = execEngine.getIntegerRenameTable().calculateAndPrintPower(outputFileWriter, "Floating Point Rename");
		
		PowerConfigNew totalPower = new PowerConfigNew(0, 0);
		totalPower.add(totalPower,  intRenamePower);
		totalPower.add(totalPower,  floatRenamePower);
		
		outputFileWriter.write("\n" + componentName + " :\n" + totalPower + "\n");
		
		return totalPower;
	}
	


}
