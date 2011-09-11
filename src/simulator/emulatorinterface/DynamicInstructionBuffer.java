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

package emulatorinterface;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;

import emulatorinterface.communication.Packet;
import generic.BranchInstr;



/*
ip-MemoryReadAddress
ip-MemoryWriteAddress
ip-taken/not-BranchAddress
*/

public class DynamicInstructionBuffer 
{
	private Queue<Packet> memReadQueue;
	private Queue<Packet> memWriteQueue;
	private Queue<Packet> branchQueue;

	
	public DynamicInstructionBuffer() 
	{
		memReadQueue = new LinkedList<Packet>();
		memWriteQueue = new LinkedList<Packet>();
		branchQueue = new LinkedList<Packet>();
	}

	/*
	 * This is a static encoding scheme to convert from Packets used in IPC
	 * between simulator and emulator to convert to the bigger
	 * DynamicInstruction to save bandwidth in IPC. The encoding scheme employed
	 * depends on the "value" field of the packet class. The scheme is mentioned
	 * as follows: a value of 1 - means that the thread has finished 0 - used in
	 * case of Peterson lock management in shared memory and mmap mechanisms 1 -
	 * -do- 2 - means a "memory read" and the "tgt" field of "Packet" contains
	 * the corresponding address 3 - means a "memory write" and "tgt" field has
	 * the address 4 - means "branch taken" with "tgt" as the target address 5 -
	 * means "branch not taken" with "tgt" as the target address 6 - means "tgt"
	 * has a source register value 7 - means "tgt" has a destination register
	 * value
	 */

	public void configurePackets(Vector<Packet> buildDynamicInstruction, int tid, int tidEmu) 
	{
		Packet p;

		long ip = buildDynamicInstruction.elementAt(0).ip;

		for (int i = 0; i < buildDynamicInstruction.size(); i++) 
		{
			p = buildDynamicInstruction.elementAt(i);
			assert (ip == p.ip) : "all instruction pointers not matching";
			switch (p.value) 
			{
				case (-1):
					break;
				
				case (0):
					assert (false) : "The value is reserved for locks. Most probable cause is a bad read";
					break;
					
				case (1):
					assert (false) : "The value is reserved for locks";
					break;
					
				case (2):
					memReadQueue.add(p);
					break;
					
				case (3):
					memWriteQueue.add(p);
					break;
					
				case (4):
					branchQueue.add(p);
					break;
					
				case (5):
					branchQueue.add(p);
					break;
					
				default:
					assert (false) : "error in configuring packets";
			}
		}
	}

	public long getMemoryReadAddress(long instructionPointer)
	{
		Packet p;
		
		do
		{
			p = memReadQueue.poll();
			
			if(p.ip == instructionPointer)
			{
				return p.tgt;
			}
			
		}while(!memReadQueue.isEmpty());
		
		//Could not find the address in the queue
		return -1;
	}
	
	public long getMemoryWriteAddress(long instructionPointer)
	{
		Packet p;
		
		do
		{
			p = memWriteQueue.poll();
			
			if(p.ip == instructionPointer)
			{
				return p.tgt;
			}
			
		}while(!memWriteQueue.isEmpty());
		
		//Could not find the address in the queue
		return -1;
	}
	
	public BranchInstr getBranchInfo(long instructionPointer)
	{
		Packet p;
				
		do
		{
			p = branchQueue.poll();
			
			if(p.ip == instructionPointer)
			{
				return new BranchInstr(p.value==4, p.tgt);
			}
			
		}while(!branchQueue.isEmpty());
		
		//Could not find the address in the queue
		return null;
	}
}