package pipeline.inorder;

import pipeline.outoforder.OpTypeToFUTypeMapping;
import config.SimulationConfig;
import memorysystem.CoreMemorySystem;
import memorysystem.MemorySystem;
import memorysystem.TLB;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.FunctionalUnitType;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;

public class ExecUnitIn extends SimulationElement{
	Core core;
	EventQueue eventQueue;
	InorderExecutionEngine containingExecutionEngine;
	
	public ExecUnitIn(Core core, InorderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		this.eventQueue = core.getEventQueue();
		containingExecutionEngine = execEngine;
	}
	
	public void execute(InorderPipeline inorderPipeline)
	{
		StageLatch exMemLatch = inorderPipeline.getExMemLatch();
		StageLatch idExLatch = inorderPipeline.getIdExLatch();
		StageLatch ifIdLatch = inorderPipeline.getIfIdLatch();
		Instruction ins = idExLatch.getInstruction();
		
		if(idExLatch.getStallCount()>0)
		{
			idExLatch.decrementStallCount(1);
			ifIdLatch.setStallCount(1);
			return;
		}
		else
		{
			if(ins!=null)
			{
				long FURequest = 0;	//will be <= 0 if an FU was obtained
				//will be > 0 otherwise, indicating how long before
				//	an FU of the type will be available (not used in new arch)
				long lat=0;
				long currentTime=GlobalClock.getCurrentTime();
				if(OpTypeToFUTypeMapping.getFUType(ins.getOperationType())!=FunctionalUnitType.memory
						&& OpTypeToFUTypeMapping.getFUType(ins.getOperationType())!=FunctionalUnitType.inValid)
				{
								
					FURequest = containingExecutionEngine.getFunctionalUnitSet().requestFU(
						OpTypeToFUTypeMapping.getFUType(ins.getOperationType()),
						currentTime,
						core.getStepSize() );
				
					lat = containingExecutionEngine.getFunctionalUnitSet().getFULatency(
							OpTypeToFUTypeMapping.getFUType(ins.getOperationType()));
					
					if(FURequest >0)
					{
						//FU is not available
						//Set the appropriate pipelines to stall due to execute for FURequest/core.getStepSize() number of cycles!!
						//Execute for this pipeline will be stalled for timeWhenAvailable-1 cycles where as for others
						//it will be stalled for timeWhenAvailable cycles
						//Also stall the decode of this pipeline for one cycle 
						//(later cycle stalls will eventually propagate from execute stage itself)
						int delayCycles = (int)(FURequest-currentTime)/core.getStepSize();
						containingExecutionEngine.setStallFetch(delayCycles);
						containingExecutionEngine.setStallPipelinesExecute(inorderPipeline.getId(), delayCycles);
						idExLatch.setStallCount(delayCycles-1);
						ifIdLatch.setStallCount(1);
						return;
					}
					
					if(lat>1)
					{
						//If it is a multicycle operation, stall appropriate pipelines for rest of the cycles
						containingExecutionEngine.setStallFetch((int)lat-1);
						containingExecutionEngine.setStallPipelinesExecute(inorderPipeline.getId(), (int)lat-1);
						//Also stall the decode of this pipeline
						ifIdLatch.setStallCount((int)lat-1);
					}
				}
				
				boolean memReqIssued = true;	//if corememsys' mshr is full, issue not possible; pipeline's preceding stages must stall
				if(ins.getOperationType()==OperationType.load)
				{
					//containingExecutionEngine.updateNoOfLd(1);
					//containingExecutionEngine.updateNoOfMemRequests(1);
									
					//Schedule a mem read event now so that it can be completed in the mem stage
					if(!SimulationConfig.detachMemSys)
					{		
						memReqIssued = containingExecutionEngine.inorderCoreMemorySystem.issueRequestToL1Cache(
								RequestType.Cache_Read,
								ins.getSourceOperand1().getValue(),
								inorderPipeline);
					}
				}
				else if(ins.getOperationType()==OperationType.store)
				{
					//containingExecutionEngine.updateNoOfSt(1);
					//containingExecutionEngine.updateNoOfMemRequests(1);
					exMemLatch.setMemDone(true); //FIXME Pipeline doesn't wait for the store to complete! 
					
					//Schedule a mem read event now so that it can be completed in the mem stage
					if(!SimulationConfig.detachMemSys)
					{
						memReqIssued = containingExecutionEngine.inorderCoreMemorySystem.issueRequestToL1Cache(
								RequestType.Cache_Write,
								ins.getSourceOperand1().getValue(),
								inorderPipeline);
					}
				}
				else
				{
					exMemLatch.setMemDone(true);
				}
				
				if(ins.getOperationType()==OperationType.floatDiv ||
						ins.getOperationType()==OperationType.floatMul ||
						ins.getOperationType()==OperationType.floatALU ||
						ins.getOperationType()==OperationType.integerALU ||
						ins.getOperationType()==OperationType.integerDiv ||
						ins.getOperationType()==OperationType.integerMul) 
				{
					containingExecutionEngine.getDestRegisters().remove(ins.getDestinationOperand());
				}	
				
				if(memReqIssued)
				{
					exMemLatch.setInstruction(ins);
					exMemLatch.setIn1(idExLatch.getIn1());
					exMemLatch.setIn2(idExLatch.getIn2());
					exMemLatch.setOut1(idExLatch.getOut1());
					exMemLatch.setOperationType(idExLatch.getOperationType());
					exMemLatch.setLoadFlag(idExLatch.getLoadFlag());
					
					if(ins.getOperationType() == OperationType.load)
					{
						exMemLatch.setMemDone(false);
						containingExecutionEngine.noOfOutstandingLoads++;
						containingExecutionEngine.updateNoOfLd(1);
						containingExecutionEngine.updateNoOfMemRequests(1);
					}
					else if(ins.getOperationType() == OperationType.store)
					{
						containingExecutionEngine.updateNoOfSt(1);
						containingExecutionEngine.updateNoOfMemRequests(1);
					}
					idExLatch.clear();
				}
				else
				{
					ifIdLatch.incrementStallCount(1);
					containingExecutionEngine.setStallFetch(1);
				}

			}
		}
	}
	
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {

		
	}
}
