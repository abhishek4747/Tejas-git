package pipeline.multi_issue_inorder;

import generic.Core;
import generic.Instruction;
import generic.Operand;
import generic.OperationType;
import generic.GenericCircularQueue;

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

	ROB(Core core, MultiIssueInorderExecutionEngine execEngine, int ROBSize) {
		this.core = core;
		this.containingExecutionEngine = execEngine;
		this.ROBSize = ROBSize;
		rob = new GenericCircularQueue<ROBSlot>(ROBSlot.class, ROBSize);
		lastValidIpSeen = -1;
		numMispredictedBranches = 0;
		numBranches = 0;
	}

	public boolean add(Instruction i, long insCompletesAt) {
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
			return true;
		}
		return false;
	}

	public int getTail() {
		if (rob.isFull())
			return -2;
		if (rob.isEmpty())
			return -1;
		return rob.getTail();
	}

	public void removeFromHead() {
		rob.dequeue();
	}

	void flush() {
		rob.clear();
	}

	public void performCommit(RF rf) {
		if (rob.isEmpty())
			return;
		System.out.print("in commit unit, performCommit(RF)");
		if (rob.peek(0).instr.getCISCProgramCounter() != -1) {
			lastValidIpSeen = rob.peek(0).instr.getCISCProgramCounter();
		}
		if (rob.peek(0).ready) {
			System.out.print(" in rob.peek(0).ready");
			if (rob.peek(0).instr.getOperationType() == OperationType.branch) {
				System.out.print(" Branch");
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
					System.out.print(" taken");
					containingExecutionEngine.setMispredStall(core
							.getBranchMispredictionPenalty());
					numMispredictedBranches++;
					flush();
					rf.flush();
				}
			}
			if (rf.rf[(int) rob.peek(0).dest.getValue()].Qi == rob.getHead()) {
				rf.rf[(int) rob.peek(0).dest.getValue()].busy = false;
			}
			removeFromHead();

			core.incrementNoOfInstructionsExecuted();
			if (core.getNoOfInstructionsExecuted() % 1000000 == 0)
				System.out.println(core.getNoOfInstructionsExecuted() / 1000000
						+ " million done on " + core.getCore_number());
		}
		System.out.println();
	}

	public static int getROBSize() {
		return 10;
	}
}