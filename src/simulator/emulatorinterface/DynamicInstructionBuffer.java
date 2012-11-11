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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import emulatorinterface.communication.Encoding;
import emulatorinterface.communication.Packet;
import generic.BranchInstr;


public class DynamicInstructionBuffer implements Encoding
{
	//FIXME: Queue must be maintained using pooling scheme.
	//Current scheme will put pressure on the garbage collector
	private Queue<ArrayList<Packet>> memReadQueue = null;
	private Queue<ArrayList<Packet>> memWriteQueue = null;
	private Queue<Packet> branchQueue = null;
	
	public DynamicInstructionBuffer() 
	{
		// Create max number of queues
		memReadQueue = new LinkedList<ArrayList<Packet>>();
		memWriteQueue = new LinkedList<ArrayList<Packet>>();
		branchQueue = new LinkedList<Packet>();
	}

	// read the packets from the arrayList and place them in suitable queues
	public void configurePackets(EmulatorPacketList arrayListPacket) 
	{
		Packet p;
		ArrayList<Packet> memReadAddr = new ArrayList<Packet>();
		ArrayList<Packet> memWriteAddr = new ArrayList<Packet>();
		Packet branchPacket = null;

		long ip = arrayListPacket.get(0).ip;
		
		for (int i = 0; i < arrayListPacket.size(); i++) 
		{
			p = arrayListPacket.get(i);
			assert (ip == p.ip) : "all instruction pointers not matching";
			
			// System.out.println(i + " : " + p);
			
			switch ((int)p.value) 
			{
				case (-1):
					break;
				
				case (0):
					misc.Error.showErrorAndExit("error in configuring packets "+p.value);
					break;
					
				case (1):
					misc.Error.showErrorAndExit("error in configuring packets "+p.value);
					break;
					
				case (MEMREAD):
					memReadAddr.add(p);
					break;
					
				case (MEMWRITE):
					memWriteAddr.add(p);
					break;
					
				case (TAKEN):
					branchPacket = p;
					break;
					
				case (NOTTAKEN):
					branchPacket = p;
					break;
					
				default:
//					System.out.println("error in configuring packets"+p.value);
//					misc.Error.showErrorAndExit("error in configuring packets"+p.value);
					
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
			//	System.out.print("\n\tExtra branch instruction found : original instruction=" +
			//			Long.toHexString(instructionPointer) + " found instruction=" + 
			//			Long.toHexString(headPacket.ip) + "\n");

			//	System.exit(0);
			}
		}
		
		return null;
	}
	
	public long getSingleLoadAddress(long instructionPointer)
	{
		ArrayList<Packet> headPacket = null;
		
		while(!memReadQueue.isEmpty())
		{
			headPacket = memReadQueue.poll();
			
			// check the ip of this instruction
			if(headPacket.get(0).ip == instructionPointer)
			{
				// return the first readAddress.
				// Hope that this instruction does not have more pending reads.
				return headPacket.get(0).tgt;
			}
			else
			{
			//	System.out.print("\n\tExtra memRead instruction found : original instruction=" +
			//			Long.toHexString(instructionPointer) + " found instruction=" + 
			//			Long.toHexString(headPacket.get(0).ip) + "\n");
				
//				System.exit(0);
			}
		}
		
		return -1;
	}
	
	public LinkedList<Long> getMultipleLoadAddresses(long instructionPointer)
	{
		ArrayList<Packet> headPacket = null;
		
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
			//	System.out.print("\n\tExtra memRead instruction found : original instruction=" +
			//			Long.toHexString(instructionPointer) + " found instruction=" + 
			//			Long.toHexString(headPacket.get(0).ip) + "\n");
				
//				System.exit(0);
			}
		}
		
		return null;
	}
	
	public long getSingleStoreAddress(long instructionPointer)
	{
		ArrayList<Packet> headPacket = null;
		
		while(!memWriteQueue.isEmpty())
		{
			headPacket = memWriteQueue.poll();
			
			// check the ip of this instruction
			if(headPacket.get(0).ip == instructionPointer)
			{
				return headPacket.get(0).tgt;
			}
			else
			{
			//	System.out.print("\n\tExtra memWrite instruction found : original instruction=" +
			//			Long.toHexString(instructionPointer) + " found instruction=" + 
			//			Long.toHexString(headPacket.get(0).ip) + "\n");

//				System.exit(0);
			}
		}
		
		return -1;
	}

	
	public LinkedList<Long> getMultipleStoreAddresses(long instructionPointer)
	{
		ArrayList<Packet> headPacket = null;
		
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
			//	System.out.print("\n\tExtra memWrite instruction found : original instruction=" +
				//		Long.toHexString(instructionPointer) + " found instruction=" + 
					//	Long.toHexString(headPacket.get(0).ip) + "\n");

			//	System.exit(0);
			}
		}
		
		return null;
	}
	
	/*
	 * This function removes multiple packets belonging to instructionPointer
	 * from the head of the queue. 
	 */
	public void gobbleInstruction(long instructionPointer)
	{
		// gobble branch instructions
		Packet headBranchPacket;
		while(!this.branchQueue.isEmpty())
		{
			headBranchPacket = branchQueue.peek();
			
			if(headBranchPacket.ip == instructionPointer)
			{
				// remove all the branch instructions whose ip=instructionPointer
				branchQueue.poll();
			}
			else
			{
				// no need to look into this queue further
				break;
			}
		}

		// gobble memRead instructions		
		ArrayList<Packet> headMemReadPacket;
		while(!this.memReadQueue.isEmpty())
		{
			headMemReadPacket = memReadQueue.peek();
			
			if(headMemReadPacket.get(0).ip == instructionPointer)
			{
				// remove all memRead instructions whose ip=instructionPointer
				memReadQueue.poll();
			}
			else
			{
				// no need to look into this queue further
				break;
			}
		}
		
		// gobble memWrite instructions
		ArrayList<Packet> headMemWritePacket;
		while(!this.memWriteQueue.isEmpty())
		{
			headMemWritePacket = memWriteQueue.peek();
			
			if(headMemWritePacket.get(0).ip == instructionPointer)
			{
				// remove all memRead instructions whose ip=instructionPointer
				memWriteQueue.poll();
			}
			else
			{
				// no need to look into this queue further
				break;
			}
		}
	}

	public void clearBuffer() 
	{
		this.branchQueue.clear();
		this.memReadQueue.clear();
		this.memWriteQueue.clear();
	}
	
	public void printBuffer()
	{
		//print branch info
		System.out.print("\n\n\tBranch Info : ");
		if(branchQueue.isEmpty())
		{
			Object[] branchInstrs = branchQueue.toArray();
			Packet branchPacket;
			for(int i=0; i<branchInstrs.length; i++)
			{
				branchPacket = (Packet)branchInstrs[i];
				System.out.print("\ttaken = " + (branchPacket.value==4) + " addr = " + branchPacket.tgt + "\n");
			}
		}
		//print memory read addresses
	}
}