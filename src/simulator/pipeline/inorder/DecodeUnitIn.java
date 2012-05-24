package pipeline.inorder;

import java.util.Hashtable;

import pipeline.outoforder.MispredictionPenaltyCompleteEvent;
import power.Counters;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.OMREntry;
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
		if(idExLatch.getStallCount()==0 && ifIdLatch.getMemDone()){
			if(ins!=null){
	//System.out.println("Decode "+ins.getSerialNo());			
				if(checkDataHazard(ins,idExLatch.getOut1()) && idExLatch.getLoadFlag()){
					core.getExecutionEngineIn().getFetchUnitIn().incrementStall(1);
	//System.out.println("Data Hazard!");
				}
	//			else{

   				OperationType opType = ins.getOperationType();
   				this.core.powerCounters.incrementWindowSelectionAccess(1);
				if(opType==OperationType.load || opType==OperationType.store){
//System.out.println("load instruction");
					this.core.powerCounters.incrementLsqWakeupAccess(1);	//FIXME lsq stats for inorder ?!
					this.core.powerCounters.incrementLsqAccess(1);
					this.core.powerCounters.incrementLsqStoreDataAccess(1);
					this.core.powerCounters.incrementLsqPregAccess(1);
				}
				else if(opType==OperationType.floatALU || opType==OperationType.floatDiv || opType==OperationType.floatMul){
//System.out.println("float alu instruction");
					this.core.powerCounters.incrementAluAccess(1);
					this.core.powerCounters.incrementFaluAccess(1);
				}
				else if(opType==OperationType.integerALU || opType==OperationType.integerDiv || opType==OperationType.integerMul){
//System.out.println("int alu instruction");
					this.core.powerCounters.incrementAluAccess(1);
					this.core.powerCounters.incrementIaluAccess(1);
				}
				
				this.core.powerCounters.incrementWindowAccess(1);
				this.core.powerCounters.incrementWindowPregAccess(1);
			  
				this.core.powerCounters.incrementRegfileAccess(1);
			  
					idExLatch.setInstruction(ins);
					idExLatch.setIn1(ins.getSourceOperand1());
					idExLatch.setIn2(ins.getSourceOperand2());			
					idExLatch.setOut1(ins.getDestinationOperand());
					idExLatch.setOperationType(ins.getOperationType());
					
					ifIdLatch.clear();
				
					if(ins.getOperationType()==OperationType.branch){ 
								this.core.powerCounters.incrementBpredAccess(1);

								core.getBranchPredictor().Train(
								ins.getRISCProgramCounter(),
								ins.isBranchTaken(),
								core.getBranchPredictor().predict(ins.getRISCProgramCounter())
								);
						if(core.getBranchPredictor().predict(ins.getRISCProgramCounter()) != ins.isBranchTaken()){
						//Branch mis predicted
						//stall pipeline for appropriate cycles
						//TODO correct the following:
		//				core.getExecutionEngineIn().getFetchUnitIn().incrementStall(core.getBranchMispredictionPenalty());
							core.getExecutionEngineIn().getFetchUnitIn().incrementStall(2);
						}
					}
	//			}
			}
			else{
	//			idExLatch.setInstruction(null);
			}
		}			

		if(idExLatch.getStallCount()>0){
			idExLatch.decrementStallCount();
			ifIdLatch.incrementStallCount();
		}
	}

	private boolean checkDataHazard(Instruction ins, Operand destOp){
		if(destOp!=null){
			//TODO check the following way of comparing the two operands
			if(destOp.equals(ins.getSourceOperand1()) || destOp.equals(ins.getSourceOperand1()))
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
