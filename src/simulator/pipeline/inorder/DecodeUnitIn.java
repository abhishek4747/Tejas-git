package pipeline.inorder;

import pipeline.outoforder.MispredictionPenaltyCompleteEvent;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.Operand;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class DecodeUnitIn extends SimulationElement{
	Core core;
	public DecodeUnitIn(Core core) {
		/*
		 * numPorts and occupancy = -1 => infinite ports 
		 * Latency = 1 . 
		 * TODO - take it from core.*/
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
	}

	public void performDecode(){
		Instruction ins;
		StageLatch ifIdLatch = this.core.getExecutionEngineIn().getIfIdLatch();
		StageLatch idExLatch = this.core.getExecutionEngineIn().getIdExLatch(); 

		ins = ifIdLatch.getInstruction();
		if(ins!=null){
			
			//	stall pipeline if data hazard detected
			//if ifIdLatch has stall count > 0 then it means there was a stall introduced in the last cycle,
			//so no need to introduce fresh stall
//			if(ifIdLatch.getStallCount()==0 && checkDataHazard(ins,idExLatch.getOut1()) && idExLatch.getLoadFlag()){
////				idExLatch.incrementStallCount();
//				core.getFetchUnitIn().setStall(true);
//				//Set stall complete event to continue the pipeline
//				core.getEventQueue().addEvent(
//				new MispredictionPenaltyCompleteEventIn(
//						GlobalClock.getCurrentTime() + core.getBranchMispredictionPenalty()*core.getStepSize(),
//						core)
//				);
//			}
//			else{
//				ifIdLatch.decrementStallCount();
//			}
			if(checkDataHazard(ins,idExLatch.getOut1()) && idExLatch.getLoadFlag()){
				core.getExecutionEngineIn().getFetchUnitIn().incrementStall(1);
				idExLatch.setInstruction(null);
				idExLatch.setIn1(null);
				idExLatch.setIn2(null);			
				idExLatch.setOut1(null);
				idExLatch.setOperationType(null);
			}
			else{
				System.out.println("Decode");
				idExLatch.setInstruction(ins);
				idExLatch.setIn1(ins.getSourceOperand1());
				idExLatch.setIn2(ins.getSourceOperand2());			
				idExLatch.setOut1(ins.getDestinationOperand());
				idExLatch.setOperationType(ins.getOperationType());
			}
			if(ins.getOperationType()==OperationType.branch){ 
				core.getBranchPredictor().Train(
						ins.getProgramCounter(),
						ins.isBranchTaken(),
						core.getBranchPredictor().predict(ins.getProgramCounter())
						);
				if(core.getBranchPredictor().predict(ins.getProgramCounter()) != ins.isBranchTaken()){
				//Branch mis predicted
				//stall pipeline for appropriate cycles
				//TODO correct the following:
//				core.getExecutionEngineIn().getFetchUnitIn().incrementStall(core.getBranchMispredictionPenalty());
					core.getExecutionEngineIn().getFetchUnitIn().incrementStall(2);
				//Set stall complete event to continue the pipeline
//				core.getEventQueue().addEvent(
//				new MispredictionPenaltyCompleteEventIn(
//						GlobalClock.getCurrentTime() + core.getBranchMispredictionPenalty()*core.getStepSize(),
//						core)
//				);
				}
			}
		}
		else{
			idExLatch.setInstruction(null);
		}
		
	}
	private boolean checkDataHazard(Instruction ins, Operand destOp){
		if(destOp!=null){
			if(destOp.equals(ins.getDestinationOperand()))
				return true;
			else 
				return false;
		}
		else
			return false;
	}


	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}
}
