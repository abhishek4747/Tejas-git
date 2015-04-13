package pipeline.multi_issue_inorder;

import generic.Core;
import generic.Instruction;
import generic.Operand;
import generic.OperationType;

class ROBSlot {
	Instruction instr;
	boolean busy;
	boolean ready;
	Operand r1;
	boolean r1avail;
	Operand r2;
	boolean r2avail;
	Operand dest;
}

public class ROB {
	public ROBSlot[] rob;
	public int ROBSize;
	public int head;
	public int tail;
	public int curSize;

	Core core;
	MultiIssueInorderExecutionEngine containingExecutionEngine;

	long lastValidIpSeen;
	int numMispredictedBranches;
	int numBranches;

	ROB(Core core, MultiIssueInorderExecutionEngine execEngine, int ROBSize) {
		this.core = core;
		this.containingExecutionEngine = execEngine;
		this.ROBSize = ROBSize;
		rob = new ROBSlot[ROBSize];
		for (int i = 0; i < ROBSize; i++)
			rob[i] = new ROBSlot();
		head = -1;
		tail = -1;
		curSize = 0;
		lastValidIpSeen = -1;
		numMispredictedBranches = 0;
		numBranches = 0;
	}

	public boolean add(Instruction i) {
		if (curSize != ROBSize && !rob[tail].busy) {
			rob[tail].instr = i;
			rob[tail].busy = true;
			rob[tail].ready = false;
			rob[tail].r1avail = false;
			rob[tail].r2avail = false;
			rob[tail].r1 = i.getSourceOperand1();
			rob[tail].r2 = i.getSourceOperand2();
			rob[tail].dest = i.getDestinationOperand();
			tail = (tail + 1) % ROBSize;
			curSize++;
			return true;
		}
		return false;
	}

	public boolean removeFromHead() {
		rob[head].busy = false;
		if (head == tail) {
			head = -1;
			tail = -1;
		} else
			head = (head + 1) % ROBSize;
		curSize--;
		return true;
	}

	void flush() {
		for (int i = 0; i < ROBSize; i++)
			rob[i].busy = false;
		head = -1;
		tail = -1;
		curSize = 0;
	}

	public void performCommit(RF rf) {
		if (head == -1)
			return;
		if (rob[head].instr.getCISCProgramCounter() != -1) {
			lastValidIpSeen = rob[head].instr.getCISCProgramCounter();
		}
		if (rob[head].ready) {
			if (rob[head].instr.getOperationType() == OperationType.branch) {
				boolean prediction = containingExecutionEngine
						.getBranchPredictor().predict(lastValidIpSeen,
								rob[head].instr.isBranchTaken());
				this.containingExecutionEngine.getBranchPredictor()
						.incrementNumAccesses(1);

				containingExecutionEngine.getBranchPredictor().Train(
						rob[head].instr.getCISCProgramCounter(),
						rob[head].instr.isBranchTaken(), prediction);
				this.containingExecutionEngine.getBranchPredictor()
						.incrementNumAccesses(1);

				numBranches++;

				if (prediction != rob[head].instr.isBranchTaken()) {
					containingExecutionEngine.setMispredStall(core
							.getBranchMispredictionPenalty());
					numMispredictedBranches++;
					flush();
					rf.flush();
				}
			}
			removeFromHead();
			core.incrementNoOfInstructionsExecuted();
			if (core.getNoOfInstructionsExecuted() % 1000000 == 0)
				System.out.println(core.getNoOfInstructionsExecuted() / 1000000
						+ " million done on " + core.getCore_number());
		}
	}

}