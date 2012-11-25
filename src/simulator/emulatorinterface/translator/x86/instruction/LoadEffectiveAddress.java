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

import main.CustomObjectPool;
import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.x86.registers.TempRegisterNum;
import generic.Instruction;
import generic.Operand;
import generic.InstructionList;

public class LoadEffectiveAddress implements X86StaticInstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum) 
					throws InvalidInstructionException
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

				operand2.setMemoryLocationSecondOperand(null); // operand2's component must not be returned to the pool
				operand2.setMemoryLocationFirstOperand(null); // operand2's component must not be returned to the pool
				operand2.incrementNumReferences(); // increment number of references to operand2 to one before returning it
				CustomObjectPool.getOperandPool().returnObject(operand2); // if operand2 is not used, ensure that it is returned to the pool
			}
			else
			{
				//operand1=a+b
				instructionArrayList.appendInstruction(Instruction.getIntALUInstruction(operand2.getMemoryLocationFirstOperand(),
						operand2.getMemoryLocationSecondOperand(), operand1));

				operand2.setMemoryLocationSecondOperand(null); // operand2's component must not be returned to the pool
				operand2.setMemoryLocationFirstOperand(null); // operand2's component must not be returned to the pool
				operand2.incrementNumReferences(); // increment number of references to operand2 to one before returning it
				CustomObjectPool.getOperandPool().returnObject(operand2); // if operand2 is not used, ensure that it is returned to the pool
			}
		}
		else
		{
			misc.Error.invalidOperation("Load Effective Address", operand1, operand2, operand3);
		}
	}
}