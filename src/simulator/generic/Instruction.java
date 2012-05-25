/*****************************************************************************
				Tejas Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
------------------------------------------------------------------------------------------------------------

	Contributors:  Prathmesh Kallurkar, Rajshekar Kalyappam
*****************************************************************************/
package generic;

import java.io.Serializable;

import emulatorinterface.Newmain;


public class Instruction implements Serializable
{
	private OperationType type;
	private Operand sourceOperand1;
	private Operand sourceOperand2;
	private Operand destinationOperand;
	
	private long riscProgramCounter;
	private long ciscProgramCounter;
	
	private boolean branchTaken;
	private long branchTargetAddress;
	private long serialNo;
	
	public Instruction()
	{
		this.sourceOperand1 = null;
		this.sourceOperand2 = null;
		this.destinationOperand = null;
	}
	
	public Instruction(OperationType type, Operand sourceOperand1,
			Operand sourceOperand2, Operand destinationOperand)
	{
		this.type = type;
		this.sourceOperand1 = sourceOperand1;
		this.sourceOperand2 = sourceOperand2;
		this.destinationOperand = destinationOperand;
	}
	
	/* our clone constructor */
	public Instruction(Instruction oldInstruction)
	{
		this.type=oldInstruction.type;
		
		if(oldInstruction.sourceOperand1==null)
			{this.sourceOperand1=null;}
		else
			{this.sourceOperand1=new Operand(oldInstruction.sourceOperand1);}
		
		if(oldInstruction.sourceOperand2==null)
			{this.sourceOperand2=null;}
		else
			{this.sourceOperand2=new Operand(oldInstruction.sourceOperand2);}
		
		if(oldInstruction.destinationOperand==null)
			{this.destinationOperand=null;}
		else
			{this.destinationOperand=new Operand(oldInstruction.destinationOperand);}
		
		this.riscProgramCounter=oldInstruction.riscProgramCounter;
		this.ciscProgramCounter=oldInstruction.ciscProgramCounter;
		this.branchTaken=oldInstruction.branchTaken;
		this.branchTargetAddress=oldInstruction.branchTargetAddress;
	}
	
	//all properties of sourceInstruction is copied to the current instruction
	public void copy(Instruction sourceInstruction)
	{
		this.type=sourceInstruction.type;
		
		if(sourceInstruction.sourceOperand1==null)
			{this.sourceOperand1=null;}
		else
			{//this.sourceOperand1=new Operand(sourceInstruction.sourceOperand1);
				try {
					this.sourceOperand1 = Newmain.operandPool.borrowObject();
				} catch (Exception e) {
					// TODO what if there are no more objects in the pool??
					e.printStackTrace();
				}
				this.sourceOperand1.copy(sourceInstruction.sourceOperand1);
			}
		
		if(sourceInstruction.sourceOperand2==null)
			{this.sourceOperand2=null;}
		else
			{//this.sourceOperand2=new Operand(sourceInstruction.sourceOperand2);
				try {
					this.sourceOperand2 = Newmain.operandPool.borrowObject();
				} catch (Exception e) {
					// TODO what if there are no more objects in the pool??
					e.printStackTrace();
				}
				this.sourceOperand2.copy(sourceInstruction.sourceOperand2);
			}
		
		if(sourceInstruction.destinationOperand==null)
			{this.destinationOperand=null;}
		else
			{//this.destinationOperand=new Operand(sourceInstruction.destinationOperand);
				try {
					this.destinationOperand = Newmain.operandPool.borrowObject();
				} catch (Exception e) {
					// TODO what if there are no more objects in the pool??
					e.printStackTrace();
				}
				this.destinationOperand.copy(sourceInstruction.destinationOperand);
			}
		
		this.riscProgramCounter=sourceInstruction.riscProgramCounter;
		this.ciscProgramCounter=sourceInstruction.ciscProgramCounter;
		this.branchTaken=sourceInstruction.branchTaken;
		this.branchTargetAddress=sourceInstruction.branchTargetAddress;
	}
	
	public long getCISCProgramCounter()
	{
		return ciscProgramCounter;
	}
	
	public long getRISCProgramCounter()
	{
		return this.riscProgramCounter;
	}
	
	public void setCISCProgramCounter(long programCounter) 
	{
		this.ciscProgramCounter = programCounter;
	}
	
	public void setRISCProgramCounter(long programCounter) 
	{
		this.riscProgramCounter = programCounter;
	}
	
	public void setOperationType(OperationType operationType)
	{
		this.type = operationType;
	}
	
	/**
	 * strInstruction method returns the instruction information in a string.
	 * @return String describing the instruction
	 */
	public String toString()
	{
		return 
		(
			String.format("%-20s", "IP = " + Long.toHexString(riscProgramCounter)) +
			String.format("%-20s", "Op = " + type) +
			String.format("%-40s", "srcOp1 = " + sourceOperand1) +
			String.format("%-40s", "srcOp2 = " + sourceOperand2) +
			String.format("%-40s", "dstOp = " + destinationOperand) 
		);
	}
	
	public OperationType getOperationType()
	{
		return type;
	}
	
	public boolean isBranchTaken() {
		return branchTaken;
	}

	public void setBranchTaken(boolean branchTaken) {
		this.branchTaken = branchTaken;
	}


	public long getBranchTargetAddress() {
		return branchTargetAddress;
	}


	public void setBranchTargetAddress(long branchTargetAddress) {
		this.branchTargetAddress = branchTargetAddress;
	}


	public Operand getSourceOperand1()
	{
		return sourceOperand1;
	}
	
	
	public Operand getSourceOperand2()
	{
		return sourceOperand2;
	}
	
	
	public Operand getDestinationOperand()
	{
		return destinationOperand;
	}
	
	public static Instruction getIntALUInstruction(Operand sourceOperand1, Operand sourceOperand2, Operand destinationOperand)
	{
		return new Instruction(OperationType.integerALU, sourceOperand1, sourceOperand2,
				destinationOperand);
	}

	public static Instruction getMoveInstruction(Operand destinationOperand, Operand sourceOperand)
	{
		return new Instruction(OperationType.mov, sourceOperand, null, destinationOperand);
	}
	
	public static Instruction getNOPInstruction()
	{
		return new Instruction(OperationType.nop, null, null, null);
	}
	
	public static Instruction getIntegerDivisionInstruction(Operand sourceOperand1,
			Operand sourceOperand2, Operand destinationOperand)
	{
		return new Instruction(OperationType.integerDiv, sourceOperand1, sourceOperand2, 
				destinationOperand);
	}
	
	public static Instruction getIntegerMultiplicationInstruction(Operand sourceOperand1,
			Operand sourceOperand2, Operand destinationOperand)
	{
		return new Instruction(OperationType.integerMul, sourceOperand1, sourceOperand2, 
				destinationOperand);
	}
	
	public static Instruction getExchangeInstruction(Operand operand1, Operand operand2)
	{
		return new Instruction(OperationType.xchg, operand1, operand2, null);
	}
	
	public static Instruction getInterruptInstruction(Operand interruptNumber)
	{
		return new Instruction(OperationType.interrupt, interruptNumber, null, null);
	}
	
	public static Instruction getFloatingPointALU(Operand sourceOperand1, 
			Operand sourceOperand2, Operand destinationOperand)
	{
		return new Instruction(OperationType.floatALU, sourceOperand1, sourceOperand2,
				destinationOperand);	
	}
	
	public static Instruction getFloatingPointMultiplication(Operand sourceOperand1, 
			Operand sourceOperand2, Operand destinationOperand)
	{
		return new Instruction(OperationType.floatMul, sourceOperand1, sourceOperand2,
				destinationOperand);	
	}

	public static Instruction getFloatingPointDivision(Operand sourceOperand1, 
			Operand sourceOperand2, Operand destinationOperand)
	{
		return new Instruction(OperationType.floatDiv, sourceOperand1, sourceOperand2,
				destinationOperand);	
	}
	
	
	public static Instruction getBranchInstruction(Operand newInstructionAddress)
	{
		return new Instruction(OperationType.branch, newInstructionAddress, null, null);
	}

	public static Instruction getUnconditionalJumpInstruction(Operand newInstructionAddress)
	{
		return new Instruction(OperationType.jump, newInstructionAddress, null, null);
	}
	
	public static Instruction getLoadInstruction(Operand memoryLocation, Operand destinationRegister)
	{
		return new Instruction(OperationType.load, memoryLocation,	null, destinationRegister);
	}

	public static Instruction getStoreInstruction(Operand memoryLocation, Operand sourceOperand)
	{
		return new Instruction(OperationType.store, memoryLocation, sourceOperand, null);
	}
	
	public Operand getOperand1()
	{
		return sourceOperand1;
	}

	public Operand getOperand2()
	{
		return sourceOperand2;
	}

	public long getSerialNo() {
		return serialNo;
	}

	public void setSerialNo(long serialNo) {
		this.serialNo = serialNo;
	}
}