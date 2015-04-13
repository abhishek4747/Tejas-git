package pipeline.multi_issue_inorder;

import generic.Instruction;
import generic.Operand;

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

	ROB(int ROBSize) {
		this.ROBSize = ROBSize;
		rob = new ROBSlot[ROBSize];
		for (int i = 0; i < ROBSize; i++)
			rob[i] = new ROBSlot();
		head = -1;
		tail = -1;
		curSize = 0;
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
		return true;
	}

}