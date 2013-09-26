package pipeline.multi_issue_inorder;

import generic.Instruction;

import config.SimulationConfig;
import main.CustomObjectPool;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;
import generic.Statistics;

public class WriteBackUnitIn_MII extends SimulationElement{
	
	Core core;
	MultiIssueInorderExecutionEngine containingExecutionEngine;
	StageLatch_MII memWbLatch;
	
	long instCtr; //for debug
	
	public WriteBackUnitIn_MII(Core core, MultiIssueInorderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, core.getNoOfRegFilePorts(), -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		containingExecutionEngine = execEngine;
		memWbLatch = execEngine.getMemWbLatch();
		
		instCtr = 0;
	}
	
	public void performWriteBack(MultiIssueInorderPipeline inorderPipeline)
	{
		if(containingExecutionEngine.getMispredStall() > 0)
		{
			return;
		}
		
		Instruction ins = null;
		
		while(memWbLatch.isEmpty() == false)
		{
			ins = memWbLatch.peek(0);
			if(ins != null)
			{
				OperationType opType = ins.getOperationType(); 
			
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
			
				//check if simulation complete
				if(ins.getOperationType()==OperationType.inValid)
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
				}
				else
				{
					//issue store
					if(ins.getOperationType() == OperationType.store)
					{
						if(!SimulationConfig.detachMemSys)
						{
							//System.out.println(" store issue at time  "+ GlobalClock.getCurrentTime() +" for address " + ins.getSourceOperand1().getValue());
							boolean memReqIssued = containingExecutionEngine.multiIssueInorderCoreMemorySystem.issueRequestToL1Cache(
									RequestType.Cache_Write,
									ins.getSourceOperand1MemValue());
							
							if(memReqIssued == false)
							{
								break;
							}
						}
					}
					
					if(core.getNoOfInstructionsExecuted()%1000000==0)
					{
						System.out.println(core.getNoOfInstructionsExecuted()/1000000 + " million done" + " by core "+core.getCore_number() 
								+ " global clock cycle " + GlobalClock.getCurrentTime());
					}
					core.incrementNoOfInstructionsExecuted();
				}
				
				if(ins.getSerialNo() != instCtr && ins.getOperationType() != OperationType.inValid)
				{
					misc.Error.showErrorAndExit("wb out of order!!");
				}
				instCtr++;	
				
				if(SimulationConfig.debugMode)
				{
					System.out.println("write back : " + GlobalClock.getCurrentTime()/core.getStepSize() + "\n"  + ins + "\n");
				}
				
				memWbLatch.poll();				
				try
				{
					CustomObjectPool.getInstructionPool().returnObject(ins);
					core.numReturns++;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			else
			{
				break;
			}
		}
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {

	}
}
