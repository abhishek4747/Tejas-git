package pipeline.inorder;

import generic.Core;
import generic.Event;
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
//		StageLatch wbDoneLatch = core.getInorderPipeline().getWbDoneLatch();
//		StageLatch memWbLatch = core.getInorderPipeline().getMemWbLatch();
//		if(memWbLatch.getStallCount()>0){
//			wbDoneLatch.incrementStallCount();
//			memWbLatch.decrementStallCount();
//		}
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}
}