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
import generic.Instruction;
import generic.InstructionList;
import generic.Operand;

public class Interrupt implements InstructionHandler 
{
	public InstructionList handle(Operand operand1, Operand operand2, Operand operand3, DynamicInstruction dynamicInstruction) 
	{
		InstructionList microOps = new InstructionList();
		
		Error : case basis
		
		if(operand1.isImmediateOperand() && operand2==null && operand3==null)
		{
			microOps.appendInstruction(Instruction.getInterruptInstruction(operand1));
			return microOps;
		}
		else
		{
			misc.Error.invalidOperation("Interrupt", operand1, operand2, operand3);
			return null;
		}
	}
}