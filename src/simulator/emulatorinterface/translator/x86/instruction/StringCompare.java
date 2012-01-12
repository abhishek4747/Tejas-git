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

	Contributors:  Prathmesh Kallurkar
*****************************************************************************/

package emulatorinterface.translator.x86.instruction;

import emulatorinterface.translator.x86.registers.Registers;
import generic.Instruction;
import generic.Operand;
import generic.InstructionArrayList;

public class StringCompare implements InstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionArrayList instructionArrayList)
	{
		Operand sourceLocation;
		Operand destinationLocation;
		
		
		if(operand1==null && operand2==null && operand3==null)
		{
			sourceLocation = Operand.getMemoryOperand(Registers.getSourceIndexRegister(),
					null);
			
			destinationLocation = Operand.getMemoryOperand(Registers.getDestinationIndexRegister(),
					null);
			
			//Load the value at the sourceLocation in a temporary register
			Operand sourceIndex = Registers.getTempIntReg();
			instructionArrayList.appendInstruction(Instruction.getLoadInstruction(sourceLocation, sourceIndex));
			
			
			//Load the value at the destination Location in a temporary register
			Operand destinationIndex = Registers.getTempIntReg();
			instructionArrayList.appendInstruction(Instruction.getLoadInstruction(destinationLocation, destinationIndex));

			//Perform compare operation
			IntegerALUExplicitDestination integerALUExplicitDestination = 
						new IntegerALUExplicitDestination();
			
			integerALUExplicitDestination.handle(instructionPointer, sourceIndex, 
					destinationIndex, null, instructionArrayList);
		}
		
		
		else if(operand1.isMemoryOperand() && operand2.isMemoryOperand() &&
				operand3==null)
		{
			sourceLocation = operand2;
			destinationLocation = operand1;
			
			//Load the value at the sourceLocation in a temporary register
			Operand sourceIndex = Registers.getTempIntReg();
			instructionArrayList.appendInstruction(Instruction.getLoadInstruction(sourceLocation, sourceIndex));
			
			//Load the value at the destination Location in a temporary register
			Operand destinationIndex = Registers.getTempIntReg();
			instructionArrayList.appendInstruction(Instruction.getLoadInstruction(sourceLocation, destinationIndex));

			//Perform compare operation
			IntegerALUExplicitDestination integerALUExplicitDestination = 
						new IntegerALUExplicitDestination();
			
			integerALUExplicitDestination.handle(instructionPointer, sourceIndex, 
					destinationIndex, null, instructionArrayList);
		}
		
		else
		{
			misc.Error.invalidOperation("String Compare", operand1, operand2, operand3);
		}
	}
}