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
import generic.InstructionList;
import generic.Operand;
import misc.Error;;

/**
 * Call
 * 1) pushes the program-counter on the stack.
 * 2) fetch the starting location of the required function in operand1.
 * 3) Perform an unconditional jump to this location.
 * @author prathmesh
 */
public class Call implements InstructionHandler 
{
	public InstructionList handle(Operand operand1,	Operand operand2, Operand operand3, DynamicInstruction dynamicInstruction) 
	{
		InstructionList microOps = new InstructionList();
		
		Operand instructionPointer = Registers.getInstructionPointer();
		microOps.appendInstruction((new Push()).handle(instructionPointer, null, null, dynamicInstruction));
		
		//If operand1 is a non-memory operand, ... 
		if((operand1.isImmediateOperand() ||    operand1.isIntegerRegisterOperand() ||  operand1.isMachineSpecificRegisterOperand() || operand1.isMemoryOperand()) 
		   &&  operand2==null  &&   operand3==null)
		{
			//Unconditional jump to a new location
			microOps.appendInstruction((new UnconditionalJump()).handle(operand1, null, null, dynamicInstruction));
			return microOps;
		}
		
		else
		{
			Error.invalidOperation("Call", operand1, operand2, operand3);
			return null;
		}
	}
}