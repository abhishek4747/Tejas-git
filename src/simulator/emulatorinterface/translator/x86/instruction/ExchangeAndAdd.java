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

import generic.InstructionLinkedList;
import generic.Operand;

public class ExchangeAndAdd implements InstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionLinkedList instructionLinkedList)
	{
		//TODO Check if the add should be performed before exchange ??
		Exchange exchange = new Exchange();
		exchange.handle(instructionPointer, operand1, operand2, operand3, instructionLinkedList);

		//Perhaps the order will now change.
		IntegerALUImplicitDestination addOperation = new IntegerALUImplicitDestination();
		addOperation.handle(instructionPointer, operand2, operand1, operand3, instructionLinkedList);
	}
}
