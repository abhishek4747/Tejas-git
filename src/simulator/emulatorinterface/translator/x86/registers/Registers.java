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

package emulatorinterface.translator.x86.registers;


import java.util.Hashtable;

import generic.Operand;


public class Registers 
{
	private static Hashtable<String, Long> machineSpecificRegistersHashTable = null;
	private static Hashtable<String, Long> integerRegistersHashTable = null;
	private static Hashtable<String, Long> floatRegistersHashTable = null;
	
	private int noOfIntTempRegs;
	private int noOfFloatTempRegs;

	public Registers()
	{
		this.noOfIntTempRegs = 0;
		this.noOfFloatTempRegs = 0;
	}
	
	public Registers(Operand operand1, Operand operand2, Operand operand3)
	{

		// initialise the number of temporary integer register
		int maxTempInt=0;
		
		if(Registers.isTempIntRegister(operand1) && operand1.getValue()>maxTempInt)
			maxTempInt=(int) (operand1.getValue());
		
		if(Registers.isTempIntRegister(operand1) && operand1.getValue()>maxTempInt)
			maxTempInt=(int) (operand1.getValue());
		
		if(Registers.isTempIntRegister(operand1) && operand1.getValue()>maxTempInt)
			maxTempInt=(int) (operand1.getValue());
		
		
		// initialise the number of temporary float register
		int maxTempFloat=0;
		
		if(Registers.isTempFloatRegister(operand1) && operand1.getValue()>maxTempFloat)
			maxTempFloat=(int) (operand1.getValue());
		
		if(Registers.isTempFloatRegister(operand1) && operand1.getValue()>maxTempFloat)
			maxTempFloat=(int) (operand1.getValue());
		
		if(Registers.isTempFloatRegister(operand1) && operand1.getValue()>maxTempFloat)
			maxTempFloat=(int) (operand1.getValue());
		
		
		this.noOfIntTempRegs = maxTempInt+1;
		this.noOfFloatTempRegs = maxTempFloat+1;
	}
	
	public Operand getTempIntReg()
	{
		//Allocate a new temporary register
		return Operand.getIntegerRegister(encodeRegister("temp" + noOfIntTempRegs++));
	}
	
	public Operand getTempFloatReg()
	{
		//Allocate a new temporary float register
		return Operand.getFloatRegister(encodeRegister("tempFloat" + noOfFloatTempRegs++));
	}
	
	public static void createRegisterHashTable()
	{
		//Create required hash-tables
		machineSpecificRegistersHashTable = new Hashtable<String, Long>();
		integerRegistersHashTable = new Hashtable<String, Long>();
		floatRegistersHashTable = new Hashtable<String, Long>();
		
		
		//--------------------------Machine specific registers---------------------------------
		//Segment Registers
		machineSpecificRegistersHashTable.put("es", new Long(0));
		machineSpecificRegistersHashTable.put("cs", new Long(1));
		machineSpecificRegistersHashTable.put("ss", new Long(2));
		machineSpecificRegistersHashTable.put("ds", new Long(3));
		machineSpecificRegistersHashTable.put("fs", new Long(4));
		machineSpecificRegistersHashTable.put("gs", new Long(5));
		
		machineSpecificRegistersHashTable.put("eflags", new Long(5));
		machineSpecificRegistersHashTable.put("rip", new Long(6));
		
		//FIXME: Not sure if this goes here
		machineSpecificRegistersHashTable.put("FP_CWORD", new Long(7));
		
		//-------------------------Integer register-----------------------------------------------
		//Registers available to the programmer
		integerRegistersHashTable.put("rax", new Long(0));
		integerRegistersHashTable.put("rbx", new Long(1));
		integerRegistersHashTable.put("rcx", new Long(2));
		integerRegistersHashTable.put("rdx", new Long(3));
		integerRegistersHashTable.put("r8", new Long(4));
		integerRegistersHashTable.put("r9", new Long(5));
		integerRegistersHashTable.put("r10", new Long(6));
		integerRegistersHashTable.put("r11", new Long(7));
		integerRegistersHashTable.put("r12", new Long(8));
		integerRegistersHashTable.put("r13", new Long(9));
		integerRegistersHashTable.put("r14", new Long(10));
		integerRegistersHashTable.put("r15", new Long(11));
		
		//Index registers
		machineSpecificRegistersHashTable.put("rsi", new Long(12));
		machineSpecificRegistersHashTable.put("rdi", new Long(13));
		machineSpecificRegistersHashTable.put("rbp", new Long(14));
		machineSpecificRegistersHashTable.put("rsp", new Long(15));
		
		//Weird Register
		machineSpecificRegistersHashTable.put("riz", new Long(17));
		
		//Temporary registers
		integerRegistersHashTable.put("temp0", new Long(18));
		integerRegistersHashTable.put("temp1", new Long(19));
		integerRegistersHashTable.put("temp2", new Long(20));
		integerRegistersHashTable.put("temp3", new Long(21));
		integerRegistersHashTable.put("temp4", new Long(22));
		integerRegistersHashTable.put("temp5", new Long(23));
		integerRegistersHashTable.put("temp6", new Long(24));
		integerRegistersHashTable.put("temp7", new Long(25));
		
		
		//-------------------------Floating-point register-----------------------------------------
		//Stack registers
		floatRegistersHashTable.put("st", new Long(0));
		floatRegistersHashTable.put("st(0)", new Long(0));
		floatRegistersHashTable.put("st(1)", new Long(1));
		floatRegistersHashTable.put("st(2)", new Long(2));
		floatRegistersHashTable.put("st(3)", new Long(3));
		floatRegistersHashTable.put("st(4)", new Long(4));
		floatRegistersHashTable.put("st(5)", new Long(5));
		floatRegistersHashTable.put("st(6)", new Long(6));
		floatRegistersHashTable.put("st(7)", new Long(7));
		
		//TODO This register-set can be used to perform integer operations too.
		//So its exact type - integer or floating point is ambiguous
		floatRegistersHashTable.put("xmm0", new Long(9));
		floatRegistersHashTable.put("xmm1", new Long(10));
		floatRegistersHashTable.put("xmm2", new Long(11));
		floatRegistersHashTable.put("xmm3", new Long(12));
		floatRegistersHashTable.put("xmm4", new Long(13));
		floatRegistersHashTable.put("xmm5", new Long(14));
		floatRegistersHashTable.put("xmm6", new Long(15));
		floatRegistersHashTable.put("xmm7", new Long(16));
		
		//temporary floating-point registers
		floatRegistersHashTable.put("tempFloat0", new Long(17));
		floatRegistersHashTable.put("tempFloat1", new Long(18));
		floatRegistersHashTable.put("tempFloat2", new Long(19));
		floatRegistersHashTable.put("tempFloat3", new Long(20));
	}

	
	//assign an index to each coarse-register
	public static long encodeRegister(String regStr)
	{
		checkAndCreateRegisterHashTable();
		
		Long codeRegister = null;
		
		if((codeRegister = machineSpecificRegistersHashTable.get(regStr)) != null)
		{
			return codeRegister.longValue();
		}
		else if((codeRegister = integerRegistersHashTable.get(regStr)) != null)
		{
			return codeRegister.longValue();
		}
		else if((codeRegister = floatRegistersHashTable.get(regStr)) != null)
		{
			return codeRegister.longValue();
		}
		else
		{
			misc.Error.showErrorAndExit("\n\tNot a valid register : " + regStr + " !!");
			return -1;
		}
	}
	

	public static boolean isMachineSpecificRegister(String regStr)
	{
		checkAndCreateRegisterHashTable();
		
		return (machineSpecificRegistersHashTable.get(regStr)!=null);
	}
	
	public static boolean isFloatRegister(String regStr)
	{
		checkAndCreateRegisterHashTable();
		
		return (floatRegistersHashTable.get(regStr)!=null);
	}
	
	public static boolean isIntegerRegister(String regStr)
	{
		checkAndCreateRegisterHashTable();
		
		return (integerRegistersHashTable.get(regStr)!=null);
	}
			
	//public static void releaseTempRegister(Operand tempRegister)
	//{
	//	//Must be called only from the simplify location method only
	//	noOfIntTempRegs--;
	//}

	/**
	 * This method converts the smaller parts of register to the complete register
	 * @param operandStr Operand string 
	 */
 	public static String coarsifyRegisters(String operandStr)
	{
		operandStr = operandStr.replaceAll("rax|eax|ax|ah|al", "rax");
		operandStr = operandStr.replaceAll("rbx|ebx|bx|bh|bl", "rbx");
		operandStr = operandStr.replaceAll("rcx|ecx|cx|ch|cl", "rcx");
		operandStr = operandStr.replaceAll("rdx|edx|dx|dh|dl", "rdx");
		
		operandStr = operandStr.replaceAll("rsi|esi|si|sil", "rsi");
		operandStr = operandStr.replaceAll("rdi|edi|di|dil", "rdi");
		operandStr = operandStr.replaceAll("rbp|ebp|bp|bpl", "rbp");
		operandStr = operandStr.replaceAll("rsp|esp|sp|spl", "rsp");
		
		operandStr = operandStr.replaceAll("r8|r8d|r8w|r8l", "r8");
		operandStr = operandStr.replaceAll("r9|r9d|r9w|r9l", "r9");
		operandStr = operandStr.replaceAll("r10|r10d|r10w|r10l", "r10");
		operandStr = operandStr.replaceAll("r11|r11d|r11w|r11l", "r11");
		operandStr = operandStr.replaceAll("r12|r12d|r12w|r10l", "r12");
		operandStr = operandStr.replaceAll("r13|r13d|r13w|r10l", "r13");
		operandStr = operandStr.replaceAll("r14|r14d|r14w|r10l", "r14");
		operandStr = operandStr.replaceAll("r15|r15d|r15w|r10l", "r15");

		operandStr = operandStr.replaceAll("eiz", "eiz");
		return operandStr;
	}

 	
 	public static Operand getStackPointer()
 	{
 		return Operand.getMachineSpecificRegister(encodeRegister("rsp"));
 	}
 	
 	public static Operand getAccumulatorRegister()
 	{
 		return Operand.getIntegerRegister(encodeRegister("rax"));
 	}

 	public static Operand getTopFPRegister()
 	{
 		return Operand.getFloatRegister(encodeRegister("st(0)"));
 	}
 	
 	public static Operand getSecondTopFPRegister()
 	{
 		return Operand.getFloatRegister(encodeRegister("st(1)"));
 	}
 	
 	public static Operand getInstructionPointer()
 	{
 		return Operand.getMachineSpecificRegister(encodeRegister("rip"));
 	}
 	

 	public static Operand getTemporaryMemoryRegister(Operand operand)
	{
 		Operand firstOperand;
 		Operand secondOperand;
 		
 		firstOperand = operand.getMemoryLocationFirstOperand();
 		secondOperand = operand.getMemoryLocationSecondOperand();
 		
		if(	operand.isMemoryOperand() && isTempIntRegister(firstOperand))
		{
			return firstOperand;
		}
		
		else if(operand.isMemoryOperand() && isTempIntRegister(secondOperand))
		{
			return secondOperand;
		}
		
		else
		{
			return null;
		}
	}

 	private static boolean isTempIntRegister(Operand operand)
 	{
 		return( 
 		operand!=null && operand.isIntegerRegisterOperand()
 		&& operand.getValue() >=encodeRegister("temp0") && operand.getValue() <=encodeRegister("temp7"));
 	}
 	
 	private static boolean isTempFloatRegister(Operand operand)
 	{
 		return( 
 		operand!=null && operand.isFloatRegisterOperand()
 		//&& operand.getValue() >=encodeRegister("tempFloat0") &&	operand.getValue() <=encodeRegister("tempFloat7")
 		);
 	}
 	
 	private static void checkAndCreateRegisterHashTable()
 	{
 		if(integerRegistersHashTable==null || machineSpecificRegistersHashTable==null || floatRegistersHashTable==null)
 			createRegisterHashTable();
 	}
 	
 	public static Operand getCounterRegister()
 	{
 		return Operand.getIntegerRegister(encodeRegister("rcx"));
 	}

 	public static Operand getSourceIndexRegister()
 	{
 		return Operand.getMachineSpecificRegister(encodeRegister("rsi"));
 	}

 	public static Operand getDestinationIndexRegister()
 	{
 		return Operand.getMachineSpecificRegister(encodeRegister("rdi"));
 	}

	public static Operand getBasePointer() 
	{
		return Operand.getMachineSpecificRegister(encodeRegister("rbp"));
	}

	public static Operand getFloatingPointControlWord() 
	{
		return Operand.getMachineSpecificRegister(encodeRegister("FP_CWORD"));
	}
 }