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

public class ShiftOperationThreeOperand implements InstructionHandler 
{
	public InstructionList handle(Operand operand1, Operand operand2, Operand operand3, DynamicInstruction dynamicInstruction) 
	{
		InstructionList microOps = new InstructionList();
		Registers registers = new Registers(operand1, operand2, operand3);

		
		if(
		   (operand1.isIntegerRegisterOperand() || operand1.isMachineSpecificRegisterOperand() || operand1.isMemoryOperand()) &&
		   (operand2.isIntegerRegisterOperand() || operand2.isMachineSpecificRegisterOperand()) &&
		   (operand3.isIntegerRegisterOperand() || operand3.isMachineSpecificRegisterOperand() || operand3.isImmediateOperand()))
		{
			Operand destination;
			if(operand1.isMemoryOperand())
			{
				destination = registers.getTempIntReg();
			}
			else
			{
				destination = operand1;
			}
			
			microOps.appendInstruction(Instruction.getIntALUInstruction(
					operand2, operand3, destination));
			
			
			if(operand1.isMemoryOperand())
			{
				microOps.appendInstruction(Instruction.getStoreInstruction(operand1, destination));
			}
			
			return microOps;
		}
		else
		{
			misc.Error.invalidOperation("Shift", operand1, operand2, operand3);
			return null;
		}
	}
}