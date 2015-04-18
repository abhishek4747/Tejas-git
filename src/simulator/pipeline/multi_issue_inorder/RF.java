package pipeline.multi_issue_inorder;

import generic.Event;
import generic.EventQueue;
import generic.Operand;
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
		super(null, -1, -1, -1, -1);
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

	public static int getRFSize() {
		return 5;
	}
}