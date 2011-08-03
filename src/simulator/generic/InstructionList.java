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

package generic;

import java.util.LinkedList;
import java.util.ListIterator;


public class InstructionList 
{
	private LinkedList<Instruction> instructionList;
	private ListIterator<Instruction> listIterator;
	
	public InstructionList()
	{
		instructionList = new LinkedList<Instruction>();
		listIterator = instructionList.listIterator();
	}
	
	//appends a single instruction to the instruction list
	public void appendInstruction(Instruction newInstruction)
	{
		instructionList.add(newInstruction);
	}
	
	//appends a list of instructions to the instruction list
	public void appendInstruction(InstructionList instructionList)
	{
		this.instructionList.addAll(instructionList.instructionList);	
	}

	public boolean isEmpty()
	{
		return instructionList.isEmpty();
	}
	
	public String toString() 
	{
		String toString = new String("");
		
		for(Instruction i : instructionList)
			toString = toString + "\n" + i;
		
		return toString;
	}

	public Instruction getNextInstruction()
	{
		if(listIterator.hasNext())
		{
			return listIterator.next(); 
		}
		else 
		{
			//If the list iterator is well past the last element we return a null
			return null;
		}
	}
	
	public Instruction pollFirst()
	{
		return instructionList.pollFirst();
	}

	public void setProgramCounter(Long instructionPointer) 
	{
		for(Instruction i : instructionList)
		{
			i.setProgramCounter(instructionPointer);
		}
	}
}