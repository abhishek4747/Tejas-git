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

package emulatorinterface.translator.x86.objparser;

import emulatorinterface.DynamicInstructionBuffer;
import emulatorinterface.translator.visaHandler.VisaHandler;
import emulatorinterface.translator.visaHandler.VisaHandlerSelector;
import emulatorinterface.translator.x86.instruction.InstructionClass;
import emulatorinterface.translator.x86.instruction.InstructionClassTable;
import emulatorinterface.translator.x86.instruction.InstructionHandler;
import emulatorinterface.translator.x86.operand.OperandTranslator;
import emulatorinterface.translator.x86.registers.Registers;
import generic.Instruction;
import generic.InstructionLinkedList_x;
import generic.InstructionLinkedList;
import generic.InstructionTable;
import generic.Operand;
import generic.PartialDecodedInstruction;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import misc.Error;
import misc.Numbers;

/**
 * Objparser class contains methods to parse a static executable file and to
 * determine the information of dynamic instructions. The x86 assembly code
 * generated by objdump utility is first parsed to obtain operation, source
 * operands and destination operand. The x86 CISC operations are broken down
 * into corresponding simpler micro-operations which follow load-store
 * architecture. We store the linear address of the instruction and the
 * corresponding micro-operations in a hash-table for faster access to
 * instruction information later on..
 * 
 * @author prathmesh
 */
public class ObjParser 
{
	public static long staticHandled = 0;
	public static long staticNotHandled = 0;
	
	public static long dynamicHandled = 0;
	public static long dynamicNotHandled = 0;
	
	private static InstructionTable instructionTable = null;
	private static InstructionLinkedList instructionLinkedList = null;
	
	/**
	* This method translates a static instruction to dynamic instruction.
	* It takes as arguments - instructionTable, instructionPointer and dynamic
	* Instruction information
	*/

	/**
	* This method parses the object file, and creates a hash-table from the
	* static instructions.
	* @param executableFile
	* @return
	*/
	public static void buildStaticInstructionTable(String executableFile) 
	{
		BufferedReader input;

		// Read the assembly code from the program using object-dump utility
		input = readObjDumpOutput(executableFile);

		// Create a new hash table
		instructionTable = new InstructionTable();

		// Create a instruction class hash-table
		InstructionClassTable instructionClassTable;
		instructionClassTable = new InstructionClassTable();

		String line;
		long instructionPointer;
		String instructionPrefix;
		String operation;
		String operand1, operand2, operand3;
		
		instructionLinkedList = new InstructionLinkedList();
		
		long lineNumber = 0;
		int microOpsIndex = 0;
		
		// Read from the obj-dump output
		while ((line = readNextLineOfObjDump(input)) != null) 
		{
			lineNumber++;

			if (!(isContainingAssemblyCode(line)))
				continue;

			String assemblyCodeTokens[] = tokenizeAssemblyCode(line);

			// read the assembly code tokens
			instructionPointer = Numbers.hexToLong(assemblyCodeTokens[0]);
			
			//initialize different parameters of an instruction.
			if(isInstructionPrefix(assemblyCodeTokens[1]))
			{
				instructionPrefix = assemblyCodeTokens[1];
				operation = assemblyCodeTokens[2];
				operand1 = assemblyCodeTokens[3];
				operand2 = assemblyCodeTokens[4];
				operand3 = null;
			}
			else
			{
				instructionPrefix = null;
				operation = assemblyCodeTokens[1];
				operand1 = assemblyCodeTokens[2];
				operand2 = assemblyCodeTokens[3];
				operand3 = assemblyCodeTokens[4];
			}
			

			// Riscify current instruction
			microOpsIndex = riscifyInstruction( instructionPointer, 
					instructionPrefix, operation, 
					operand1, operand2, operand3, 
					instructionClassTable, instructionLinkedList);

			// add instruction's index into the hash-table
			instructionTable.addInstruction(instructionPointer, microOpsIndex);
		}

		// close the buffered reader
		try {input.close();}
		catch (IOException ioe) {Error.showErrorAndExit("\n\tError in closing the buffered reader !!");}
		
		//System.out.print("\n\tProgram statically parsed.\n");
		//System.out.print("\n\tIts microOps list ...\n");
		//instructionLinkedList.printList();
	}

	private static int riscifyInstruction(
			long instructionPointer, String instructionPrefix, String operation, 
			String operand1Str, String operand2Str, String operand3Str, 
			InstructionClassTable instructionClassTable, InstructionLinkedList instructionLinkedList) 
	{
		int microOpsIndex = instructionLinkedList.length();
		
		//Determine the instruction class for this instruction
		InstructionClass instructionClass;
		instructionClass = InstructionClassTable.getInstructionClass(operation);
		
		// Simplify the operands
		Operand operand1, operand2, operand3;
		
		Registers.noOfIntTempRegs = 0;
		Registers.noOfFloatTempRegs = 0;
		
		operand1 = OperandTranslator.simplifyOperand(operand1Str, instructionLinkedList);
		operand2 = OperandTranslator.simplifyOperand(operand2Str, instructionLinkedList);
		operand3 = OperandTranslator.simplifyOperand(operand3Str, instructionLinkedList);
		
		
		// Obtain a handler for this instruction
		InstructionHandler handler;
		handler = InstructionClassTable.getInstructionClassHandler(instructionClass);
		
		// Handle the instruction
		if(handler!=null)
		{
			handler.handle(instructionPointer, operand1, operand2, operand3, instructionLinkedList);
		}
		
		//now set the ip of all converted instructions to instructionPointer
		for(int i=microOpsIndex; i<instructionLinkedList.length(); i++)
		{
			instructionLinkedList.setProgramCounter(i, instructionPointer);
		}
		
		return microOpsIndex;
	}
	
	//return true if the string is a valid instruction prefix
	private static boolean isInstructionPrefix(String string)
	{
		if(string.matches("rep|repe|repne|repz|repnz|lock"))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	// runs obj-dump utility on the executable file to obtain the assembly code.
	// The obj-dump output is then returned using a buffered reader.
	private static BufferedReader readObjDumpOutput(String executableFileName) {
		BufferedReader input = null;

		try {
			// Generate the command for obj-dump with required command line
			// arguments
			String command[] = { "objdump", "--disassemble", "-Mintel",
					"--prefix-addresses", executableFileName };
			Process p = Runtime.getRuntime().exec(command);

			// read the output of the process in a buffered reader
			input = new BufferedReader(
					new InputStreamReader(p.getInputStream()));
		} catch (IOException ioe) {
			Error
					.showErrorAndExit("\n\tError in running objdump on the executable file !!");
		}

		return input;
	}


	// reads the next line of buffered reader "input"
	private static String readNextLineOfObjDump(BufferedReader input) 
	{
		try 
		{
			return (input.readLine());
		} 
		catch (IOException ioe) 
		{
			Error.showErrorAndExit("\n\tError in reading from the buffered reader containing assembly code !!");
		}

		// we would never reach this statement
		return null;
	}

	// checks if the passed line of objdump output matches the output for an
	// assembly code.
	private static boolean isContainingAssemblyCode(String line) 
	{
		// A valid assembly code line has following pattern
		// linear-address <referrence-address> opcode (operands)
		return line.matches("[0-9a-fA-F]+ <.*> [a-zA-Z]+.*");
	}

	
	// for a line of assembly code, this would return the
	// linear address, operation, operand1,operand2, operand3
	private static String[] tokenizeAssemblyCode(String line) 
	{
		String linearAddress;
		String operation;
		String operand1, operand2, operand3;
		String operands;

		// Initialise all operands to null
		operands = operand1 = operand2 = operand3 = null;

		// Tokenize the line
		StringTokenizer lineTokenizer = new StringTokenizer(line);

		// Read the tokens into required variables
		linearAddress = lineTokenizer.nextToken();
		lineTokenizer.nextToken(); // skip the referred address
		operation = lineTokenizer.nextToken();
		if (lineTokenizer.hasMoreTokens())
			operands = lineTokenizer.nextToken();

		// If the operation has tokens, then break it further.
		if (operands != null) {
			// First join all the operand tokens
			while (lineTokenizer.hasMoreTokens())
				operands = operands + " " + lineTokenizer.nextToken();

			StringTokenizer operandTokenizer = new StringTokenizer(operands,
					",", false);

			if (operandTokenizer.hasMoreTokens())
				operand1 = operandTokenizer.nextToken();
			if (operandTokenizer.hasMoreTokens())
				operand2 = operandTokenizer.nextToken();
			if (operandTokenizer.hasMoreTokens())
				operand3 = operandTokenizer.nextToken();
		}

		return new String[] { linearAddress, operation, operand1, operand2,
				operand3 };
	}

	// prints the assembly code parameters for a particular instruction
	@SuppressWarnings("unused")
	private static void printCodeDetails(Long linearAddress, String operation,
			String operand1, String operand2, String operand3, long lineNumber,
			PartialDecodedInstruction partialDecodedInstruction) 
	{
		System.out.print("\n\n"
				+ String.format("%-180s", " ").replace(" ", "-")
				+ "\n\n"
				+ String.format("%-20s", "Line-number = " + lineNumber
						+ "\tLinear-address = " + linearAddress + "\n")
				+ String.format("%-20s", "Op = " + operation.toString())
				+ String.format("%-40s", "Op1 = " + operand1)
				+ String.format("%-40s", "Op2 = " + operand2)
				+ String.format("%-40s", "Op3 = " + operand3));

		printPartialDecodedInstruction(partialDecodedInstruction);
	}

	private static void printPartialDecodedInstruction(
			PartialDecodedInstruction partialDecodedInstruction) 
	{
		System.out.print("\ninstructionClass = "
				+ partialDecodedInstruction.getInstructionClass()
				+ String.format("%-40s", "\toperand1 = "
						+ partialDecodedInstruction.getOperand1())
				+ String.format("%-40s", "\toperand2 = "
						+ partialDecodedInstruction.getOperand2())
				+ String.format("%-40s", "\toperand3 = "
						+ partialDecodedInstruction.getOperand3())
				+ "\ninstructionList = "
				+ partialDecodedInstruction.getInstructionList());
	}

	public static InstructionLinkedList_x translateInstruction(
			long startInstructionPointer,
			DynamicInstructionBuffer dynamicInstructionBuffer)
	{
		int microOpIndex;
		Instruction microOp;
		VisaHandler visaHandler;
		InstructionLinkedList_x instructionLinkedList_x = new InstructionLinkedList_x();

//		dynamicInstructionBuffer.printBuffer();
		System.out.print("\tEntered translate instruction @ ip = " + Long.toHexString(startInstructionPointer) + "\n");
		
		// traverse dynamicInstruction Buffer to go to a known instruction
		while(true)
		{
			microOpIndex = instructionTable.getMicroOpIndex(startInstructionPointer);
			
			if(microOpIndex==-1)
			{
				// if this instruction was never a part of the executable, just clear buffer and exit.
				dynamicInstructionBuffer.clearBuffer();
				return instructionLinkedList_x;
			}
			
			else if(instructionLinkedList.get(microOpIndex).getProgramCounter()!=startInstructionPointer)
			{
				// if the starting instructions was part of the executable but not decoded, 
				// then gobble all the instructions with this ip and allign the startInstruction pointer 
				// to the next ip.
				dynamicInstructionBuffer.gobbleInstruction(startInstructionPointer);
				
				// go to the next microOpIndex and set startInstructionPointer = microOps ip.
				microOpIndex++;
				startInstructionPointer = instructionLinkedList.get(microOpIndex).getProgramCounter();
			}
			
			else
			{
				break;
			}
		}

		int microOpIndexBefore;
		
		// main translate loop.
		while(true)
		{
			microOp = instructionLinkedList.get(microOpIndex); 
			if(microOp==null)
			{break;}
			
			visaHandler = VisaHandlerSelector.selectHandler(microOp.getOperationType());
			
			microOpIndexBefore = microOpIndex;     //store microOpIndex
			microOpIndex = visaHandler.handle(microOpIndex, instructionTable, microOp, dynamicInstructionBuffer); //handle
			instructionLinkedList_x.appendInstruction(instructionLinkedList.get(microOpIndexBefore)); //append microOp
			
			if(microOpIndex != -1)
			{
				//System.out.print("microOp(" + microOpIndex + ") : " + microOp + "\n");
			}
			else
			{
				instructionLinkedList_x.removeInstructionFromTail(instructionLinkedList.get(microOpIndexBefore).getProgramCounter());
				break;
			}
		}
		
		dynamicInstructionBuffer.clearBuffer();
		
		instructionLinkedList_x.printList();
		return instructionLinkedList_x;
	}
}