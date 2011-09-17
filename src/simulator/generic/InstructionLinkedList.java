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


public class InstructionLinkedList 
{
	private LinkedList<Instruction> instructionLinkedList;
	private ListIterator<Instruction> listIterator;
	//SynchronizationObject syncObject;
	//SynchronizationObject syncObject2;
	
	public InstructionLinkedList()
	{
		instructionLinkedList = new LinkedList<Instruction>();
		listIterator = instructionLinkedList.listIterator();
		//syncObject = new SynchronizationObject();
		//syncObject2 = new SynchronizationObject();
	}

	//appends a single instruction to the instruction list
	public void appendInstruction(Instruction newInstruction)
	{
		instructionLinkedList.add(newInstruction);
	}
	
	public boolean isEmpty()
	{
		return instructionLinkedList.isEmpty();
	}
	
	// This method removes all the micro-ops at the end of the list which have 
	// ip=instructionPointer
	public void removeInstructionFromTail(long instructionPointer)
	{
		while( (instructionLinkedList.isEmpty()==false) &&
			(instructionLinkedList.getLast().getProgramCounter()==instructionPointer))
		{
			instructionLinkedList.removeLast();
		}
	}
	
	public void printList() 
	{
		for(int i = 0; i< instructionLinkedList.size(); i++)
		{
			System.out.print(instructionLinkedList.get(i).toString() + "\n");
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

	public Instruction peekInstructionAt(int position)
	{
		return instructionLinkedList.get(position);
	}

	public Instruction pollFirst()
	{
		// FIXME : Need to decide an laternative for this
		return instructionLinkedList.pollFirst();
	}

	public int getListSize()
	{
		return instructionLinkedList.size();
	}
	
//	public SynchronizationObject getSyncObject() {
//		return syncObject;
//	}
	
	public int length()
	{
		return instructionLinkedList.size();
	}
	
	/*public SynchronizationObject getSyncObject2() {
		return syncObject2;
	}*/
}