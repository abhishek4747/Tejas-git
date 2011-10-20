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

public class DynamicInstruction {

	private long instructionPointer;

	private int threadId;

	private boolean branchTaken;
	private long branchTargetAddress;

	private ArrayList<Long> memoryReadAddress;
	private ArrayList<Long> memoryWriteAddress;

	private ArrayList<Long> sourceRegisters;
	private ArrayList<Long> destinationRegister;

	public DynamicInstruction() {
	}

	public DynamicInstruction(long instructionPointer, int threadId,
			boolean branchTaken, long branchTargetAddress,
			ArrayList<Long> memoryReadAddress, ArrayList<Long> memoryWriteAddress,
			ArrayList<Long> sourceRegisters, ArrayList<Long> destinationRegister) {
		super();
		this.instructionPointer = instructionPointer;
		this.threadId = threadId;
		this.branchTaken = branchTaken;
		this.branchTargetAddress = branchTargetAddress;
		this.memoryReadAddress = memoryReadAddress;
		this.memoryWriteAddress = memoryWriteAddress;
		this.sourceRegisters = sourceRegisters;
		this.destinationRegister = destinationRegister;
	}

	public long getInstructionPointer() {
		return instructionPointer;
	}

	public void setInstructionPointer(long instructionPointer) {
		this.instructionPointer = instructionPointer;
	}

	public long getThreadId() {
		return threadId;
	}

	public void setThreadId(int threadId) {
		this.threadId = threadId;
	}

	public boolean isBranchTaken() {
		return branchTaken;
	}

	public void setBranchTaken(boolean branchTaken) {
		this.branchTaken = branchTaken;
	}

	public long getBranchTargetAddress() {
		return branchTargetAddress;
	}

	public void setBranchTargetAddress(long branchTargetAddress) {
		this.branchTargetAddress = branchTargetAddress;
	}

	public ArrayList<Long> getMemoryReadAddress() {
		return memoryReadAddress;
	}

	public void setMemoryReadAddress(ArrayList<Long> memoryReadAddress) {
		this.memoryReadAddress = memoryReadAddress;
	}

	public ArrayList<Long> getMemoryWriteAddress() {
		return memoryWriteAddress;
	}

	public void setMemoryWriteAddress(ArrayList<Long> memoryWriteAddress) {
		this.memoryWriteAddress = memoryWriteAddress;
	}

	public ArrayList<Long> getSourceRegisters() {
		return sourceRegisters;
	}

	public void setSourceRegisters(ArrayList<Long> sourceRegisters) {
		this.sourceRegisters = sourceRegisters;
	}

	public ArrayList<Long> getDestinationRegister() {
		return destinationRegister;
	}

	public void setDestinationRegister(ArrayList<Long> destinationRegister) {
		this.destinationRegister = destinationRegister;
	}

	public String toString() 
	{

		return (  "\n" + String.format("%-180s", " ").replace(" ", "-")
				+ "\nDynamicInstruction ..."
			    + "\ninstructionPointer=" + Long.toHexString(instructionPointer) + "\tthreadId=" + threadId
				+ "\nbranchTaken=" + branchTaken + "\tbranchTargetAddress=" + Long.toHexString(branchTargetAddress) +

				"\ndestinationRegister=" + destinationRegister
				+ "\tsourceRegisters=" + sourceRegisters +

				"\nmemoryReadAddress=" + memoryReadAddress
				+ "\tmemoryWriteAddress=" + memoryWriteAddress + "\n");
	}
}