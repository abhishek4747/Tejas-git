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


import emulatorinterface.translator.x86.registers.Registers;
import generic.Instruction;
import generic.Operand;
import generic.InstructionLinkedList;


public class FloatingPointLoad implements InstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionLinkedList instructionLinkedList) 
	{
		if(operand1.isMemoryOperand() && 
				operand2==null	&& operand3==null)
		{
			//TODO Push operation at present requires just a single move operation
			//may be some more work needs to be done
			instructionLinkedList.appendInstruction(Instruction.getLoadInstruction(operand1, 
					Registers.getTopFPRegister()));
		}
		
		else if(operand1.isFloatRegisterOperand() && 
				operand2==null	&& operand3==null)
		{
			//TODO Push operation at present requires just a single move operation
			//may be some more work needs to be done
			instructionLinkedList.appendInstruction(Instruction.getMoveInstruction(
					Registers.getTopFPRegister(), operand1));
		}
		
		else
		{
			misc.Error.invalidOperation("Floating Point Load", operand1, operand2, operand3);
		}
	}
}