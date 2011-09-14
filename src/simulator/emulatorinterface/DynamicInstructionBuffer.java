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


public class DynamicInstructionBuffer 
{
	private Queue<DynamicInstruction> queue;

	public DynamicInstructionBuffer() 
	{
		// Create max number of queues
		queue = new LinkedList<DynamicInstruction>();
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
		Vector<Long> memReadAddr = new Vector<Long>();
		Vector<Long> memWriteAddr = new Vector<Long>();
		Vector<Long> srcRegs = new Vector<Long>();
		Vector<Long> dstRegs = new Vector<Long>();

		long ip = buildDynamicInstruction.elementAt(0).ip;
		boolean taken = false;
		long branchTargetAddress = 0;
		for (int i = 0; i < buildDynamicInstruction.size(); i++) {
			p = buildDynamicInstruction.elementAt(i);
			assert (ip == p.ip) : "all instruction pointers not matching";
			switch (p.value) {
			case (-1):
				break;
			case (0):
				assert (false) : "The value is reserved for locks. Most probable cause is a bad read";
				break;
			case (1):
				assert (false) : "The value is reserved for locks";
				break;
			case (2):
				memReadAddr.add(p.tgt);
				break;
			case (3):
				memWriteAddr.add(p.tgt);
				break;
			case (4):
				taken = true;
				branchTargetAddress = p.tgt;
				break;
			case (5):
				taken = false;
				branchTargetAddress = p.tgt;
				break;
			case (6):
				srcRegs.add(p.tgt);
				break;
			case (7):
				dstRegs.add(p.tgt);
				break;
			default:
				assert (false) : "error in configuring packets";
			}
		}

		queue.add(new DynamicInstruction(ip, tidEmu, taken,
				branchTargetAddress, memReadAddr, memWriteAddr, srcRegs,
				dstRegs));
	}

	public void addDynamicInstruction(DynamicInstruction dynamicInstruction) 
	{
		queue.add(dynamicInstruction);
	}

	public DynamicInstruction getNextDynamicInstruction(int threadID) 
	{
		try 
		{
			return queue.poll();
		} 
		catch (Exception exception) 
		{
			misc.Error.showErrorAndExit("\n\tThread " + threadID
					+ " unable to obtain next dynamic operation !!");
			
			return null;
		}
	}

	public boolean isEmpty(int threadID) 
	{
		return queue.isEmpty();
	}
}