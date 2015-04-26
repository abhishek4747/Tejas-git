package pipeline.multi_issue_inorder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;
import generic.RequestType;

public class CommonDataBus extends SimulationElement {
	int size;
	boolean busy[];
	int register[];
	Object value[];
	int occupied;
	ROB rob;

	public CommonDataBus(Core core,
			MultiIssueInorderExecutionEngine execEngine, int size) {
		super(PortType.FirstComeFirstServe, size, 1, 0, -1);
		this.size = size;
		rob = execEngine.getROB();
		this.busy = new boolean[size];
		this.register = new int[size];
		this.value = new Object[size];
		this.occupied = 0;
		for (int i = 0; i < size; i++) {
			this.busy[i] = false;
		}
	}

	public int find(int register) {
		for (int i = 0; i < size; i++) {
			if (this.register[i] == register) {
				return i;
			}
		}
		return -1;
	}

	public boolean insert(int register, EventQueue eventQueue) {
		// Not sure if this register is already there
		// int r = find(register);
		// if (r == -1) {
		// for (int i = 0; i < size; i++) {
		// if (!this.busy[i]) {
		// this.register[i] = register;
		// this.value[i] = value;
		// this.busy[i] = true;
		// this.flushCDB();
		// return true;
		// }
		// }
		// } else {
		// this.value[r] = value;
		// if (this.busy[r]) {
		// System.out
		// .println("Something might be wrong. Overwriting register "
		// + r + " in CDB.");
		// } else {
		// occupied++;
		// }
		// this.busy[r] = true;
		// this.flushCDB();
		// return true;
		// }
		// return false;
		CDBEvents event = new CDBEvents(eventQueue, this, rob,
				RequestType.WriteToCDB, rob, register, 1);

		this.getPort().put(event);
		// rob.rob.absPeek(register).ready = true;
		return true;
	}

	public Object get(int register) {
		int r = find(register);
		if (r == -1) {
			return null;
		} else {
			if (!busy[r]) {
				System.out
						.println("Something might be wrong. Reading register "
								+ r + " again.");
			} else {
				occupied--;
			}
			busy[r] = false;
			return value[r];
		}
	}

	public void flushCDB() {
		for (int i = 0; i < size; i++) {
			if (busy[i]) {
				rob.rob.absPeek(register[i]).ready = true;
			}
		}
	}

	public boolean isFull() {
		return occupied == size;
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO
	}
}
