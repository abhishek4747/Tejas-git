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


public class DynamicInstructionBuffer 
{
	private Queue<Vector<Packet>> memReadQueue = null;
	private Queue<Vector<Packet>> memWriteQueue = null;
	private Queue<Packet> branchQueue = null;
	
	public DynamicInstructionBuffer() 
	{
		// Create max number of queues
		memReadQueue = new LinkedList<Vector<Packet>>();
		memWriteQueue = new LinkedList<Vector<Packet>>();
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

	public void configurePackets(Vector<Packet> vectorPacket,
			int tid2, int tidEmu) 
	{
		Packet p;
		Vector<Packet> memReadAddr = new Vector<Packet>();
		Vector<Packet> memWriteAddr = new Vector<Packet>();
		Packet branchPacket = null;

		long ip = vectorPacket.elementAt(0).ip;
		
		for (int i = 0; i < vectorPacket.size(); i++) 
		{
			p = vectorPacket.elementAt(i);
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
					memReadAddr.add(p);
					break;
					
				case (3):
					memWriteAddr.add(p);
					break;
					
				case (4):
					branchPacket = p;
					break;
					
				case (5):
					branchPacket = p;
					break;
					
				default:
					assert (false) : "error in configuring packets";
			}
		}
		
		if(!memReadAddr.isEmpty())
		{
			this.memReadQueue.add(memReadAddr);
		}
		
		if(!memWriteAddr.isEmpty())
		{
			this.memWriteQueue.add(memWriteAddr);
		}
		
		if(branchPacket != null)
		{
			this.branchQueue.add(branchPacket);
		}
	}
	
	public BranchInstr getBranchPacket(long instructionPointer)
	{
		Packet headPacket = null;
		
		while(!branchQueue.isEmpty())
		{
			headPacket = branchQueue.poll();
			
			if(headPacket.ip == instructionPointer)
			{
				return new BranchInstr(headPacket.value==4, headPacket.tgt);
			}
			else
			{
				System.out.print("\n\tExtra instruction found !!\n");
				System.exit(0);
			}
		}
		
		return null;
	}
	
	public LinkedList<Long> getmemoryReadAddress(long instructionPointer)
	{
		Vector<Packet> headPacket = null;
		
		while(!memReadQueue.isEmpty())
		{
			headPacket = memReadQueue.poll();
			
			// check the ip of this instruction
			if(headPacket.get(0).ip == instructionPointer)
			{
				// read Addresses contains all addresses read by this instruction.
				LinkedList<Long> readAddessList = new LinkedList<Long>();

				for(int i=0; i<headPacket.size(); i++)
				{
					readAddessList.add(headPacket.get(i).tgt);
				}
				
				return readAddessList;
			}
			else
			{
				System.out.print("\n\tExtra instruction found !!\n");
				System.exit(0);
			}
		}
		
		return null;
	}
	
	public LinkedList<Long> getmemoryWriteAddress(long instructionPointer)
	{
		Vector<Packet> headPacket = null;
		
		while(!memWriteQueue.isEmpty())
		{
			headPacket = memWriteQueue.poll();
			
			// check the ip of this instruction
			if(headPacket.get(0).ip == instructionPointer)
			{
				// read Addresses contains all addresses read by this instruction.
				LinkedList<Long> writeAddessList = new LinkedList<Long>();
								
				for(int i=0; i<headPacket.size(); i++)
				{
					writeAddessList.add(headPacket.get(i).tgt);
				}
								
				return writeAddessList;
			}
			else
			{
				System.out.print("\n\tExtra instruction found !!\n");
				System.exit(0);
			}
		}
		
		return null;
	}

	public void clearBuffer() 
	{
		this.branchQueue.clear();
		this.memReadQueue.clear();
		this.memWriteQueue.clear();
	}
}