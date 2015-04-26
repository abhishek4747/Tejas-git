package pipeline.multi_issue_inorder;

import generic.Event;
import generic.EventQueue;
import generic.Operand;
import generic.OperandType;
import generic.PortType;
import generic.SimulationElement;

class Register {
	boolean busy;
	Operand value;
	int Qi;
}

public class RF extends SimulationElement {
	int size;
	Register rf[];

	RF(int size) {
		super(PortType.Unlimited, -1, -1, -1, -1);
		this.size = size;
		rf = new Register[size];
		for (int i = 0; i < size; i++)
			rf[i] = new Register();
	}

	void flush() {
		for (int i = 0; i < size; i++)
			rf[i].Qi = 0;
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub

	}

	public static Register getRegister(RF irf, RF frf, Operand op, int index) {
		if (op.getOperandType() == OperandType.integerRegister) {
			return irf.rf[index];
		} else if (op.getOperandType() == OperandType.floatRegister) {
			return frf.rf[index];
		} else {
			return null;
		}
	}

	public static Register getRegister(RF irf, RF frf, Operand op) {
		return RF.getRegister(irf, frf, op, (int) op.getValue());
	}
}