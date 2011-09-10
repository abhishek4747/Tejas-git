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
import generic.MicroOpsList;


public class FloatingPointDivision implements InstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			MicroOpsList microOpsList) 
	{
		//If no operand is provided to the function, then st(0)
		//and st(1) are the implicit operands
		if(operand1==null && operand2==null	&& operand3==null)
		{
			Operand st0 = Registers.getTopFPRegister();
			Operand st1 = Registers.getSecondTopFPRegister();
			
			microOpsList.appendInstruction(Instruction.getFloatingPointDivision(st0, st1, st0));
		}
		
		//If there is a single operand to the function, then the other implicit 
		//source as well as destination operand is st(0)
		else if(operand1.isFloatRegisterOperand() && 
				operand2==null	&& operand3==null)
		{
			Operand st0 = Registers.getTopFPRegister();
			microOpsList.appendInstruction(Instruction.getFloatingPointDivision(st0, operand1, st0));
		}
		
		//If the only operand is from memory, first fetch the value from the memory into a
		//temporary floating point register. The other implicit source as well as 
		//destination operand is st(0)
		else if(operand1.isMemoryOperand() && 
				operand2==null && operand3==null)
		{
			Operand st0 = Registers.getTopFPRegister();
			
			Operand tempFloatRegister;
			tempFloatRegister = Registers.getTempFloatReg();
			
			microOpsList.appendInstruction(Instruction.getLoadInstruction(operand1,
					tempFloatRegister));
			
			//st(0) = st(0) + tempFloatRegister
			microOpsList.appendInstruction(Instruction.getFloatingPointDivision(st0, 
					tempFloatRegister, st0));
		}
		
		//If there are two operands, both must be floating-point registers.
		else if(operand1.isFloatRegisterOperand() && operand2.isFloatRegisterOperand() &&
				operand3==null)
		{
			microOpsList.appendInstruction(Instruction.getFloatingPointDivision(operand1,
					operand2, operand1));
		}
		
		else
		{
			misc.Error.invalidOperation("Floating Point Division", operand1, operand2, operand3);
		}
	}
}