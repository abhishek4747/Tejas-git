package pipeline.multi_issue_inorder;

import generic.Core;
import generic.GenericCircularQueue;
import generic.Instruction;
import generic.Operand;
import generic.OperationType;
import generic.PinPointsProcessing;
import generic.RequestType;
import config.SimulationConfig;

class ROBSlot {
	Instruction instr;
	boolean busy;
	boolean ready;
	Operand r1;
	boolean r1avail;
	Operand r2;
	boolean r2avail;
	Operand dest;
	long instructionCompletesAt;
}

public class ROB {
	GenericCircularQueue<ROBSlot> rob;
	public int ROBSize;

	Core core;
	MultiIssueInorderExecutionEngine containingExecutionEngine;

	long lastValidIpSeen;
	int numMispredictedBranches;
	int numBranches;
	RF irf, frf;

	ROB(Core core, MultiIssueInorderExecutionEngine execEngine, int ROBSize) {
		this.core = core;
		this.containingExecutionEngine = execEngine;
		this.ROBSize = ROBSize;
		rob = new GenericCircularQueue<ROBSlot>(ROBSlot.class, ROBSize);
		irf = execEngine.getIntRF();
		frf = execEngine.getFloatRF();
		lastValidIpSeen = -1;
		numMispredictedBranches = 0;
		numBranches = 0;
	}

	public int add(Instruction i, long insCompletesAt) {
		if (!rob.isFull()) {
			ROBSlot r = new ROBSlot();
			r.busy = true;
			r.instr = i;
			r.ready = false;
			r.r1avail = false;
			r.r2avail = false;
			r.r1 = i.getSourceOperand1();
			r.r2 = i.getSourceOperand2();
			r.dest = i.getDestinationOperand();
			r.instructionCompletesAt = insCompletesAt;
			rob.enqueue(r);
			return indexOf(r);
		}
		return -1;
	}

	private int indexOf(ROBSlot r) {
		for (int i = 0; i < ROBSize; i++)
			if (r == rob.absPeek(i))
				return i;
		return -1;
	}

	public int getTail() {
		if (rob.isFull())
			return -2;
		if (rob.isEmpty())
			return -1;
		return rob.getTail();
	}

	public int getFreeTail() {
		return (getTail() + 1) % rob.getBufferSize();
	}

	public void removeFromHead() {
		System.out.println("inside removefromhead");
		rob.dequeue();
	}

	void flush() {
		rob.clear();
	}

	public void performCommit() {
		System.out.println("6--> In commit Unit");
		if (rob.isEmpty()) {
			System.out.println("\tROB empty. Nothing to be done.");
			return;
		}
		Instruction ins = rob.peek(0).instr;
		if (ins != null) {
			// check if simulation complete
			if (ins.getOperationType() == OperationType.inValid) {
				this.core.currentThreads--;

				if (this.core.currentThreads == 0) { // set exec complete
														// only if there are
														// n other thread
														// already
														// assigned to this
														// pipeline
					containingExecutionEngine.setExecutionComplete(true);
					if (SimulationConfig.pinpointsSimulation == false) {
						containingExecutionEngine.setTimingStatistics();
						containingExecutionEngine
								.setPerCoreMemorySystemStatistics();
					} else {
						PinPointsProcessing.processEndOfSlice();
					}
				}
			}
			// else {
			// if (core.getNoOfInstructionsExecuted() % 1000000 == 0) {
			// System.out.println(core.getNoOfInstructionsExecuted()
			// / 1000000 + " million done" + " by core "
			// + core.getCore_number()
			// + " global clock cycle "
			// + GlobalClock.getCurrentTime());
			// }
			// core.incrementNoOfInstructionsExecuted();
			// }

			while (rob.peek(0) != null && rob.peek(0).ready) {
				if (rob.peek(0).instr.getCISCProgramCounter() != -1) {
					lastValidIpSeen = rob.peek(0).instr.getCISCProgramCounter();
				}
				ins = rob.peek(0).instr;
				if (ins == null) {
					break;
				}
				System.out.print("\tRob head is ready to be committed.");
				if (ins.getOperationType() == OperationType.store) {
					boolean memReqIssued = containingExecutionEngine.multiIssueInorderCoreMemorySystem
							.issueRequestToL1Cache(RequestType.Cache_Write,
									ins.getSourceOperand1MemValue());
					if (!memReqIssued)
						System.out.println("Error in issuing store request");
				} else if (ins.getOperationType() == OperationType.branch) {
					System.out.print("\tBranch Instruction");
					boolean prediction = containingExecutionEngine
							.getBranchPredictor().predict(lastValidIpSeen,
									rob.peek(0).instr.isBranchTaken());
					this.containingExecutionEngine.getBranchPredictor()
							.incrementNumAccesses(1);

					containingExecutionEngine.getBranchPredictor().Train(
							rob.peek(0).instr.getCISCProgramCounter(),
							rob.peek(0).instr.isBranchTaken(), prediction);
					this.containingExecutionEngine.getBranchPredictor()
							.incrementNumAccesses(1);

					numBranches++;

					if (prediction != rob.peek(0).instr.isBranchTaken()) {
						System.out.println("\tFlushing ROB coz mispredicted");
						containingExecutionEngine.setMispredStall(core
								.getBranchMispredictionPenalty());
						numMispredictedBranches++;
						flush();
						irf.flush();
						frf.flush();
					}
				}

				Operand toBeFreed = rob.peek(0).dest;
				if (ins.getOperationType() == OperationType.store) {
					toBeFreed = rob.peek(0).r2;
				}
				if (RF.getRegister(irf, frf, toBeFreed).Qi == rob.getHead()) {
					RF.getRegister(irf, frf, toBeFreed).busy = false;
				}

				removeFromHead();

				core.incrementNoOfInstructionsExecuted();
				if (core.getNoOfInstructionsExecuted() % 1000000 == 0)
					System.out.println(core.getNoOfInstructionsExecuted()
							/ 1000000 + " million done on "
							+ core.getCore_number());
			}
		}
	}

	public int getIndex(Instruction ins) {
		for (int i = rob.getHead(); i < rob.getHead() + rob.size(); i++) {
			int j = i % ROBSize;
			if (rob.absPeek(j).instr == ins)
				return j;
		}
		return -1;
	}

	public int getRelIndex(Instruction ins) {
		return (getIndex(ins) - rob.getHead() + rob.getBufferSize())
				% rob.getBufferSize();
	}

	public boolean storesAtThisAddressBefore(Instruction ins) {
		int r = getRelIndex(ins);
		for (int i = 0; i < r; i++) {
			int j = i;
			if (rob.peek(j).instr.getOperationType() == OperationType.store
					&& rob.peek(j).instr.getSourceOperand1MemValue() == ins
							.getSourceOperand1MemValue())
				return false;
		}
		return true;
	}
}
