package pipeline.inorder;

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
		StageLatch exMemLatch = core.getExecutionEngineIn().getExMemLatch();
		StageLatch idExLatch = core.getExecutionEngineIn().getIdExLatch();
		Instruction ins = idExLatch.getInstruction();
		if(ins!=null){
				//TODO Account for multicycle operations.
				exMemLatch.setInstruction(ins);
				exMemLatch.setIn1(idExLatch.getIn1());
				exMemLatch.setIn2(idExLatch.getIn2());
				exMemLatch.setOut1(idExLatch.getOut1());
				exMemLatch.setOperationType(idExLatch.getOperationType());
				
				if(idExLatch.getOperationType()==OperationType.load){
					core.getExecutionEngineIn().updateNoOfLd(1);
					core.getExecutionEngineIn().updateNoOfMemRequests(1);
					exMemLatch.setMemDone(false);
					//Schedule a mem read event now so that it can be completed in the mem stage
	
					this.core.getExecutionEngineIn().coreMemorySystem.issueRequestToL1Cache(
							core.getExecutionEngineIn().getMemUnitIn(),
							RequestType.Cache_Read,
							ins.getSourceOperand1().getValue());
				}
				else if(idExLatch.getOperationType()==OperationType.store){
					core.getExecutionEngineIn().updateNoOfSt(1);
					core.getExecutionEngineIn().updateNoOfMemRequests(1);
//					exMemLatch.setMemDone(false); /FIXME *Pipeline doesn't wait for the store to complete! */
					//Schedule a mem read event now so that it can be completed in the mem stage
	
					this.core.getExecutionEngineIn().coreMemorySystem.issueRequestToL1Cache(
							core.getExecutionEngineIn().getMemUnitIn(),
							RequestType.Cache_Write,
							ins.getSourceOperand1().getValue());
				}
				else{
					exMemLatch.setMemDone(true);
				}
			}
			else{
				exMemLatch.setInstruction(null);
			}
		}
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}
}
