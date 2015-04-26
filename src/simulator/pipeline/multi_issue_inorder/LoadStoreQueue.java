package pipeline.multi_issue_inorder;

import generic.Instruction;

import java.util.Iterator;
import java.util.LinkedList;

class LSQEntry {
	boolean isLoad;
	int ROBEntry;
	long address;
	Instruction instruction;

	LSQEntry(boolean isLoad, int ROBEntry, long address, Instruction i) {
		this.isLoad = isLoad;
		this.address = address;
		this.ROBEntry = ROBEntry;
		this.instruction = i;
	}
}

public class LoadStoreQueue {
	LinkedList<LSQEntry> LSQ;

	public LoadStoreQueue() {
		LSQ = new LinkedList<LSQEntry>();
	}

	public void enqueue(boolean isLoad, int ROBEntry, long address,
			Instruction instr) {
		LSQ.add(new LSQEntry(isLoad, ROBEntry, address, instr));
	}

	public LSQEntry dequeue() {
		return LSQ.pop();
	}
	
	public LSQEntry dequeue(int index) {
		return LSQ.remove(index);
	}

	public boolean noStoresBefore(int index) {
		for (int i = 0; i < index; i++)
			if (!LSQ.get(i).isLoad)
				return false;
		return true;
	}

	public int getIndex(Instruction ins) {
		Iterator<LSQEntry> i = LSQ.iterator();
		int c = 0;
		while (i.hasNext()) {
			LSQEntry l = i.next();
			if (l.instruction == ins)
				return c;
			c++;
		}
		return -1;
	}
}
