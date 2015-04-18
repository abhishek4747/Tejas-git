package pipeline.multi_issue_inorder;

import generic.Event;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;

public class CommonDataBus extends SimulationElement{
	int size;
	boolean busy[];
	int register[];
	Object value[];
	int occupied;
	
	public CommonDataBus(int size) {
		super(PortType.FirstComeFirstServe, CommonDataBus.getCDBSize(), 1, 0, -1);
		this.size = size;
		this.busy = new boolean[size];
		this.register = new int[size];
		this.value = new Object[size];
		this.occupied = 0;
		for (int i = 0; i < size; i++) {
			this.busy[i] = false;
		}
	}
	
	public int find(int register){
		for (int i=0; i<size; i++){
			if (this.register[i]==register){
				return i;
			}
		}
		return -1;
	}
	
	public boolean insert(int register, Object value){
		// Not sure if this register is already there
		int r = find(register);
		if (r==-1){
			for (int i = 0; i < size; i++) {
				if (!this.busy[i]) {
					this.register[i] = register;
					this.value[i] = value;
					this.busy[i] = true;
					return true;
				}
			}
		}else{
			this.value[r] = value;
			if (this.busy[r]){
				System.out.println("Something might be wrong. Overwriting register "+r+" in CDB.");
			}else{
				occupied++;
			}
			this.busy[r] = true;
			return true;
		}
		return false;
	}
	
	public Object get(int register){
		int r = find(register);
		if (r==-1){
			return null;
		}else{
			if (!busy[r]){
				System.out.println("Something might be wrong. Reading register "+r+" again.");
			}else{
				occupied--;
			}
			busy[r] = false;
			return value[r];
		}
	}
	
	public boolean isFull(){
		return occupied==size;
	}
	
	public static int getCDBSize(){
		return 4;
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}
	
}
