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
import generic.InstructionList;
import generic.Operand;

public class ExchangeAndAdd implements InstructionHandler 
{
	public InstructionList handle(Operand operand1, Operand operand2, Operand operand3, DynamicInstruction dynamicInstruction) 
	{
		InstructionList microOps = new InstructionList();

		Error : case basis
		
		//TODO Check if the add should be performed before exchange ??
		Exchange exchange = new Exchange();
		microOps.appendInstruction(exchange.handle(operand1, operand2, operand3, dynamicInstruction));

		IntegerALUImplicitDestination addOperation = new IntegerALUImplicitDestination();
		microOps.appendInstruction(addOperation.handle(operand2, operand1, operand3, dynamicInstruction));
		
		return microOps;
	}
}
