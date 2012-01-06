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
		StageLatch memWbLatch = core.getExecutionEngineIn().getMemWbLatch();
		StageLatch exMemLatch = core.getExecutionEngineIn().getExMemLatch();
		Instruction ins = exMemLatch.getInstruction();
		if(!exMemLatch.getMemDone())
			core.getExecutionEngineIn().getFetchUnitIn().setStall(1);
		if(ins!=null){
			memWbLatch.setInstruction(ins);
			memWbLatch.setIn1(exMemLatch.getIn1());
			memWbLatch.setIn2(exMemLatch.getIn2());
			memWbLatch.setOut1(exMemLatch.getOut1());
			memWbLatch.setOperationType(exMemLatch.getOperationType());
		}
		else{
			memWbLatch.setInstruction(null);
		}
	}


	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		core.getExecutionEngineIn().getFetchUnitIn().setStall(0);
		core.getExecutionEngineIn().getExMemLatch().setMemDone(true);
		
	}
}
