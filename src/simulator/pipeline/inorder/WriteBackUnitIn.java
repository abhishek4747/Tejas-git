package pipeline.inorder;

import emulatorinterface.Newmain;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class WriteBackUnitIn extends SimulationElement{
	
	Core core;
	int j;
	
	public WriteBackUnitIn(Core core)
	{
		super(PortType.Unlimited, core.getNoOfRegFilePorts(), -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		this.j=0;
	}
	
	public void performWriteBack(InorderPipeline inorderPipeline)
	{
		StageLatch memWbLatch = inorderPipeline.getMemWbLatch();
		if(memWbLatch.getInstruction()!=null)
		{ 
			OperationType opType = memWbLatch.getInstruction().getOperationType(); 
			
			if(!(opType==OperationType.branch || opType==OperationType.jump)){
				this.core.powerCounters.incrementWindowAccess(1);
				this.core.powerCounters.incrementWindowPregAccess(1);
				this.core.powerCounters.incrementWindowWakeupAccess(1);
				this.core.powerCounters.incrementResultbusAccess(1);
			}
			
			if(!(opType==OperationType.store || opType == OperationType.branch 
					|| opType == OperationType.nop || opType == OperationType.jump))
			{
				this.core.powerCounters.incrementRegfileAccess(1);
			}
			
				
			if(memWbLatch.getInstruction().getOperationType()==OperationType.inValid)
			{
				//FIXME the following does not set the statistics. Check!
				core.getExecutionEngineIn().setExecutionComplete(true);
				this.core.getExecutionEngineIn().setTimingStatistics();			
				this.core.getExecutionEngineIn().setPerCoreMemorySystemStatistics();
				this.core.getExecutionEngineIn().setPerCorePowerStatistics();
				
			}
			else
			{
				if(core.getNoOfInstructionsExecuted()%1000000==0)
				{
					System.out.println(this.j++ + " million done");
				}
				core.incrementNoOfInstructionsExecuted();
				
				try
				{
					Newmain.instructionPool.returnObject(memWbLatch.getInstruction());
					core.numReturns++;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				
				memWbLatch.clear();
			}
		}
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {

	}
}
