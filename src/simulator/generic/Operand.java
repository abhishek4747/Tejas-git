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

import main.CustomObjectPool;
import main.Main;



public class Operand implements Serializable
{
	private int numReferrences = 0; 
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
		
		if(sourceOperand.memoryLocationFirstOperand==null) {
			this.memoryLocationFirstOperand=null;
		} else 	{
			this.memoryLocationFirstOperand = CustomObjectPool.getOperandPool().borrowObject();
			this.memoryLocationFirstOperand.copy(sourceOperand.memoryLocationFirstOperand);
		}
		
		if(sourceOperand.memoryLocationSecondOperand==null) {
			this.memoryLocationSecondOperand=null;
		} else 	{
			this.memoryLocationSecondOperand = CustomObjectPool.getOperandPool().borrowObject();
			this.memoryLocationSecondOperand.copy(sourceOperand.memoryLocationSecondOperand);
		}
		
		// we must increment the numReferences of this operand only. Its component's numReferences will be increment in their copy method.
		this.numReferrences++;
	}
	
	public String toString()
	{
			return ("(" + type + ") " + value + " ref=" + numReferrences);
	}

	public OperandType getOperandType()
	{
		return type;
	}

	public void setValue(long value) 
	{
		this.value = value; 
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
		Operand op = CustomObjectPool.getOperandPool().borrowObject();
		op.set(OperandType.integerRegister, value);
		return op;
	}
	
	public static Operand getFloatRegister(long value)
	{
		Operand op = CustomObjectPool.getOperandPool().borrowObject();
		op.set(OperandType.floatRegister, value);
		return op;
	}
	
	public static Operand getMachineSpecificRegister(long value)
	{
		Operand op = CustomObjectPool.getOperandPool().borrowObject();
		op.set(OperandType.machineSpecificRegister, value);
		return op;
	}
	
	public static Operand getImmediateOperand()
	{
		Operand op = CustomObjectPool.getOperandPool().borrowObject();
		op.set(OperandType.immediate, -1);
		return op;
	}
	
	public static Operand getMemoryOperand(Operand memoryOperand1, Operand memoryOperand2)
	{
		Operand op = CustomObjectPool.getOperandPool().borrowObject();
		op.set(OperandType.memory, -1, memoryOperand1, memoryOperand2);
		return op;
	}
	
	public void incrementNumReferences() {
		this.numReferrences++;
		
		if(this.memoryLocationFirstOperand!=null) {
			this.memoryLocationFirstOperand.incrementNumReferences();
		}
		
		if(this.memoryLocationSecondOperand!=null) {
			this.memoryLocationSecondOperand.incrementNumReferences();
		}
	}
	
	public void decrementNumReferences() {
		this.numReferrences--;
	}
	
	public int getNumReferences() {
		return this.numReferrences;
	}
	
	public int getNumDistinctRecursiveReferences() {
		int numDistinctReferences = 0;
		
		if(this.getNumReferences()==1) {
			numDistinctReferences++;
		}
		
		if(this.getMemoryLocationFirstOperand()!=null) {
			numDistinctReferences += getMemoryLocationFirstOperand().getNumDistinctRecursiveReferences();
		}
			
		if(this.getMemoryLocationSecondOperand()!=null) {
			numDistinctReferences += getMemoryLocationSecondOperand().getNumDistinctRecursiveReferences();
		}
		
		return numDistinctReferences;
	}
}