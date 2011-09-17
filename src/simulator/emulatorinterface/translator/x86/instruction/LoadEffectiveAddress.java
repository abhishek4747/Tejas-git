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

import generic.Instruction;
import generic.Operand;
import generic.InstructionArrayList;

public class LoadEffectiveAddress implements InstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionArrayList instructionArrayList)
	{
		//Effective address is of the form [a+b]
		if((operand1.isIntegerRegisterOperand() || operand1.isMachineSpecificRegisterOperand() ) && 
				operand2.isMemoryOperand() && operand3==null)
		{
			if(operand2.getMemoryLocationSecondOperand()==null)
			{
				//If b is invalid operand, operand1 = a
				instructionArrayList.appendInstruction(Instruction.getMoveInstruction(operand1, 
						operand2.getMemoryLocationFirstOperand()));
			}
			else
			{
				//operand1=a+b
				instructionArrayList.appendInstruction(Instruction.getIntALUInstruction(operand2.getMemoryLocationFirstOperand(),
						operand2.getMemoryLocationSecondOperand(), operand1));
			}
		}
		else
		{
			misc.Error.invalidOperation("Load Effective Address", operand1, operand2, operand3);
		}
	}
}