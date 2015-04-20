package pipeline.multi_issue_inorder;

import generic.Operand;
import generic.OperationType;
import pipeline.FunctionalUnitType;
import pipeline.OpTypeToFUTypeMapping;
import config.CoreConfig;;

class RS {
	OperationType opType;
	boolean busy;
	int Qi;
	int Qj, Qk;
	Operand Vj, Vk;
	long A;
	boolean executionComplete;

	RS() {
		busy = false;
		Qi = -1;
		Qj = -1;
		Qk = -1;
		Vj = new Operand();
		Vk = new Operand();
		executionComplete = false;
		A = 0;
	}

	public String toString() {
		return "RS: busy:" + busy + " Qi:" + Qi + " Qj:" + Qj + " Qk:" + Qk
				+ " Vj:" + Vj + " Vk:" + Vk + " OpType:" + opType + " A:" + A;
	}
}

public class ReservationStation {
	int size;
	RS rs[];

	ReservationStation(int size) {
		this.size = size;
		this.rs = new RS[size];
		for (int i = 0; i < size; i++)
			rs[i] = new RS();
	}

	public int getFree() {
		for (int i = 0; i < size; i++) {
			if (!rs[i].busy) {
				return i;
			}
		}
		return -1;
	}

	public boolean isFull() {
		return this.getFree() == -1;
	}

	public int getBusy() {
		int busy = 0;
		for (int i = 0; i < size; i++) {
			if (rs[i].busy)
				busy++;
		}
		return busy;
	}

	public boolean isEmpty() {
		return this.getFree() > -1;
	}

	public int getIWithOp(OperationType op) {
		for (int i = 0; i < size; i++) {
			if (!rs[i].busy && !rs[i].executionComplete && rs[i].opType == op) {
				return i;
			}
		}
		return -1;
	}

	public int getIWithFu(FunctionalUnitType fu) {
		for (int i = 0; i < size; i++) {
			if (rs[i].opType != null && rs[i].busy && !rs[i].executionComplete
					&& OpTypeToFUTypeMapping.getFUType(rs[i].opType) == fu) {
				return i;
			}
		}
		return -1;
	}

	public boolean isEmpty(OperationType op) {
		return this.getIWithOp(op) < 0;
	}

	public boolean isEmpty(FunctionalUnitType fu) {
		return this.getIWithFu(fu) < 0;
	}

	public static int getRSSize() {
		return 10;
	}

	public int getExecuted() {
		int busy = 0;
		for (int i = 0; i < size; i++) {
			if (rs[i].busy && rs[i].executionComplete)
				busy++;
		}
		return busy;
	}
}
