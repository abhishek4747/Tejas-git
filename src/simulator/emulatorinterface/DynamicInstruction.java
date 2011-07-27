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

import java.util.Vector;

public class DynamicInstruction {

	private long instructionPointer;

	private int threadId;

	private boolean branchTaken;
	private long branchTargetAddress;

	private Vector<Long> memoryReadAddress;
	private Vector<Long> memoryWriteAddress;

	private Vector<Long> sourceRegisters;
	private Vector<Long> destinationRegister;

	public DynamicInstruction() {
	}

	public DynamicInstruction(long instructionPointer, int threadId,
			boolean branchTaken, long branchTargetAddress,
			Vector<Long> memoryReadAddress, Vector<Long> memoryWriteAddress,
			Vector<Long> sourceRegisters, Vector<Long> destinationRegister) {
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

	public Vector<Long> getMemoryReadAddress() {
		return memoryReadAddress;
	}

	public void setMemoryReadAddress(Vector<Long> memoryReadAddress) {
		this.memoryReadAddress = memoryReadAddress;
	}

	public Vector<Long> getMemoryWriteAddress() {
		return memoryWriteAddress;
	}

	public void setMemoryWriteAddress(Vector<Long> memoryWriteAddress) {
		this.memoryWriteAddress = memoryWriteAddress;
	}

	public Vector<Long> getSourceRegisters() {
		return sourceRegisters;
	}

	public void setSourceRegisters(Vector<Long> sourceRegisters) {
		this.sourceRegisters = sourceRegisters;
	}

	public Vector<Long> getDestinationRegister() {
		return destinationRegister;
	}

	public void setDestinationRegister(Vector<Long> destinationRegister) {
		this.destinationRegister = destinationRegister;
	}

	public String toString() {

		return ("\ninstructionPointer=" + instructionPointer + "\tthreadId=" + threadId
				+ "\nbranchTaken=" + branchTaken + "\tbranchTargetAddress=" + branchTargetAddress +

				"\ndestinationRegister=" + destinationRegister
				+ "\tsourceRegisters=" + sourceRegisters +

				"\nmemoryReadAddress=" + memoryReadAddress
				+ "\tmemoryWriteAddress=" + memoryWriteAddress);
	}
}