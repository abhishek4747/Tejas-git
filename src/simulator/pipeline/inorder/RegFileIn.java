package pipeline.inorder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class RegFileIn extends SimulationElement{
	Core core;
	public RegFileIn(Core core) {
		super(PortType.Unlimited, core.getNoOfRegFilePorts(), -1 ,core.getEventQueue(), -1, -1);

		this.core = core;
		// TODO Auto-generated constructor stub
	}
	public void fetchOperands(InorderPipeline inorderPipeline){
		//TODO check for load hazard
		StageLatch idExLatch = inorderPipeline.getIdExLatch();
		if(idExLatch.getOperationType()==OperationType.load){
			//set the load flag in the idexlatch so that next time when 
			//decode unit checks for data hazards it stalls the pipeline appropriately
			idExLatch.setLoadFlag(true);
		}
		else
			idExLatch.setLoadFlag(false);
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}
	 
	
}
