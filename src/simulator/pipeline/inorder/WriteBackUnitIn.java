package pipeline.inorder;

import config.SimulationConfig;
import main.CustomObjectPool;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;
import generic.Statistics;

public class WriteBackUnitIn extends SimulationElement{
	
	Core core;
	InorderExecutionEngine containingExecutionEngine;
	int j;
	
	public WriteBackUnitIn(Core core, InorderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, core.getNoOfRegFilePorts(), -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		this.j=0;
		containingExecutionEngine = execEngine;
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
				this.core.currentThreads--;
				
				if(this.core.currentThreads == 0){   //set exec complete only if there are n other thread already 
													  //assigned to this pipeline	
					containingExecutionEngine.setExecutionComplete(true);
					if(SimulationConfig.pinpointsSimulation == false)
					{					
						containingExecutionEngine.setTimingStatistics();			
						containingExecutionEngine.setPerCoreMemorySystemStatistics();
						containingExecutionEngine.setPerCorePowerStatistics();
					}
					else
					{
						Statistics.processEndOfSlice();
					}
				}
				memWbLatch.clear();
			}
			else
			{
				if(core.getNoOfInstructionsExecuted()%1000000==0)
				{
					System.out.println(this.j++ + " million done" + " by core "+core.getCore_number() 
							+ " global clock cycle " + GlobalClock.getCurrentTime());
				}
				core.incrementNoOfInstructionsExecuted();
				
				try
				{
					CustomObjectPool.getInstructionPool().returnObject(memWbLatch.getInstruction());
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
