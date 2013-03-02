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

import emulatorinterface.translator.x86.registers.Registers;

import main.CustomObjectPool;
import main.Main;



public class Operand implements Serializable
{
	// pre-allocated operands
	private static Operand floatRegisterOperands[];
	private static Operand integerRegisterOperands[];
	private static Operand machineSpecificRegisterOperands[];
	private static Operand memoryOperands[][];
	private static Operand immediateOperand;
	
	public static void preAllocateOperands() {
		
		// Create immediate operand
		immediateOperand = new Operand();
		immediateOperand.type = OperandType.immediate;
		immediateOperand.value = -1;
		
		// Create integer registers
		integerRegisterOperands = new Operand[Registers.getMaxIntegerRegisters()];
		for(int i=0; i<Registers.getMaxIntegerRegisters(); i++) {
			integerRegisterOperands[i] = new Operand();
			integerRegisterOperands[i].type = OperandType.integerRegister;
			integerRegisterOperands[i].value = i;
		}
		
		// Create float registers
		floatRegisterOperands = new Operand[Registers.getMaxFloatRegisters()];
		for(int i=0; i<Registers.getMaxFloatRegisters(); i++) {
			floatRegisterOperands[i] = new Operand();
			floatRegisterOperands[i].type = OperandType.floatRegister;
			floatRegisterOperands[i].value = i;
		}
		
		// Create machine specific registers
		machineSpecificRegisterOperands = new Operand[Registers.getMaxMachineSpecificRegisters()];
		for(int i=0; i<Registers.getMaxMachineSpecificRegisters(); i++) {
			machineSpecificRegisterOperands[i] = new Operand();
			machineSpecificRegisterOperands[i].type = OperandType.machineSpecificRegister; 
			machineSpecificRegisterOperands[i].value = i;
		}
		
		// Create memory operands
		// Options : integer-integer, integer-msr, msr-msr
		// order  :  integer-register --- msr --- immediate
		memoryOperands = new Operand[(integerRegisterOperands.length*machineSpecificRegisterOperands.length)+1]
				[(integerRegisterOperands.length*machineSpecificRegisterOperands.length)+1];
		
		// allocate integer-integer operands
		for(int i=0; i<integerRegisterOperands.length; i++) {
			for(int j=0; j<integerRegisterOperands.length; j++) {
				memoryOperands[i][j] = new Operand();
				memoryOperands[i][j].type = OperandType.memory;
				memoryOperands[i][j].memoryLocationFirstOperand = integerRegisterOperands[i];
				memoryOperands[i][j].memoryLocationSecondOperand = integerRegisterOperands[j];
				memoryOperands[i][j].value = -1;
			}
		}
		
		// allocate integer-msr operands
		for(int i=0; i<integerRegisterOperands.length; i++) {
			for(int j=0; j<machineSpecificRegisterOperands.length; j++) {
				memoryOperands[i][j+integerRegisterOperands.length] = new Operand();
				memoryOperands[i][j+integerRegisterOperands.length].memoryLocationFirstOperand = integerRegisterOperands[i];
				memoryOperands[i][j+integerRegisterOperands.length].memoryLocationSecondOperand = machineSpecificRegisterOperands[j];
				memoryOperands[i][j+integerRegisterOperands.length].value = -1;
			}
		}
		
		// allocate msr-msr operands
		for(int i=0; i<machineSpecificRegisterOperands.length; i++) {
			for(int j=0; j<machineSpecificRegisterOperands.length; j++) {
				memoryOperands[i+integerRegisterOperands.length][j+integerRegisterOperands.length] = new Operand();
				memoryOperands[i+integerRegisterOperands.length][j+integerRegisterOperands.length].type = OperandType.memory;
				memoryOperands[i+integerRegisterOperands.length][j+integerRegisterOperands.length].memoryLocationFirstOperand = machineSpecificRegisterOperands[i];
				memoryOperands[i+integerRegisterOperands.length][j+integerRegisterOperands.length].memoryLocationSecondOperand = machineSpecificRegisterOperands[j];
				memoryOperands[i+integerRegisterOperands.length][j+integerRegisterOperands.length].value = -1;
			}
		}
		
		// allocate integer-immediate
		for(int i=0; i<integerRegisterOperands.length; i++) {
			memoryOperands[i][integerRegisterOperands.length+machineSpecificRegisterOperands.length+1] = new Operand();
			memoryOperands[i][integerRegisterOperands.length+machineSpecificRegisterOperands.length+1].type = OperandType.memory;
			memoryOperands[i][integerRegisterOperands.length+machineSpecificRegisterOperands.length+1].memoryLocationFirstOperand = integerRegisterOperands[i];
			memoryOperands[i][integerRegisterOperands.length+machineSpecificRegisterOperands.length+1].memoryLocationSecondOperand = immediateOperand;
			memoryOperands[i][integerRegisterOperands.length+machineSpecificRegisterOperands.length+1].value = -1;
		}
		
		// allocate msr-immediate
		for(int i=0; i<machineSpecificRegisterOperands.length; i++) {
			memoryOperands[i+integerRegisterOperands.length][integerRegisterOperands.length+machineSpecificRegisterOperands.length+1] = new Operand();
			memoryOperands[i+integerRegisterOperands.length][integerRegisterOperands.length+machineSpecificRegisterOperands.length+1].type = OperandType.memory;
			memoryOperands[i+integerRegisterOperands.length][integerRegisterOperands.length+machineSpecificRegisterOperands.length+1].memoryLocationFirstOperand = machineSpecificRegisterOperands[i];
			memoryOperands[i+integerRegisterOperands.length][integerRegisterOperands.length+machineSpecificRegisterOperands.length+1].memoryLocationSecondOperand = immediateOperand;
			memoryOperands[i+integerRegisterOperands.length][integerRegisterOperands.length+machineSpecificRegisterOperands.length+1].value = -1;
		}
		
		// allocate immediate memory operand
		memoryOperands[integerRegisterOperands.length+machineSpecificRegisterOperands.length+1][0] = new Operand();
		memoryOperands[integerRegisterOperands.length+machineSpecificRegisterOperands.length+1][0].memoryLocationFirstOperand = immediateOperand;
		memoryOperands[integerRegisterOperands.length+machineSpecificRegisterOperands.length+1][0].memoryLocationSecondOperand = null;
		memoryOperands[integerRegisterOperands.length+machineSpecificRegisterOperands.length+1][0].value = -1;
	}
	
	
	private OperandType type;
	private long value;			//if operand type is register, value indicates which register
								//if operand type is immediate, value indicates the operand value
	Operand memoryLocationFirstOperand;
	Operand memoryLocationSecondOperand;
	
	public void setMemoryLocationFirstOperand(Operand memoryLocationFirstOperand) {
		this.memoryLocationFirstOperand = memoryLocationFirstOperand;
	}

	public void setMemoryLocationSecondOperand(Operand memoryLocationSecondOperand) {
		this.memoryLocationSecondOperand = memoryLocationSecondOperand;
	}

	public Operand()
	{
		this.value = 0;
		this.memoryLocationFirstOperand = null;
		this.memoryLocationSecondOperand = null;
	}
	 
	public void clear()
	{
		this.value = 0;
		this.memoryLocationFirstOperand = null;
		this.memoryLocationSecondOperand = null;
	}
	
	public Operand(OperandType operandType, long  operandValue)
	{
		this.type = operandType;
		this.value = operandValue;
		
		this.memoryLocationFirstOperand = null;
		this.memoryLocationSecondOperand = null;
	}

	public Operand(OperandType operandType, long  operandValue,
			Operand memoryLocationFirstOperand, Operand memoryOperandSecondOperand)
	{
		this.type = operandType;
		this.value = operandValue;

		this.memoryLocationFirstOperand = memoryLocationFirstOperand;
		this.memoryLocationSecondOperand = memoryOperandSecondOperand;
	}
	
//	/* our copy constructor */
//	public Operand(Operand operand)
//	{
//		this.type=operand.type;
//		this.value=operand.value;
//		
//		if(operand.memoryLocationFirstOperand==null) {
//			this.memoryLocationFirstOperand=null;
//		} else {
//			this.memoryLocationFirstOperand=new Operand(operand.memoryLocationFirstOperand);
//		}
//		
//		if(operand.memoryLocationSecondOperand==null) {
//			this.memoryLocationSecondOperand=null;
//		} else {
//			this.memoryLocationSecondOperand=new Operand(operand.memoryLocationSecondOperand);
//		}
//	}
	
	//all properties of sourceOperand is copied to the current operand
	public void copy(Operand sourceOperand)
	{
		this.type=sourceOperand.type;
		this.value=sourceOperand.value;
		
		this.memoryLocationFirstOperand = sourceOperand.memoryLocationFirstOperand;
		this.memoryLocationSecondOperand = sourceOperand.memoryLocationSecondOperand;
	}
	
	public String toString()
	{
			return ("(" + type + ") " + value);
	}

	public OperandType getOperandType()
	{
		return type;
	}

	public void setValue(long value) 
	{
		if(this.type==OperandType.memory) {
			misc.Error.showErrorAndExit("please do not use value field for memory operand");
		}
		
		this.value = value; 
	}
		
	public long getValue()
	{
		if(this.type==OperandType.memory) {
			misc.Error.showErrorAndExit("please do not use value field for memory operand");
		}
		
		return value;
	}
	
	public Operand getMemoryLocationFirstOperand()
	{
		return this.memoryLocationFirstOperand;
	}
	
	public Operand getMemoryLocationSecondOperand()
	{
		return this.memoryLocationSecondOperand;
	}
	
	public boolean isIntegerRegisterOperand()
	{
		return (this.type == OperandType.integerRegister);
	}
	
	public boolean isMachineSpecificRegisterOperand()
	{
		return (this.type == OperandType.machineSpecificRegister);
	}
	
	public boolean isImmediateOperand()
	{
		return (this.type == OperandType.immediate);
	}
	
	public boolean isMemoryOperand()
	{
		return (this.type == OperandType.memory);
	}
	
	public boolean isFloatRegisterOperand()
	{
		return (this.type == OperandType.floatRegister);
	}
	
	private void set(OperandType operandType, long  operandValue)
	{
		this.type = operandType;
		this.value = operandValue;
		
		this.memoryLocationFirstOperand = null;
		this.memoryLocationSecondOperand = null;
	}

	private void set(OperandType operandType, long  operandValue,
			Operand memoryLocationFirstOperand, Operand memoryOperandSecondOperand)
	{
		this.type = operandType;
		this.value = operandValue;

		this.memoryLocationFirstOperand = memoryLocationFirstOperand;
		this.memoryLocationSecondOperand = memoryOperandSecondOperand;
	}
	
	public static Operand getIntegerRegister(long value)
	{
		return integerRegisterOperands[(int) value];
	}
	
	public static Operand getFloatRegister(long value)
	{
		return floatRegisterOperands[(int) value];
	}
	
	public static Operand getMachineSpecificRegister(long value)
	{
		return machineSpecificRegisterOperands[(int)value];
	}
	
	public static Operand getImmediateOperand()
	{
		return immediateOperand;
	}
	
	public static Operand getMemoryOperand(Operand memoryOperand1, Operand memoryOperand2)
	{
		int index1 = -1, index2 = -1;
		
		// operand1's index
		if (memoryOperand1.type==OperandType.integerRegister) {
			index1 = (int) memoryOperand1.value;
		} else if (memoryOperand1.type==OperandType.machineSpecificRegister) {
			index1 = (int) (memoryOperand1.value + integerRegisterOperands.length);
		} else if (memoryOperand1.type==OperandType.immediate) {
			index1 = integerRegisterOperands.length + floatRegisterOperands.length + 1;
		} else {
			System.out.println("invalid memOp1 = " + memoryOperand1);
		}
		
		// operand2's index
		if (memoryOperand2.type==OperandType.integerRegister) {
			index2 = (int) memoryOperand2.value;
		} else if (memoryOperand2.type==OperandType.machineSpecificRegister) {
			index2 = (int) (memoryOperand2.value + integerRegisterOperands.length);
		} else if (memoryOperand2.type==OperandType.immediate) {
			index2 = integerRegisterOperands.length + floatRegisterOperands.length + 1;
		} else {
			System.out.println("invalid memOp2 = " + memoryOperand2);
		}
		
		return memoryOperands[index1][index2];
	}
	
}