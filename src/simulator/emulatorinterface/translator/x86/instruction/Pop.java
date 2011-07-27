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
import generic.Operand;
import generic.InstructionList;



public class Pop implements InstructionHandler 
{
	public InstructionList handle(Operand operand1,	Operand operand2, Operand operand3, DynamicInstruction dynamicInstruction) 
	{
		InstructionList microOps = new InstructionList();
		
		// Create stack-pointer and [stack-pointer]
		Operand stackPointer = Registers.getStackPointer();
		Operand stackPointerLocation = Operand.getMemoryOperand(stackPointer, null, dynamicInstruction.getMemoryReadAddress().firstElement());
		

		if((operand1.isIntegerRegisterOperand() || operand1.isMachineSpecificRegisterOperand() || operand1.isMemoryOperand()) &&
			operand2==null && operand3==null)
		{
			if(operand1.isMemoryOperand())
			{
				Registers registers = new Registers(operand1, operand2, operand3);
				Operand temporaryIntegerRegister = registers.getTempIntReg();
				
				//stack to temporary-register
				microOps.appendInstruction(Instruction.getLoadInstruction(stackPointerLocation,	temporaryIntegerRegister));
				
				//temporary-register to memory
				operand1.setMemoryAddress(dynamicInstruction.getMemoryWriteAddress().firstElement());
				microOps.appendInstruction(Instruction.getStoreInstruction(operand1, temporaryIntegerRegister));
			}
			else
			{
				//stack to operand1 directly
				microOps.appendInstruction(Instruction.getLoadInstruction(stackPointerLocation,	operand1));
			}
			
			//stack-pointer = stack-pointer - 4
			microOps.appendInstruction(Instruction.getIntALUInstruction(stackPointer, Operand.getImmediateOperand(), stackPointer));
			
			return microOps;
		}
		else
		{
			misc.Error.invalidOperation("Pop", operand1, operand2, operand3);
			return null;
		}
	}
}