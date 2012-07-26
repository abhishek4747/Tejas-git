package pipeline.inorder;

import java.util.Hashtable;

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
		// TODO Auto-generated constructor stub
	}
	
	public void performMemEvent(InorderPipeline inorderPipeline){
		StageLatch memWbLatch = inorderPipeline.getMemWbLatch();
		StageLatch exMemLatch = inorderPipeline.getExMemLatch();
		StageLatch idExLatch = inorderPipeline.getIdExLatch();
		Instruction ins = exMemLatch.getInstruction();
		if(exMemLatch.getStallCount()>0){
			exMemLatch.decrementStallCount(1);
			idExLatch.setStallCount(1);
			return;
		}
		if(!exMemLatch.getMemDone() || !missStatusHoldingRegister.isEmpty()){
			/*if(!missStatusHoldingRegister.isEmpty()){
				this.core.getExecutionEngineIn().getFetchUnitIn().incrementStallLowerMSHRFull(1);
				System.err.println("Stalling pipeline coz MSHR of the next level can't take any more requests");
				return;
			}*/
			this.core.getExecutionEngineIn().incrementMemStall(1);
//			core.getExecutionEngineIn().getFetchUnitIn().setStall(1);
			//Stall appropriate pipelines and stall fetch
			core.getExecutionEngineIn().setStallFetch(1);
			core.getExecutionEngineIn().setStallPipelinesMem(inorderPipeline.getId(), 1);
			//Also stall the exec of this pipeline
			idExLatch.setStallCount(1);
			return;
/*			if(exMemLatch.getStallCount()>200){
				exMemLatch.setMemDone(true);		//FIXME
				exMemLatch.setStallCount(0);
			}*/
			
//			memWbLatch.setInstruction(null);
//System.out.println("Memory Stall!");
		}
		else{
			if(ins!=null){
//System.out.println("Mem "+ins.getSerialNo());			
				if(ins.getOperationType()==OperationType.load)
					this.core.getExecutionEngineIn().getDestRegisters().remove(ins.getDestinationOperand());
				memWbLatch.setInstruction(ins);
				memWbLatch.setIn1(exMemLatch.getIn1());
				memWbLatch.setIn2(exMemLatch.getIn2());
				memWbLatch.setOut1(exMemLatch.getOut1());
				memWbLatch.setOperationType(exMemLatch.getOperationType());
				exMemLatch.clear();
				
			}
//			else{
////				memWbLatch.setInstruction(null);
//			}
		}
	}


	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
//		core.getExecutionEngineIn().getFetchUnitIn().setStall(0);
//System.out.println("handling mem");
		core.getExecutionEngineIn().setMemDone(((AddressCarryingEvent)event).getAddress(),true);
//		core.getExecutionEngineIn().getExMemLatch(0).setMemDone(true);
	}
}
