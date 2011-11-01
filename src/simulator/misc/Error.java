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

	Contributors:  Prathmesh Kallurkar, Abhishek Sagar
*****************************************************************************/

package misc;

import emulatorinterface.Newmain;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.shm.SharedMem;
import generic.Operand;

public class Error 
{
	public static void showErrorAndExit(String message)
	{
		System.out.print(message);
		System.exit(0);
	}

	public static void shutDown(String message, IpcBase type) 
	{
		Newmain.process.destroy();
		type.finish();
		System.out.print(message);
		System.exit(0);
	}
	
	public static void invalidOperation(String operation, Operand operand1, 
			Operand operand2, Operand operand3)
	{
		System.out.print("\n\tIllegal operands to a " + operation + ".");
		System.out.print("\n\tOperand1 : " + operand1);
		System.out.print("\tOperand2 : " + operand2);
		System.out.print("\tOperand3 : " + operand3);
		System.exit(0);
	}
	
	public static void invalidOperand(String operandString)
	{
		System.out.print("\n\tInvalid operand string : " + operandString + ".");
		System.exit(0);
	}
}