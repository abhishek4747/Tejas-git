package pipeline.inorder;

import java.util.ArrayList;
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
import generic.OperandType;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class DecodeUnitIn extends SimulationElement{
	
	Core core;
	InorderExecutionEngine containingExecutionEngine;
	
	public DecodeUnitIn(Core core, InorderExecutionEngine execEngine)
	{
		/*
		 * numPorts and occupancy = -1 => infinite ports 
		 * Latency = 1 . 
		 * TODO - take it from core.*/
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		containingExecutionEngine = execEngine;
	}

	
	public void performDecode(InorderPipeline inorderPipeline){
		Instruction ins;
		StageLatch ifIdLatch = inorderPipeline.getIfIdLatch();
		StageLatch idExLatch = inorderPipeline.getIdExLatch(); 
		StageLatch exMemLatch = inorderPipeline.getExMemLatch();
		ins = ifIdLatch.getInstruction();
		
		if(ifIdLatch.getStallCount()>0)
		{
			ifIdLatch.decrementStallCount(1);
			return;
		}
		if(ifIdLatch.getStallCount()==0)
		{
			if(ins!=null)
			{
				if(checkDataHazard(ins))
				{
					containingExecutionEngine.setStallFetch(1);
					containingExecutionEngine.setStallPipelinesDecode(inorderPipeline.getId(),1);
					containingExecutionEngine.incrementDataHazardStall(1);
					return;
				}
				
   				OperationType opType = ins.getOperationType();
   				this.core.powerCounters.incrementWindowSelectionAccess(1);
   				
				if(opType==OperationType.load || opType==OperationType.store)
				{
					this.core.powerCounters.incrementLsqWakeupAccess(1);	//FIXME lsq stats for inorder ?!
					this.core.powerCounters.incrementLsqAccess(1);
					this.core.powerCounters.incrementLsqStoreDataAccess(1);
					this.core.powerCounters.incrementLsqPregAccess(1);
				}
				else if(opType==OperationType.floatALU ||
						opType==OperationType.floatDiv ||
						opType==OperationType.floatMul)
				{
					this.core.powerCounters.incrementAluAccess(1);
					this.core.powerCounters.incrementFaluAccess(1);
				}
				else if(opType==OperationType.integerALU ||
						opType==OperationType.integerDiv ||
						opType==OperationType.integerMul)
				{
					this.core.powerCounters.incrementAluAccess(1);
					this.core.powerCounters.incrementIaluAccess(1);
				}
				
				this.core.powerCounters.incrementWindowAccess(1);
				this.core.powerCounters.incrementWindowPregAccess(1);
			  
				this.core.powerCounters.incrementRegfileAccess(1);
			  
				if(ins.getDestinationOperand()!=null)
				{
					if(ins.getOperationType()==OperationType.floatDiv ||
							ins.getOperationType()==OperationType.floatMul ||
							ins.getOperationType()==OperationType.floatALU ||
							ins.getOperationType()==OperationType.integerALU ||
							ins.getOperationType()==OperationType.integerDiv ||
							ins.getOperationType()==OperationType.integerMul ||
							ins.getOperationType()==OperationType.load)
					{
						containingExecutionEngine.getDestRegisters().add(ins.getDestinationOperand());
					}
				}
				
				if(ins.getOperationType()==OperationType.branch)
				{ 
					this.core.powerCounters.incrementBpredAccess(1);

					if(core.getBranchPredictor().predict(ins.getRISCProgramCounter()) != ins.isBranchTaken())
					{
						//Branch mis predicted
						//stall pipelines for appropriate cycles
						//TODO correct the following:
//										core.getExecutionEngineIn().getFetchUnitIn().incrementStall(core.getBranchMispredictionPenalty());
						this.core.powerCounters.incrementBpredMisses();
						containingExecutionEngine.setStallPipelinesDecode(inorderPipeline.getId(), core.getBranchMispredictionPenalty());
						containingExecutionEngine.setStallFetch(core.getBranchMispredictionPenalty());
					}
	
					core.getBranchPredictor().Train(
							ins.getRISCProgramCounter(),
							ins.isBranchTaken(),
							core.getBranchPredictor().predict(ins.getRISCProgramCounter())
							);
				}
				
				idExLatch.setInstruction(ins);
				idExLatch.setIn1(ins.getSourceOperand1());
				idExLatch.setIn2(ins.getSourceOperand2());			
				idExLatch.setOut1(ins.getDestinationOperand());
				idExLatch.setOperationType(ins.getOperationType());
				ifIdLatch.clear();
			}
		}
	}

	private boolean checkDataHazard(Instruction ins)
	{
		ArrayList<Operand> destRegisters = containingExecutionEngine.getDestRegisters();
		for(Operand e: destRegisters)
		{
			if(ins.getSourceOperand1()!=null &&
					e.getOperandType()==ins.getSourceOperand1().getOperandType() 
					&& e.getValue() == ins.getSourceOperand1().getValue())
			{
				return true;
			}
			if(ins.getSourceOperand2()!=null &&
					e.getOperandType()==ins.getSourceOperand2().getOperandType() 
					&& e.getValue() == ins.getSourceOperand2().getValue())
			{
				return true;
			}
		}
		return false;
	}


	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
	}
}
