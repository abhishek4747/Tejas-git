package pipeline.multi_issue_inorder;

import generic.OperationType;
import generic.Operand;

class RS {
	OperationType opType;
	boolean busy;
	int Qi;
	int Qj, Qk;
	Operand Vj, Vk;

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
	
	public static int getRSSize(){
		return 10;
	}
}