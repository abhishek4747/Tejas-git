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


public class Instruction implements Serializable
{
	private OperationType type;
	private Operand sourceOperand1;
	private Operand sourceOperand2;
	private Operand destinationOperand;
	private long programCounter;
	private boolean branchTaken;
	private long branchTargetAddress;
	
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
			{this.sourceOperand2=new Operand(oldInstruction.sourceOperand1);}
		
		if(oldInstruction.destinationOperand==null)
			{this.destinationOperand=null;}
		else
			{this.destinationOperand=new Operand(oldInstruction.sourceOperand1);}
		
		this.programCounter=oldInstruction.programCounter;
		this.branchTaken=oldInstruction.branchTaken;
		this.branchTargetAddress=oldInstruction.branchTargetAddress;
	}	
	
	public long getProgramCounter()
	{
		return this.programCounter;
	}
	
	public void setProgramCounter(long programCounter) 
	{
		this.programCounter = programCounter;
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
			String.format("%-20s", "IP = " + Long.toHexString(programCounter)) +
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
}