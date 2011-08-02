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

public class SSEALU implements InstructionHandler 
{
	public InstructionList handle(Operand operand1, Operand operand2, Operand operand3, DynamicInstruction dynamicInstruction) 
	{
		InstructionList microOps = new InstructionList();

		if(operand1.isFloatRegisterOperand() && operand2.isFloatRegisterOperand() &&
				operand3==null)
		{
			//operand1 = operand1 + operand2
			microOps.appendInstruction(Instruction.getFloatingPointALU(operand1,
					operand2, operand1));
			return microOps;
		}
		
		else if(operand1.isFloatRegisterOperand() && operand2.isMemoryOperand() &&
				operand3==null)
		{
			Registers registers = new Registers(operand1, operand2, operand3);

			//tempFloatRegister = [operand2]
			Operand tempFloatRegister = registers.getTempFloatReg();
			
			operand2.setMemoryAddress(dynamicInstruction.getMemoryReadAddress().firstElement());
			microOps.appendInstruction(Instruction.getLoadInstruction(operand2,
					tempFloatRegister));
			
			//operand1 = operand1 +/- tempFloatRegister
			microOps.appendInstruction(Instruction.getFloatingPointALU(operand1,
					tempFloatRegister, operand1));
			
			return microOps;
		}
		
		else
		{
			misc.Error.invalidOperation("SSE ALU Operation", operand1, operand2, operand3);
			return null;
		}
	}
}