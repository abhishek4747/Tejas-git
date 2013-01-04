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

import emulatorinterface.communication.Encoding;
import emulatorinterface.communication.Packet;

public class DynamicInstructionBuffer implements Encoding
{
	private long memRead[];
	int memReadSize, memReadCount;
	
	private long memWrite[];
	int memWriteSize, memWriteCount;
	
	private boolean branchTaken;
	private long branchAddress;
	
	private long ip;
	
	public DynamicInstructionBuffer() 
	{
		memRead = new long[64];
		memWrite = new long[64];
	}

	// read the packets from the arrayList and place them in suitable queues
	public void configurePackets(EmulatorPacketList arrayListPacket) 
	{
		memReadCount = 0;  memReadSize = 0;
		memWriteCount = 0; memWriteSize = 0;
		
		branchAddress = -1;
		
		ip = arrayListPacket.get(0).ip;
		
		for (int i = 0; i < arrayListPacket.size(); i++) 
		{
			Packet p = arrayListPacket.get(i);
			assert (ip == p.ip) : "all instruction pointers not matching";
			
			// System.out.println(i + " : " + p);
			
			switch ((int)p.value) 
			{
				case (-1):
					break;
				
				case (0):
					misc.Error.showErrorAndExit("error in configuring packets "+p);
					break;
					
				case (1):
					misc.Error.showErrorAndExit("error in configuring packet : " + p);
					break;
					
				case (MEMREAD):
					memRead[memReadSize++] = p.value;
					break;
					
				case (MEMWRITE):
					memRead[memWriteSize++] = p.value;
					break;
					
				case (TAKEN):
					branchTaken = true;
					branchAddress = p.value;
					break;
					
				case (NOTTAKEN):
					branchTaken = false;
					branchAddress = p.value;
					break;
					
				default:
//					System.out.println("error in configuring packets"+p.value);
//					misc.Error.showErrorAndExit("error in configuring packets"+p.value);
					
			}
		}
	}
	
	public boolean getBranchTaken(long instructionPointer)
	{
		return branchTaken;
	}
	
	public long getBranchAddress(long instructionPointer)
	{
		return branchAddress;
	}
	
	public long getSingleLoadAddress(long instructionPointer)
	{
		long ret = -1;
		
		if(memReadCount<memReadSize) {
			ret = memRead[memReadCount++];
		} else {
//			System.err.println("expected load address : " +
//				"ip = " + Long.toHexString(ip).toLowerCase()+   
//				"\tinstructionP = " + Long.toHexString(instructionPointer).toLowerCase() + " !!");
		}
		
		return ret;
	}
	
		
	public long getSingleStoreAddress(long instructionPointer)
	{
		long ret = -1;
		
		if(memWriteCount<memWriteSize) {
			ret = memWrite[memWriteCount++];
		} else {
//			System.err.println("expected store address : " +
//				"ip = " + Long.toHexString(ip).toLowerCase()+   
//				"\tinstructionP = " + Long.toHexString(instructionPointer).toLowerCase() + " !!");
		}
		
		return ret;
	}

	public void printBuffer()
	{
		//TODO
	}
}