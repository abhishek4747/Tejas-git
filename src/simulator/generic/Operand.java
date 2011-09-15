/*****************************************************************************
				BhartiSim Simulator
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


public class Operand 
{
	
	private OperandType type;
	private long value;			//if operand type is register, value indicates which register
								//if operand type is immediate, value indicates the operand value
	
	Operand memoryLocationFirstOperand;
	Operand memoryLocationSecondOperand;
	
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
	
	public String toString()
	{
			return ("(" + type + ")" + Long.toHexString(value));
	}

	public OperandType getOperandType()
	{
		return type;
	}
		
	public long getValue()
	{
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
	
	public static Operand getIntegerRegister(long value)
	{
		return new Operand(OperandType.integerRegister, value);
	}
	
	public static Operand getFloatRegister(long value)
	{
		return new Operand(OperandType.floatRegister, value);	
	}
	
	public static Operand getMachineSpecificRegister(long value)
	{
		return new Operand(OperandType.machineSpecificRegister, value);	
	}
	
	public static Operand getImmediateOperand()
	{
		return new Operand(OperandType.immediate, -1);
	}
	
	public static Operand getMemoryOperand(Operand memoryOperand1, Operand memoryOperand2)
	{
		return new Operand(OperandType.memory, -1, memoryOperand1, memoryOperand2);
	}

	public void setValue(long value) 
	{
		this.value = value; 
	}
}