package pipeline.inorder;


import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import memorysystem.MemorySystem;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.Instruction;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;

public class ExecUnitIn extends SimulationElement{
	Core core;
	EventQueue eventQueue;
	public ExecUnitIn(Core core) {
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		this.eventQueue = core.getEventQueue();
		// TODO Auto-generated constructor stub
	}
	
	public void execute(){
		Instruction ins = core.getExecutionEngineIn().getIdExLatch().getInstruction();
		StageLatch exMemLatch = core.getExecutionEngineIn().getExMemLatch();
		StageLatch idExLatch = core.getExecutionEngineIn().getIdExLatch();
//		if(idExLatch.getStallCount()>0){
			if(ins!=null){
				exMemLatch.setInstruction(ins);
				exMemLatch.setIn1(idExLatch.getIn1());
				exMemLatch.setIn2(idExLatch.getIn2());
				exMemLatch.setOut1(idExLatch.getOut1());
				exMemLatch.setOperationType(idExLatch.getOperationType());
			}
//		}
//		else{
//			idExLatch.decrementStallCount();
//			exMemLatch.incrementStallCount();
//		}
			if(idExLatch.getOperationType()==OperationType.load){
				exMemLatch.setMemDone(false);
				//Schedule a mem read event now so that it can be completed in the mem stage
				//TODO this.getPort() ?? Is this correct ??
				this.core.getExecutionEngineIn().getCoreMemorySystem().getL1Cache().getPort().put(
						new AddressCarryingEvent(
								this.eventQueue,
								this.core.getExecutionEngineIn().getCoreMemorySystem().getL1Cache().getLatencyDelay(),
								core.getExecutionEngineIn().getMemUnitIn(),
								core.getExecutionEngineIn().getCoreMemorySystem().getL1Cache(),//TODO FIXME 
								RequestType.Cache_Read,
								ins.getSourceOperand1().getValue()));
				
			}
			else if(idExLatch.getOperationType()==OperationType.store){
				exMemLatch.setMemDone(false);
				//Schedule a mem read event now so that it can be completed in the mem stage
				//TODO this.getPort() ?? Is this correct ??
				this.core.getExecutionEngineIn().getCoreMemorySystem().getL1Cache().getPort().put(
						new AddressCarryingEvent(
								this.eventQueue,
								this.core.getExecutionEngineIn().getCoreMemorySystem().getL1Cache().getLatencyDelay(),
								core.getExecutionEngineIn().getMemUnitIn(),
								core.getExecutionEngineIn().getCoreMemorySystem().getL1Cache(),//TODO FIXME 
								RequestType.Cache_Write,
								ins.getSourceOperand1().getValue()));
				
			}
			else{
				exMemLatch.setMemDone(true);
			}
					
	}



	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}
}
