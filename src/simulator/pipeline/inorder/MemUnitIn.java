package pipeline.inorder;

import java.util.Hashtable;

import emulatorinterface.Newmain;

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
	EventQueue eventQueue;
	Hashtable<Long,OMREntry> missStatusHoldingRegister;
	public Hashtable<Long, OMREntry> getMissStatusHoldingRegister() {
		return missStatusHoldingRegister;
	}

	public void setMissStatusHoldingRegister(
			Hashtable<Long, OMREntry> missStatusHoldingRegister) {
		this.missStatusHoldingRegister = missStatusHoldingRegister;
	}

	public MemUnitIn(Core core) {
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		this.eventQueue = core.getEventQueue();
		this.missStatusHoldingRegister = new Hashtable<Long,OMREntry>();
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
			this.core.getExecutionEngineIn().incrementMemStall(1);
			core.getExecutionEngineIn().setStallFetch(1);
			core.getExecutionEngineIn().setStallPipelinesMem(inorderPipeline.getId(), 1);
			idExLatch.setStallCount(1);
			return;
		}
		else
		{
			if(ins!=null)
			{
				if(ins.getOperationType()==OperationType.load)
				{
					this.core.getExecutionEngineIn().getDestRegisters().remove(ins.getDestinationOperand());
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
		core.getExecutionEngineIn().setMemDone(((AddressCarryingEvent)event).getAddress(),true);
	}
	
	public void processCompletionOfMemRequest(long requestedAddress)
	{
		core.getExecutionEngineIn().setMemDone(requestedAddress,true);
		core.getExecutionEngineIn().noOfOutstandingLoads--;
	}
}
