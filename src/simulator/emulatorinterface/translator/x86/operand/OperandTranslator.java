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

package emulatorinterface.translator.x86.operand;



import emulatorinterface.translator.x86.registers.Registers;
import generic.Instruction;
import generic.InstructionList;
import generic.Operand;
import generic.OperandType;
import java.util.StringTokenizer;



import misc.Numbers;


public class OperandTranslator 
{
	public static Operand simplifyOperand(String operandStr, InstructionList microOps, Registers registers)
	{
		//If there is no operand, then just don't process it. 
		if(operandStr == null)
			return null;
		
		
		//Replace all the occurrences of registers with the 64-bit register versions
		if(operandStr!=null)
			operandStr = Registers.coarsifyRegisters(operandStr);
		
		
		//If operand is a valid number, then it is an immediate
		if(Numbers.isValidNumber(operandStr))
		{
			return new Operand(OperandType.immediate, Numbers.hexToLong(operandStr));
		}
		else if(Registers.isIntegerRegister(operandStr))
		{
			return Operand.getIntegerRegister(Registers.encodeRegister(operandStr));
		}
		else if(Registers.isFloatRegister(operandStr))
		{
			return Operand.getFloatRegister(Registers.encodeRegister(operandStr));
		}
		else if(Registers.isMachineSpecificRegister(operandStr))
		{
			return Operand.getMachineSpecificRegister(Registers.encodeRegister(operandStr));
		}
		//Simplify memory locations specified by [...]
		else if(operandStr.matches(".*\\[.*\\]"))
		{
			//contains a memory location specified by the memory address
			//Strip the string enclosed in square brackets
			String memLocation = operandStr = operandStr.substring(operandStr.indexOf("[") + 1, operandStr.indexOf("]"));
			
			//Mark the operand as an operand whose value is stored in the memory
			return simplifyMemoryLocation(memLocation, microOps, registers);
		}
		
		else if(operandStr.matches("[0-9a-f]+ <.*>"))
		{
			//Above pattern is numbers <random>
			//This operand contains a memory address and a reference address enclosed in <>
			//We just need the first field containing address. This is an immediate
			String memLocation = new StringTokenizer(operandStr).nextToken();
			return new Operand(OperandType.immediate, Numbers.hexToLong(memLocation));
		}
		
		else if(operandStr.matches("[a-zA-Z ]+:0x[0-9a-f]+"))
		{	
			//This operand contains :. So it must be like DWORD PTR segment-register:memory Address
			StringTokenizer memLocTokenizer = new StringTokenizer(operandStr, ":", false);
			memLocTokenizer.nextToken(); //Skip the segmentDescriptor
			String memLocation = memLocTokenizer.nextToken();

			// FIXME something seems to be wrong
			// If the operand contains the keyword PTR, mark it as stored in memory
			if(operandStr.contains("PTR"))
				return simplifyMemoryLocation(memLocation, microOps, registers);
			else
			{
				//TODO must check if the immediate address is available from PIN tool
				return Operand.getMemoryOperand(Operand.getImmediateOperand(), null, -1);
			}
		}
		
		else
		{
			misc.Error.invalidOperand(operandStr);
			return null;
		}
	}
	

	static Operand simplifyMemoryLocation(String operandStr, InstructionList microOps, Registers registers)
	{
		String memoryAddressTokens[] = operandStr.split("\\+|-");
		
		if(!areValidMemoryAddressTokens(memoryAddressTokens))
		{
			misc.Error.showErrorAndExit("\n\tIllegal arguments to a memory address : " 
					+ operandStr + " !!");
		}
		
		
		Operand base, offset, index,scale;
		String indexStr, scaleStr = null;
		base=offset=index=scale=null;
				
		
		//Determine all the parameters of the string 
		for(int i=0; i<memoryAddressTokens.length; i++)
		{
			//base register
			if(Registers.isIntegerRegister(memoryAddressTokens[i]))
			{
				base = Operand.getIntegerRegister(Registers.encodeRegister(memoryAddressTokens[i]));
			}
			else if(Registers.isMachineSpecificRegister(memoryAddressTokens[i]))
			{
				base = Operand.getMachineSpecificRegister(Registers.encodeRegister(memoryAddressTokens[i]));	
			}
			
			
			//offset
			else if(Numbers.isValidNumber(memoryAddressTokens[i]))
			{
				//if offset is zero, then this won't be considered as an offset in
				//actual address
				if(Numbers.hexToLong(memoryAddressTokens[i])==0)
					continue;
				
				offset = Operand.getImmediateOperand();
			}
			
			
			
			//index*scale
			else if(memoryAddressTokens[i].matches("[a-zA-Z0-9]+\\*[0x123456789abcdef]+"))
			{
				indexStr = memoryAddressTokens[i].split("\\*")[0];
				scaleStr = memoryAddressTokens[i].split("\\*")[1];
				
				//if index is eiz then it means that this is a dummy scaled operand
				if(indexStr.contentEquals("eiz"))
					continue;
				
				if(Registers.isIntegerRegister(memoryAddressTokens[i].split("\\*")[0]))
				{
					index = Operand.getIntegerRegister(Registers.encodeRegister(indexStr));
				}
				else if(Registers.isMachineSpecificRegister(memoryAddressTokens[i].split("\\*")[0]))
				{
					index = Operand.getMachineSpecificRegister(Registers.encodeRegister(indexStr));
				}
				scale = Operand.getImmediateOperand();
			}
			
			else
			{
				misc.Error.invalidOperand(operandStr);
			}
		}

		//Create scaled index
		Operand scaledIndex = null;
		if(scale!=null)
		{
			if(Numbers.hexToLong(scaleStr)==1)
			{
				scaledIndex = index;
			}
			else
			{
				scaledIndex = registers.getTempIntReg();
				microOps.appendInstruction(Instruction.getIntALUInstruction(index, scale, scaledIndex));
			}
		}

		
		//TODO : Once xml file is ready, we have to read this boolean from the configuration parameters
		//Default value is true.
		boolean indexedAddressingMode;
		indexedAddressingMode = true;
		
		//determine the type of addressing used
		Operand memoryLocationFirstOperand = null;
		Operand memoryLocationSecondOperand = null;
		
		if(base==null && index==null && offset==null)
		{}
		
		else if(base==null && index==null && offset!=null)
		{
			memoryLocationFirstOperand = offset;
		}
		
		else if(base==null && index!=null && offset==null)
		{
			memoryLocationFirstOperand = scaledIndex;
		}
		
		else if(base==null && index!=null && offset!=null)
		{
			memoryLocationFirstOperand = scaledIndex;
			memoryLocationSecondOperand = offset;
		}
		
		else if(base!=null && index==null && offset==null)
		{
			memoryLocationFirstOperand = base;
		}
		
		else if(base!=null && index==null && offset!=null)
		{
			memoryLocationFirstOperand = base;
			memoryLocationSecondOperand = offset;
		}
		
		else if(base!=null && index!=null)
		{
			if(indexedAddressingMode == false)
			{
				Operand registerSumOperand;
				
				//In this case, we must always perform tempRegister = base + scaledIndex
				if(scaledIndex != index)
				{
					registerSumOperand = scaledIndex;
				}
				else
				{
					registerSumOperand = registers.getTempIntReg();
				}
				
				microOps.appendInstruction(Instruction.getIntALUInstruction(scaledIndex, base, registerSumOperand));
				memoryLocationFirstOperand = registerSumOperand;
				
				if(offset!=null)
					memoryLocationSecondOperand = offset;
			}
			
			else if(indexedAddressingMode==true && offset==null)
			{
				memoryLocationFirstOperand = base;
				memoryLocationSecondOperand = scaledIndex;
			}
			
			else if(indexedAddressingMode==true && offset!=null) 
			{
				Operand registerSumOperand;
				
				//In this case, we must always perform tempRegister = base + scaledIndex
				if(scaledIndex != index)
				{
					registerSumOperand = scaledIndex;
				}
				else
				{
					registerSumOperand = registers.getTempIntReg();
				}
				
				microOps.appendInstruction(Instruction.getIntALUInstruction(scaledIndex, base, registerSumOperand));
				memoryLocationFirstOperand = registerSumOperand;
				memoryLocationSecondOperand = offset;
			}
		}
		
		return Operand.getMemoryOperand(memoryLocationFirstOperand, memoryLocationSecondOperand, -1);
	}
	


	private static boolean areValidMemoryAddressTokens(String memoryAddressTokens[])
	{
		return ((memoryAddressTokens.length>=1 && memoryAddressTokens.length<=3));
	}
	
	public static Operand getLocationToStoreValue(Operand operand, Registers registers)
	{
		if(!operand.isMemoryOperand())
		{
			misc.Error.showErrorAndExit("\n\tTrying to obtain value from a " +	"non-memory operand !!");
		}
		
		Operand tempMemoryRegister;
		
		tempMemoryRegister = Registers.getTemporaryMemoryRegister(operand);
		
		//If we don't have the luxury of an additional temporary register,
		//then we must allocate a new one
		if(tempMemoryRegister == null)
		{
			//If we don't have any disposable register available, then use a new register
			tempMemoryRegister = registers.getTempIntReg();
		}
		
		return tempMemoryRegister;
	}
}