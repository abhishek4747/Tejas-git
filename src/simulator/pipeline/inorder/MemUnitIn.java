package pipeline.inorder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.Instruction;
import generic.PortType;
import generic.SimulationElement;

public class MemUnitIn extends SimulationElement{
	Core core;
	EventQueue eventQueue;
	public MemUnitIn(Core core) {
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		this.eventQueue = core.getEventQueue();
		// TODO Auto-generated constructor stub
	}
	
	public void performMemEvent(){
		Instruction ins = core.getInorderPipeline().getExMemLatch().getInstruction();
		StageLatch memWbLatch = core.getInorderPipeline().getMemWbLatch();
		StageLatch exMemLatch = core.getInorderPipeline().getExMemLatch();
//		if(exMemLatch.getStallCount()>0){
			if(ins!=null){
				memWbLatch.setInstruction(ins);
				memWbLatch.setIn1(exMemLatch.getIn1());
				memWbLatch.setIn2(exMemLatch.getIn2());
				memWbLatch.setOut1(exMemLatch.getOut1());
				memWbLatch.setOperationType(exMemLatch.getOperationType());
			}
//		}
//		else{
//			exMemLatch.decrementStallCount();
//			memWbLatch.incrementStallCount();
//		}
		
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}
}
