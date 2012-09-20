package pipeline.inorder;

import java.util.Hashtable;


import main.Main;
import memorysystem.AddressCarryingEvent;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.Instruction;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;
import generic.OMREntry;

public class MemUnitIn extends SimulationElement{
	
	Core core;
	InorderExecutionEngine containingExecutionEngine;
	EventQueue eventQueue;

	public MemUnitIn(Core core, InorderExecutionEngine execEngine) {
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		this.eventQueue = core.getEventQueue();
		containingExecutionEngine = execEngine;
	}
	
	public void performMemEvent(InorderPipeline inorderPipeline)
	{
		StageLatch memWbLatch = inorderPipeline.getMemWbLatch();
		StageLatch exMemLatch = inorderPipeline.getExMemLatch();
		StageLatch idExLatch = inorderPipeline.getIdExLatch();
		Instruction ins = exMemLatch.getInstruction();
		
		if(exMemLatch.getStallCount() > 0)
		{
			exMemLatch.decrementStallCount(1);
			idExLatch.setStallCount(1);
			return;
		}
		
		if(!exMemLatch.getMemDone())
		{
			this.containingExecutionEngine.incrementMemStall(1);
			containingExecutionEngine.setStallFetch(1);
			containingExecutionEngine.setStallPipelinesMem(inorderPipeline.getId(), 1);
			idExLatch.setStallCount(1);
			return;
		}
		else
		{
			if(ins!=null)
			{
				if(ins.getOperationType()==OperationType.load)
				{
					this.containingExecutionEngine.getDestRegisters().remove(ins.getDestinationOperand());
				}
				
				memWbLatch.setInstruction(ins);
				memWbLatch.setIn1(exMemLatch.getIn1());
				memWbLatch.setIn2(exMemLatch.getIn2());
				memWbLatch.setOut1(exMemLatch.getOut1());
				memWbLatch.setOperationType(exMemLatch.getOperationType());
				exMemLatch.clear();
				
			}
		}
	}


	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		containingExecutionEngine.setMemDone(((AddressCarryingEvent)event).getAddress(),true);
	}
	
	public void processCompletionOfMemRequest(long requestedAddress)
	{
		containingExecutionEngine.setMemDone(requestedAddress,true);
		containingExecutionEngine.noOfOutstandingLoads--;
	}
}
