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

package generic;

import java.util.ArrayList;

public class InstructionArrayList 
{
	private ArrayList<Instruction> instructionArrayList;
	//SynchronizationObject syncObject;
	//SynchronizationObject syncObject2;
	
	public InstructionArrayList()
	{
		instructionArrayList = new ArrayList<Instruction>();
		//syncObject = new SynchronizationObject();
		//syncObject2 = new SynchronizationObject();
	}

	//appends a single instruction to the instruction list
	public void appendInstruction(Instruction newInstruction)
	{
		instructionArrayList.add(newInstruction);
	}
	
	public boolean isEmpty()
	{
		return instructionArrayList.isEmpty();
	}
	
	public Instruction get(int index)
	{
		// For the last instruction of the file, we will have to return null,
		// otherwise, we will encounter an Exception.
		if(index >= instructionArrayList.size())
		{
			return null;
		}
		else
		{
			return instructionArrayList.get(index);
		}
	}
	
	public void printList() 
	{
		for(int i = 0; i< instructionArrayList.size(); i++)
		{
			System.out.print(instructionArrayList.get(i).toString() + "\n");
		}
	}

//	public Instruction getNextInstruction()
//	{
//		if(listIterator.hasNext())
//		{
//			return listIterator.next(); 
//		}
//		else 
//		{
//			//If the list iterator is well past the last element we return a null
//			return null;
//		}
//	}
	
//	public Instruction pollFirst()
//	{
//		// FIXME : Need to decide an laternative for this
//		return instructionLinkedList.pollFirst();
//	}

	public void setCISCProgramCounter(int index, long instructionPointer) 
	{
		instructionArrayList.get(index).setCISCProgramCounter(instructionPointer);
	}
	
	public void setRISCProgramCounter(int index, long instructionPointer) 
	{
		instructionArrayList.get(index).setRISCProgramCounter(instructionPointer);
	}
	
	public int getListSize()
	{
		return instructionArrayList.size();
	}
	
	public Instruction peekInstructionAt(int position)
	{
		return instructionArrayList.get(position);
	}
	
	public void removeLastInstr()
	{
		this.instructionArrayList.remove(instructionArrayList.size()-1);
	}
//	public SynchronizationObject getSyncObject() {
//		return syncObject;
//	}
	
	public int length()
	{
		return instructionArrayList.size();
	}
	
	/*public SynchronizationObject getSyncObject2() {
		return syncObject2;
	}*/
}