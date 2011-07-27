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

	Contributors:  Prathmesh Kallurkar
*****************************************************************************/

package emulatorinterface.translator.x86.instruction;

import emulatorinterface.DynamicInstruction;
import emulatorinterface.translator.x86.registers.Registers;
import generic.Instruction;
import generic.InstructionList;
import generic.Operand;

public class StringCompare implements InstructionHandler 
{
	public InstructionList handle(Operand operand1, Operand operand2, Operand operand3, DynamicInstruction dynamicInstruction) 
	{
		InstructionList microOps = new InstructionList();
		Registers registers = new Registers(operand1, operand2, operand3);
		
		Operand sourceLocation;
		Operand destinationLocation;
		
		
		if(operand1==null && operand2==null && operand3==null)
		{
			Long sourceIndexLocation = dynamicInstruction.getMemoryReadAddress().get(0);
			Long destinationIndexLocation = dynamicInstruction.getMemoryReadAddress().get(1);
			
			sourceLocation = Operand.getMemoryOperand(Registers.getSourceIndexRegister(),
					null, sourceIndexLocation);
			
			destinationLocation = Operand.getMemoryOperand(Registers.getDestinationIndexRegister(),
					null, destinationIndexLocation);
			
			//Load the value at the sourceLocation in a temporary register
			Operand sourceIndex = registers.getTempIntReg();
			microOps.appendInstruction(Instruction.getLoadInstruction(sourceLocation, sourceIndex));
			
			
			//Load the value at the destination Location in a temporary register
			Operand destinationIndex = registers.getTempIntReg();
			microOps.appendInstruction(Instruction.getLoadInstruction(destinationLocation, destinationIndex));

			//Perform compare operation
			IntegerALUNoImplicitDestination integerALUNoImplicitDestination = 
						new IntegerALUNoImplicitDestination();
			
			microOps.appendInstruction(integerALUNoImplicitDestination.handle(sourceIndex, destinationIndex, 
					null, dynamicInstruction));
			
			return microOps;
		}
		
		
		else if(operand1.isMemoryOperand() && operand2.isMemoryOperand() &&
				operand3==null)
		{
			sourceLocation = operand2;
			destinationLocation = operand1;
			
			//Load the value at the sourceLocation in a temporary register
			Operand sourceIndex = registers.getTempIntReg();
			microOps.appendInstruction(Instruction.getLoadInstruction(sourceLocation, sourceIndex));
			
			//Load the value at the destination Location in a temporary register
			Operand destinationIndex = registers.getTempIntReg();
			microOps.appendInstruction(Instruction.getLoadInstruction(sourceLocation, destinationIndex));

			//Perform compare operation
			IntegerALUNoImplicitDestination integerALUNoImplicitDestination = 
						new IntegerALUNoImplicitDestination();
			
			microOps.appendInstruction(integerALUNoImplicitDestination.handle(sourceIndex, destinationIndex, 
					null, dynamicInstruction));
			
			return microOps;
		}
		
		else
		{
			misc.Error.invalidOperation("String Compare", operand1, operand2, operand3);
			return null;
		}
	}
}