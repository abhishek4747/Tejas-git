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

package emulatorinterface.translator.x86.objparser;

import emulatorinterface.DynamicInstructionBuffer;
import emulatorinterface.EmulatorPacketList;
import emulatorinterface.communication.Encoding;
import emulatorinterface.communication.Packet;
import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.qemuTranslationCache.TranslatedInstructionCache;
import emulatorinterface.translator.visaHandler.DynamicInstructionHandler;
import emulatorinterface.translator.visaHandler.VisaHandlerSelector;
import emulatorinterface.translator.x86.instruction.InstructionClass;
import emulatorinterface.translator.x86.instruction.InstructionClassTable;
import emulatorinterface.translator.x86.instruction.X86StaticInstructionHandler;
import emulatorinterface.translator.x86.operand.OperandTranslator;
import emulatorinterface.translator.x86.registers.Registers;
import emulatorinterface.translator.x86.registers.TempRegisterNum;
import generic.CustomOperandPool;
import generic.GenericCircularQueue;
import generic.Instruction;
import generic.InstructionList;
import generic.InstructionTable;
import generic.Operand;
import generic.PartialDecodedInstruction;
import generic.Statistics;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;
import config.EmulatorConfig;
import main.CustomObjectPool;
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
	private static InstructionTable ciscIPtoRiscIP = null;
	private static InstructionList staticMicroOpList = null;
	private static InstructionList threadMicroOpsList[] = null;
	
	public static void initializeThreadMicroOpsList(int maxApplicationThreads) {
		threadMicroOpsList = new InstructionList[maxApplicationThreads];
		
		for(int i=0; i<maxApplicationThreads; i++) {
			threadMicroOpsList[i] = new InstructionList(10000);
		}
	}
	
	private static DynamicInstructionBuffer[] staticDynamicInstructionBuffers;
	public static void initializeDynamicInstructionBuffer(int maxApplicationThreads) {
		staticDynamicInstructionBuffers = new DynamicInstructionBuffer[maxApplicationThreads];
		
		for(int i=0; i<maxApplicationThreads; i++) {
			staticDynamicInstructionBuffers[i] = new DynamicInstructionBuffer();
		}
	}
	
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

		long noOfLines = noOfLines(executableFile);
		if(noOfLines==0) {
			misc.Error.showErrorAndExit("error in reading the output of objdump on " + executableFile);
		}

		// Read the assembly code from the program using object-dump utility
		input = readObjDumpOutput(executableFile);

		// Create a new hash table
		ciscIPtoRiscIP = new InstructionTable((int)noOfLines);

		System.out.println("The executable has " + noOfLines + " assembly lines");
		
		String line;
		long instructionPointer;
		String instructionPrefix;
		String operation;
		String operand1, operand2, operand3;
		
		staticMicroOpList = new InstructionList((int)noOfLines*3);
		
		int microOpsIndex = 0;
		
		long handled = 0, notHandled = 0;
		
		// Read from the obj-dump output
		while ((line = readNextLineOfObjDump(input)) != null) 
		{
//			System.out.println("Number of lines = " + (handled+notHandled) + 
//					" and size of array-list = " + staticMicroOpList.getListSize());
			
			if (!(isContainingObjDumpAssemblyCode(line))) {
				continue;
			}

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
			int microOpsIndexBefore = microOpsIndex;
			int numRicscInsn = riscifyInstruction( instructionPointer, 
					instructionPrefix, operation, 
					operand1, operand2, operand3, 
					staticMicroOpList);
			microOpsIndex += numRicscInsn;
			
			if(microOpsIndexBefore==microOpsIndex) {
				notHandled++;
			} else {
				handled++;
			}

			// add instruction's index into the hash-table
			ciscIPtoRiscIP.addInstruction(instructionPointer, microOpsIndexBefore);
		}
		
		System.out.println("Total number of assembly lines = " + (handled + notHandled)); 
		System.out.println("Total number of micro-operations = " + staticMicroOpList.length());

		// close the buffered reader
		try {input.close();}
		catch (IOException ioe) {Error.showErrorAndExit("\n\tError in closing the buffered reader !!");}

		Statistics.setStaticCoverage(((double)handled/(double)(handled+notHandled))*(double)100);
	}

	private static int riscifyInstruction(
			long instructionPointer, String instructionPrefix, String operation, 
			String operand1Str, String operand2Str, String operand3Str, 
			InstructionList instructionList) 
	{
//		if(instructionPointer==4222125) {
//			System.out.println("ip=" + instructionPointer + "\tprefix=" + instructionPrefix + 
//					"\top=" + operation + "\top1=" + operand1Str + "\top2=" + operand2Str + "\top3=" + operand3Str);
//		}
		
		int previousPoolSize = CustomObjectPool.getOperandPool().getSize();
		int previousPoolCapacity = CustomObjectPool.getOperandPool().getPoolCapacity();
		
		int numDistinctOperand = 0;
		int microOpsIndexBefore = instructionList.length();
		Operand operand1 = null, operand2 = null, operand3 = null;

		
		// System.out.println("instructionList size before = " + microOpsIndexBefore);
		
		try
		{
			//Determine the instruction class for this instruction
			InstructionClass instructionClass;
			instructionClass = InstructionClassTable.getInstructionClass(operation);

			// Obtain a handler for this instruction
			X86StaticInstructionHandler handler;
			handler = InstructionClassTable.getInstructionClassHandler(instructionClass);
			
			// Handle the instruction
			if(handler!=null) {
				// Simplify the operands
	
				TempRegisterNum tempRegisterNum = new TempRegisterNum();
				
				operand1 = OperandTranslator.simplifyOperand(operand1Str, instructionList, tempRegisterNum);
				operand2 = OperandTranslator.simplifyOperand(operand2Str, instructionList, tempRegisterNum);
				operand3 = OperandTranslator.simplifyOperand(operand3Str, instructionList, tempRegisterNum);
				
				handler.handle(instructionPointer, operand1, operand2, operand3, instructionList, tempRegisterNum);
				
				//now set the ip of all converted instructions to instructionPointer
				for(int i=microOpsIndexBefore; i<instructionList.length(); i++)
				{
					instructionList.setCISCProgramCounter(i, instructionPointer);
					//FIXME : index in the array list - check ??
					instructionList.setRISCProgramCounter(i, i);
					
					// increment references for each argument
					if(instructionList.get(i).getOperand1()!=null) {
						instructionList.get(i).getOperand1().incrementNumReferences();
						numDistinctOperand += instructionList.get(i).getOperand1().getNumDistinctRecursiveReferences();
					}

					if(instructionList.get(i).getOperand2()!=null) {
						instructionList.get(i).getOperand2().incrementNumReferences();
						numDistinctOperand += instructionList.get(i).getOperand2().getNumDistinctRecursiveReferences();
					}

					if(instructionList.get(i).getDestinationOperand()!=null) {
						instructionList.get(i).getDestinationOperand().incrementNumReferences();
						numDistinctOperand += instructionList.get(i).getDestinationOperand().getNumDistinctRecursiveReferences();
					}
				}
			} else {
				throw new InvalidInstructionException("", false);
			}			
		} catch(Exception inInstrEx) {
			/*
			 * microOps created for this instruction are not valid 
			 * since the translation of the instruction did not 
			 * complete its execution.
			 */
			
			System.err.print("Unable to riscify instruction : ");
			System.err.println("ip="+instructionPointer+"\toperation="+operation+"\top1="
					+operand1Str+"\top2="+operand2Str+"\top3="+operand3Str);

			if(operand1!=null) {
				operand1.incrementNumReferences();
				CustomObjectPool.getOperandPool().returnObject(operand1);
			}
			
			if(operand2!=null) {
				operand2.incrementNumReferences();
				CustomObjectPool.getOperandPool().returnObject(operand2);
			}
			
			if(operand3!=null) {
				operand3.incrementNumReferences();
				CustomObjectPool.getOperandPool().returnObject(operand3);
			}
			
			while(instructionList.getListSize() != microOpsIndexBefore) {
				instructionList.removeLastInstr(operand1, operand2, operand3);
			}
		}
		
		int numOperandsRemovedFromPool = (previousPoolSize-CustomObjectPool.getOperandPool().getSize());
		int currentPoolCapacity = CustomObjectPool.getOperandPool().getPoolCapacity();
		
		if((currentPoolCapacity==previousPoolCapacity) && (numOperandsRemovedFromPool!=numDistinctOperand)) {
			System.err.println("ip=" + instructionPointer + "\tprefix=" + instructionPrefix + 
					"\top=" + operation + "\top1=" + operand1Str + "\top2=" + operand2Str + "\top3=" + operand3Str);

			System.err.println("ip=" + instructionPointer + 
				"\t#operands removed from pool = " + numOperandsRemovedFromPool + 
				"\tnumDistinctOperands = " + numDistinctOperand);
			
			//misc.Error.showErrorAndExit("numOperandsRemovedFromPool!=numDistinctOperand");
		}
		
		return (instructionList.length()-microOpsIndexBefore);
	}
	
	//return true if the string is a valid instruction prefix
	private static boolean isInstructionPrefix(String string)
	{
		if(string.matches("rep|repe|repne|repz|repnz|lock|o16"))
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
	
	// Counts number of lines in a file.
	public static int noOfLines(String executableFileName) {
		int numLines = 0;
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
			
			while((input.readLine())!=null) {
				numLines++;
			}
		} catch (IOException ioe) {
			Error
					.showErrorAndExit("\n\tError in running objdump on the executable file !!");
		}

		return numLines;
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
	private static boolean isContainingObjDumpAssemblyCode(String line) 
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
		
		// remove the suffix part of string 
		if(line.indexOf("#")!=-1) {
			line = line.substring(0, line.indexOf("#"));
		}
		
		// remove the part of string enclosed in <...>
		line.replaceAll("<.*>", "");

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
	
	// for  a line of assembly code, this would return the
	// linear address, operation, operand1,operand2, operand3
	private static String[] tokenizeQemuAssemblyCodeSS(String line) 
	{
		String instructionPrefix;
		String operation;
		String operand1, operand2, operand3;
		String operands;

		// Initialise all operands to null
		instructionPrefix = operation = null; 
		operands = operand1 = operand2 = operand3 = null;

		// Tokenize the line
		StringTokenizer lineTokenizer = new StringTokenizer(line);

		// Read the tokens into required variables
		String str = lineTokenizer.nextToken().trim();
		if(isInstructionPrefix(str)==true) {
			instructionPrefix = str;
			operation = lineTokenizer.nextToken().trim();
		} else {
			instructionPrefix = null;
			operation = str;
		}
		
		if (lineTokenizer.hasMoreTokens()) {
			operands = lineTokenizer.nextToken();
		
			// First join all the operand tokens
			while (lineTokenizer.hasMoreTokens()) {
				operands = operands + " " + lineTokenizer.nextToken();
			}

			StringTokenizer operandTokenizer = new StringTokenizer(operands,
					",", false);

			if (operandTokenizer.hasMoreTokens()) {
				operand1 = operandTokenizer.nextToken().trim();
			}
			
			if (operandTokenizer.hasMoreTokens()) {
				operand2 = operandTokenizer.nextToken().trim();
			}
			
			if (operandTokenizer.hasMoreTokens()) {
				operand3 = operandTokenizer.nextToken().trim();
			}
		}

		return new String[] { instructionPrefix, operation, operand1, operand2, operand3 };
	}
	
	// return index of null character for a byte array
	private static int len(byte[] asmBytes) {
		int i=0;
		for(;asmBytes[i]!=0 && i<asmBytes.length;i++);
		return i;
	}
	
	// searches character ch in asmByes. If not-found return -1, else return index of ch
	private static int indexOf(byte[] asmBytes, char ch, int offset, int len) {
		for(int i=offset; i<len(asmBytes); i++) {
			if(asmBytes[i]==ch) {
				return i;
			}
		}
		
		return -1;
	}
	
	private static String concatenateStringArray(String[] strArray) {
		String concatenatedString = new String(strArray[0] + strArray[1] + strArray[2] + strArray[3]);
		return concatenatedString;
	}
	// for  a line of assembly code, this would return the
	// linear address, operation, operand1,operand2, operand3
	private static String[] tokenizeQemuAssemblyCode(byte[] asmBytes) {
		
		String assemblyTokens[] = new String[5];
		
		// System.out.println("assembly = " + new String(asmBytes));
		
		int previousPointer, currentPointer;
		previousPointer = currentPointer = 0;

		// -------------- instructionPrefix and operation ---------------------------------- 
		currentPointer = indexOf(asmBytes, ' ', previousPointer, 64);
		
		if(currentPointer==-1) {
			assemblyTokens[0] = null;
			assemblyTokens[1] = new String(asmBytes, 0, len(asmBytes)); // only operation field is present
			assemblyTokens[2] = assemblyTokens[3] = assemblyTokens[4] = null;
			return assemblyTokens;
		}
		
		String str = new String(asmBytes, previousPointer, (currentPointer-previousPointer));
		currentPointer++; previousPointer = currentPointer;
		
		if(isInstructionPrefix(str)) {
			assemblyTokens[0] = str;
			currentPointer = indexOf(asmBytes, ' ', previousPointer, 64);
			
			assemblyTokens[1] = new String(asmBytes, previousPointer, (currentPointer-previousPointer));
			currentPointer++; previousPointer = currentPointer;
		} else {
			assemblyTokens[0] = null;
			assemblyTokens[1] = str;
		}
		
		// --------------------- operand1, operand2, operand3 --------------------------------
		if(previousPointer==len(asmBytes)) {
			assemblyTokens[2] = assemblyTokens[3] = assemblyTokens[4] = null;
			return assemblyTokens;
		}
		
		currentPointer = indexOf(asmBytes, ',', previousPointer, 64);
		if(currentPointer==-1) {
			assemblyTokens[3] = assemblyTokens[4] = null;
			assemblyTokens[2] = new String(asmBytes, previousPointer, len(asmBytes)-previousPointer);
			return assemblyTokens;
		} else {
			assemblyTokens[2] = new String(asmBytes, previousPointer, (currentPointer-previousPointer));
			currentPointer+=2; previousPointer=currentPointer;
			
			if(previousPointer==len(asmBytes)) {
				assemblyTokens[3] = assemblyTokens[4] = null;
			} else {
				currentPointer = indexOf(asmBytes, ',', previousPointer, 64);
				
				if(currentPointer==-1) {
					assemblyTokens[4] = null;
					assemblyTokens[3] = new String(asmBytes, previousPointer, len(asmBytes)-previousPointer);
					return assemblyTokens;
				} else {
					assemblyTokens[3] = new String(asmBytes, previousPointer, (currentPointer-previousPointer));
					currentPointer++; previousPointer=currentPointer;
					
					assemblyTokens[4] = new String(asmBytes, previousPointer, len(asmBytes)-previousPointer);
					return assemblyTokens;
				}
			}
		}
		
		return assemblyTokens;
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
	
	private static boolean removeInstructionFromTail(GenericCircularQueue<Instruction> inputToPipeline, long instructionPointer) {
		
		Instruction removedInstruction;
		boolean foundThisCISC = false;
		
		while( (inputToPipeline.isEmpty()== false) &&
			(inputToPipeline.peek(inputToPipeline.size()-1).getCISCProgramCounter()==instructionPointer))
		{
			foundThisCISC = true;
			removedInstruction = inputToPipeline.pop();
			try {
				CustomObjectPool.getInstructionPool().returnObject(removedInstruction);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return foundThisCISC;
	}

	/*
	 * This function fuses the statically translated micro-ops with the information received from the emulator.
	 * New micro-ops are added to the circular buffer(argument). Finally it returns the number of CISC instructions it could 
	 * translate.
	 */
	public static int fuseInstruction(
			int tidApp, long startInstructionPointer,
			EmulatorPacketList arrayListPacket, GenericCircularQueue<Instruction> inputToPipeline)
	{
		//System.out.println("ip = " + startInstructionPointer + "\t" + Long.toHexString(startInstructionPointer));
		
		// Create a dynamic instruction buffer for all control packets
		DynamicInstructionBuffer dynamicInstructionBuffer = staticDynamicInstructionBuffers[tidApp];
		dynamicInstructionBuffer.configurePackets(arrayListPacket);
		
		InstructionList assemblyPacketList = null;
		
		int numCISC = 1;
		int microOpIndex = -1;
		
		// Riscify the assembly packets
		if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_QEMU) {
			assemblyPacketList = threadMicroOpsList[tidApp];
			threadMicroOpsList[tidApp].clear();
			
			//This is a bug(at least in case of caching): assemblyPacketList = threadMicroOpsList[tidApp]; 
			Packet p = arrayListPacket.get(0);
				
			if(p.value==Encoding.ASSEMBLY) {
				byte asmBytes[] = CustomObjectPool.getCustomAsmCharPool().dequeue(tidApp);
				String assemblyTokens[] = tokenizeQemuAssemblyCode(asmBytes);
				String asmText = concatenateStringArray(assemblyTokens);
		
				//check if present in translated-instruction cache
				if(TranslatedInstructionCache.isPresent(asmText)) {
					assemblyPacketList = TranslatedInstructionCache.getInstructionList(asmText);
					
					for(int j=0; j<assemblyPacketList.length(); j++) {
						assemblyPacketList.setCISCProgramCounter(j, p.ip);
						assemblyPacketList.setRISCProgramCounter(j, j);
					}
					
				} else {
					// System.out.println(i + " : " + assemblyLine);
					long instructionPointer = p.ip;
					String instructionPrefix, operation, operand1, operand2, operand3;
					instructionPrefix = assemblyTokens[0]; operation = assemblyTokens[1];
					operand1 = assemblyTokens[2]; operand2 = assemblyTokens[3]; operand3 = assemblyTokens[4];
					
					riscifyInstruction( instructionPointer, 
						instructionPrefix, operation, 
						operand1, operand2, operand3, 
						assemblyPacketList);
					
					//Add to translated-instruction cache
					TranslatedInstructionCache.add(asmText, assemblyPacketList);
				}
			} else {
				misc.Error.showErrorAndExit("First packet to fuse instruction must be assembly packet !!");
			}
			
			microOpIndex = 0;
			
		} else if (EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_PIN) {
			assemblyPacketList = staticMicroOpList;
			
			microOpIndex = ciscIPtoRiscIP.getMicroOpIndex(startInstructionPointer);
			
			if((microOpIndex==-1) || 
			  (assemblyPacketList.get(microOpIndex).getCISCProgramCounter()!=startInstructionPointer)) 
			{
				// dynamicInstructionBuffer.clearBuffer();
				//System.out.println("static -- " + microOpIndex);
				return 0;
			}
		}
		
		Instruction staticMicroOp, dynamicMicroOp;
		DynamicInstructionHandler dynamicInstructionHandler;
				
		// main translate loop.
		while(true)
		{
			staticMicroOp = assemblyPacketList.get(microOpIndex); 
			if(staticMicroOp==null || staticMicroOp.getCISCProgramCounter() != startInstructionPointer) {
				break;
			}
			
			dynamicInstructionHandler = VisaHandlerSelector.selectHandler(staticMicroOp.getOperationType());
			dynamicMicroOp = getDynamicMicroOp(staticMicroOp);
			microOpIndex = dynamicInstructionHandler.handle(microOpIndex, dynamicMicroOp, dynamicInstructionBuffer);
			
			if(microOpIndex==-1) {
				//System.out.println("dynamic");
				// I was unable to fuse certain micro-ops of this instruction. 
				// So, I must remove any previously 
				// computed micro-ops from the buffer
				CustomObjectPool.getInstructionPool().returnObject(dynamicMicroOp);
				removeInstructionFromTail(inputToPipeline, staticMicroOp.getCISCProgramCounter());
				numCISC = 0;
				break;
			} else {
				inputToPipeline.enqueue(dynamicMicroOp); //append microOp
			}
		}
		
		/* clear the dynamicInstructionBuffer */		
		// dynamicInstructionBuffer.clearBuffer();
		//System.out.println(inputToPipeline);
		return numCISC;
	}

	private static Instruction getDynamicMicroOp(Instruction staticMicroOp) {
		
		Instruction dynamicMicroOp = null;
		
		if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_PIN) {
			dynamicMicroOp = CustomObjectPool.getInstructionPool().borrowObject();
			dynamicMicroOp.copy(staticMicroOp);
		} else if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_QEMU) {
			// This will ensure that the packet is returned to instruction pool
			dynamicMicroOp = staticMicroOp;
		}
		
		return dynamicMicroOp;
	}
}