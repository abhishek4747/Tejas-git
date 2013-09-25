package pipeline.multi_issue_inorder;

import java.util.ArrayList;

import config.SimulationConfig;

import pipeline.outoforder.OpTypeToFUTypeMapping;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.FunctionalUnitType;
import generic.GlobalClock;
import generic.Instruction;
import generic.Operand;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class DecodeUnit_MII extends SimulationElement{
	
	Core core;
	MultiIssueInorderExecutionEngine containingExecutionEngine;
	StageLatch_MII ifIdLatch;
	StageLatch_MII idExLatch; 
	
	long numBranches;
	long numMispredictedBranches;
	long lastValidIPSeen;
	
	long instCtr; //for debug
	
	public DecodeUnit_MII(Core core, MultiIssueInorderExecutionEngine execEngine)
	{
		/*
		 * numPorts and occupancy = -1 => infinite ports 
		 * Latency = 1 . 
		 * 
		*/
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		containingExecutionEngine = execEngine;
		ifIdLatch = execEngine.getIfIdLatch();
		idExLatch = execEngine.getIdExLatch();
		
		numBranches = 0;
		numMispredictedBranches = 0;
		lastValidIPSeen = -1;
		
		instCtr = 0;
	}

	
	public void performDecode(MultiIssueInorderPipeline inorderPipeline){
		
		Instruction ins = null;
				
		while(ifIdLatch.isEmpty() == false
				&& idExLatch.isFull() == false)
		{
			ins = ifIdLatch.peek(0);
			OperationType opType;
				
			if(ins!=null)
			{
				opType = ins.getOperationType();
				
				if(checkDataHazard(ins))	//Data Hazard Detected,Stall Pipeline
				{
					containingExecutionEngine.incrementDataHazardStall(1);
					break;
				}
				
				//check for structural hazards
				long FURequest = 0;
				if(OpTypeToFUTypeMapping.getFUType(ins.getOperationType()) != FunctionalUnitType.inValid
						&& OpTypeToFUTypeMapping.getFUType(ins.getOperationType()) != FunctionalUnitType.memory)
				{
					FURequest = containingExecutionEngine.getFunctionalUnitSet().requestFU(
							OpTypeToFUTypeMapping.getFUType(ins.getOperationType()),
							GlobalClock.getCurrentTime() + 1,
							core.getStepSize() );
				
					if(FURequest > 0)
					{
						break;
					}
				}
				
				//increment power counters
   				this.core.powerCounters.incrementWindowSelectionAccess(1);
   				
				if(opType==OperationType.load || opType==OperationType.store)
				{
					this.core.powerCounters.incrementLsqWakeupAccess(1);
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
			  
				//add destination register of ins to list of outstanding registers
				if(ins.getDestinationOperand() != null)
				{
					if(ins.getOperationType() == OperationType.load)
					{
						addToValueReadyArray(ins.getDestinationOperand(), Long.MAX_VALUE);
					}
					else if(ins.getOperationType() == OperationType.mov)
					{
						addToValueReadyArray(ins.getDestinationOperand(), GlobalClock.getCurrentTime() + 1);
					}
					else
					{
						addToValueReadyArray(ins.getDestinationOperand(),
											GlobalClock.getCurrentTime()
												+ containingExecutionEngine.getFunctionalUnitSet().getFULatency(
														OpTypeToFUTypeMapping.getFUType(ins.getOperationType())));
					}
				}
				
				if(ins.getOperationType() == OperationType.xchg)
				{
					addToValueReadyArray(ins.getSourceOperand1(), GlobalClock.getCurrentTime() + 1);
					if(ins.getSourceOperand1().getValue() != ins.getSourceOperand2().getValue()
							|| ins.getSourceOperand1().getOperandType() != ins.getSourceOperand2().getOperandType())
					{
						addToValueReadyArray(ins.getSourceOperand2(), GlobalClock.getCurrentTime() + 1);
					}
				}
				
				//update last valid IP seen
				if(ins.getCISCProgramCounter() != -1)
				{
					lastValidIPSeen = ins.getCISCProgramCounter();
				}
				
				//perform branch prediction
				if(ins.getOperationType()==OperationType.branch)
				{
					boolean prediction = core.getBranchPredictor().predict(
																		lastValidIPSeen,
																		ins.isBranchTaken());
					if(prediction != ins.isBranchTaken())
					{
						//Branch mispredicted
						//stall pipeline for appropriate cycles
						containingExecutionEngine.setStallFetch(core.getBranchMispredictionPenalty());
						numMispredictedBranches++;
						this.core.powerCounters.incrementBpredMisses();
					}
					this.core.powerCounters.incrementBpredAccess(1);
	
					//Train Branch Predictor
					core.getBranchPredictor().Train(
							ins.getCISCProgramCounter(),
							ins.isBranchTaken(),
							prediction
							);
					this.core.powerCounters.incrementBpredAccess(1);
					
					numBranches++;
				}
				
				if(ins.getSerialNo() != instCtr && ins.getOperationType() != OperationType.inValid)
				{
					misc.Error.showErrorAndExit("decode out of order!!");
				}
				instCtr++;
				
				//move ins to next stage
				idExLatch.add(ins, GlobalClock.getCurrentTime() + 1);
				ifIdLatch.poll();
				
				if(SimulationConfig.debugMode)
				{
					System.out.println("decoded : " + GlobalClock.getCurrentTime()/core.getStepSize() + "\n"  + ins + "\n");
				}
				
				//if a branch/jump instruction is issued, no more instructions to be issued this cycle
				if(opType == OperationType.branch
						|| opType == OperationType.jump)
				{
					break;
				}
			}
			else
			{
				break;
			}
		}
	}

	private boolean checkDataHazard(Instruction ins)
	{
		Operand srcOpnd;
		
		//operand 1
		srcOpnd = ins.getSourceOperand1();
		if(srcOpnd != null)
		{
			if(srcOpnd.isIntegerRegisterOperand())
			{
				if(containingExecutionEngine.getValueReadyInteger()[(int)(srcOpnd.getValue())]
																			> GlobalClock.getCurrentTime())
				{
					return true;
				}
			}

			else if(srcOpnd.isFloatRegisterOperand())
			{
				if(containingExecutionEngine.getValueReadyFloat()[(int)(srcOpnd.getValue())]
																			> GlobalClock.getCurrentTime())
				{
					return true;
				}
			}

			else if(srcOpnd.isMachineSpecificRegisterOperand())
			{
				if(containingExecutionEngine.getValueReadyMSR()[(int)(srcOpnd.getValue())]
																			> GlobalClock.getCurrentTime())
				{
					return true;
				}
			}
		}
		
		//operand 2
		srcOpnd = ins.getSourceOperand2();
		if(srcOpnd != null)
		{
			if(srcOpnd.isIntegerRegisterOperand())
			{
				if(containingExecutionEngine.getValueReadyInteger()[(int)(srcOpnd.getValue())]
																			> GlobalClock.getCurrentTime())
				{
					return true;
				}
			}

			else if(srcOpnd.isFloatRegisterOperand())
			{
				if(containingExecutionEngine.getValueReadyFloat()[(int)(srcOpnd.getValue())]
																			> GlobalClock.getCurrentTime())
				{
					return true;
				}
			}

			else if(srcOpnd.isMachineSpecificRegisterOperand())
			{
				if(containingExecutionEngine.getValueReadyMSR()[(int)(srcOpnd.getValue())]
																			> GlobalClock.getCurrentTime())
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	private void addToValueReadyArray(Operand destOpnd, long timeWhenValueReady)
	{
		if(destOpnd.isIntegerRegisterOperand())
		{
			containingExecutionEngine.getValueReadyInteger()[(int)(destOpnd.getValue())]
																		 = timeWhenValueReady;
		}

		else if(destOpnd.isFloatRegisterOperand())
		{
			containingExecutionEngine.getValueReadyFloat()[(int)(destOpnd.getValue())]
																		 = timeWhenValueReady;
		}

		else if(destOpnd.isMachineSpecificRegisterOperand())
		{
			containingExecutionEngine.getValueReadyMSR()[(int)(destOpnd.getValue())]
																		 = timeWhenValueReady;
		}
	}


	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
	}

	public long getNumBranches() {
		return numBranches;
	}

	public long getNumMispredictedBranches() {
		return numMispredictedBranches;
	}
}
