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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;


public class MicroOpsList 
{
	private ArrayList<Instruction> instructionList;
	private ListIterator<Instruction> listIterator;
	SynchronizationObject syncObject;
	//SynchronizationObject syncObject2;
	
	public MicroOpsList()
	{
		instructionList = new ArrayList<Instruction>();
		listIterator = instructionList.listIterator();
		syncObject = new SynchronizationObject();
		//syncObject2 = new SynchronizationObject();
	}

	//appends a single instruction to the instruction list
	synchronized public void appendInstruction(Instruction newInstruction)
	{
		instructionList.add(newInstruction);
	}
	
	//appends a list of instructions to the instruction list
	synchronized public void appendInstruction(MicroOpsList microOpsList)
	{
		this.instructionList.addAll(microOpsList.instructionList);	
	}

	synchronized public boolean isEmpty()
	{
		return instructionList.isEmpty();
	}
	
	public void printList() 
	{
		for(int i = 0; i< instructionList.size(); i++)
		{
			System.out.print(instructionList.get(i).toString() + "\n");
		}
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
	
	synchronized public Instruction pollFirst()
	{
		// FIXME : Need to decide an laternative for this
		return instructionList.pollFirst();
	}

	public void setProgramCounter(int index, long instructionPointer) 
	{
		instructionList.get(index).setProgramCounter(instructionPointer);
	}
	
	synchronized public int getListSize()
	{
		return instructionList.size();
	}
	
	synchronized public Instruction peekInstructionAt(int position)
	{
		return instructionList.get(position);
	}
	
	public SynchronizationObject getSyncObject() {
		return syncObject;
	}
	
	public int length()
	{
		return instructionList.size();
	}
	
	/*public SynchronizationObject getSyncObject2() {
		return syncObject2;
	}*/
}