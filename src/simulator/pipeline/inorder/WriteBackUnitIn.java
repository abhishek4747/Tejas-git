package pipeline.inorder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class WriteBackUnitIn extends SimulationElement{
	Core core;
	public WriteBackUnitIn(Core core) {
		super(PortType.Unlimited, core.getNoOfRegFilePorts(), -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		// TODO Auto-generated constructor stub
	}
	
	public void performWriteBack(){
		StageLatch memWbLatch = core.getExecutionEngineIn().getMemWbLatch();
		if(memWbLatch.getInstruction()!=null){ 
			if(memWbLatch.getInstruction().getOperationType()==OperationType.inValid)
				core.getExecutionEngineIn().setExecutionComplete(true);
			else 
				core.incrementNoOfInstructionsExecuted();			
		}
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}
}
