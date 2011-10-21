package pipeline.inorder;


import generic.Core;
import generic.Event;
import generic.Instruction;
import generic.PortType;
import generic.SimulationElement;

public class ExecUnitIn extends SimulationElement{
	Core core;
	public ExecUnitIn(Core core) {
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		// TODO Auto-generated constructor stub
	}
	
	public void execute(){
		Instruction ins = core.getInorderPipeline().getIdExLatch().getInstruction();
		StageLatch exMemLatch = core.getInorderPipeline().getExMemLatch();
		StageLatch idExLatch = core.getInorderPipeline().getIdExLatch();
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
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}
}
