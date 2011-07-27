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
import emulatorinterface.translator.x86.operand.OperandTranslator;
import emulatorinterface.translator.x86.registers.Registers;
import generic.Instruction;
import generic.Operand;
import generic.InstructionList;


public class FloatingPointLoad implements InstructionHandler 
{
	public InstructionList handle(Operand operand1, Operand operand2, Operand operand3, DynamicInstruction dynamicInstruction) 
	{
		
		InstructionList microOps = new InstructionList();
				
		
		if(operand1.isMemoryOperand() && 
				operand2==null	&& operand3==null)
		{
			//TODO Push operation at present requires just a single move operation
			//may be some more work needs to be done
			microOps.appendInstruction(Instruction.getLoadInstruction(operand1, 
					Registers.getTopFPRegister()));
			
			return microOps;
		}
		
		else if(operand1.isFloatRegisterOperand() && 
				operand2==null	&& operand3==null)
		{
			//TODO Push operation at present requires just a single move operation
			//may be some more work needs to be done
			microOps.appendInstruction(Instruction.getMoveInstruction(
					Registers.getTopFPRegister(), operand1));
			
			return microOps;
		}
		
		else
		{
			misc.Error.invalidOperation("Floating Point Load", operand1, operand2, operand3);
			return null;
		}
	}
}