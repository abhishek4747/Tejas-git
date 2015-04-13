package pipeline.multi_issue_inorder;

import generic.Operand;

class Register {
	int busy;
	Operand value;
	int Qi;

}

public class RF {
	int size;
	Register rf[];

	RF(int size) {
		this.size = size;
		rf = new Register[size];
		for (int i = 0; i < size; i++)
			rf[i] = new Register();
	}

	void flush() {
		for (int i = 0; i < size; i++)
			rf[i].Qi = 0;
	}
}