package pipeline.multi_issue_inorder;

import config.SimulationConfig;
import memorysystem.AddressCarryingEvent;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;

public class MemUnitIn_MII extends SimulationElement{
	
	Core core;
	MultiIssueInorderExecutionEngine containingExecutionEngine;
	EventQueue eventQueue;
	StageLatch_MII exMemLatch;
	StageLatch_MII memWbLatch;
	
	long instCtr; //for debug

	public MemUnitIn_MII(Core core, MultiIssueInorderExecutionEngine execEngine) {
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		this.eventQueue = core.getEventQueue();
		containingExecutionEngine = execEngine;
		exMemLatch = execEngine.getExMemLatch();
		memWbLatch = execEngine.getMemWbLatch();
		
		instCtr = 0;
	}
	
	public void performMemEvent(MultiIssueInorderPipeline inorderPipeline)
	{
		Instruction ins = null;
		
		while(exMemLatch.isEmpty() == false
				&& memWbLatch.isFull() == false)
		{
			ins = exMemLatch.peek(0);
			
			if(ins != null)
			{
				long lat = 1;
				if(ins.getOperationType() == OperationType.load)
				{
					//issue load request
					if(!SimulationConfig.detachMemSys)
					{		
						boolean memReqIssued = containingExecutionEngine.multiIssueInorderCoreMemorySystem.issueRequestToL1Cache(
								RequestType.Cache_Read,
								ins.getSourceOperand1MemValue());
						
						if(memReqIssued == false)
						{
							break;
						}
					}
					
					//set instruction's MEM stage completion time to Long.MAX_VALUE
					lat = Long.MAX_VALUE - GlobalClock.getCurrentTime();
				}
				else
				{
					//remove from list of outstanding registers
					if(ins.getDestinationOperand() != null)
					{
						this.containingExecutionEngine.getDestRegisters().remove(ins.getDestinationOperand());
					}
					
					if(ins.getOperationType() == OperationType.xchg)
					{
						this.containingExecutionEngine.getDestRegisters().remove(ins.getSourceOperand1());
						if(ins.getSourceOperand1().getOperandType() != ins.getSourceOperand2().getOperandType()
								|| ins.getSourceOperand1().getValue() != ins.getSourceOperand2().getValue())
						{
							this.containingExecutionEngine.getDestRegisters().remove(ins.getSourceOperand2());
						}
					}
				}
				
				if(ins.getSerialNo() != instCtr && ins.getOperationType() != OperationType.inValid)
				{
					misc.Error.showErrorAndExit("mem out of order!!");
				}
				instCtr++;
				
				//move ins to next stage
				memWbLatch.add(ins, GlobalClock.getCurrentTime() + lat);
				exMemLatch.poll();
			}
			else
			{
				break;
			}
		}
	}


	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		updateInstructionCompletions(((AddressCarryingEvent)event).getAddress());
	}
	
	public void processCompletionOfMemRequest(long requestedAddress)
	{
		updateInstructionCompletions(requestedAddress);
		containingExecutionEngine.noOfOutstandingLoads--;
	}
	
	void updateInstructionCompletions(long address)
	{
		Instruction instructions[] = memWbLatch.getInstructions();
		long instructionCompletesAt[] = memWbLatch.getInstructionCompletesAt();
		
		for(int i = 0; i < containingExecutionEngine.getIssueWidth(); i++)
		{
			if(instructions[i] != null
					&& instructions[i].getOperationType() == OperationType.load
					&& instructions[i].getSourceOperand1MemValue() == address
					&& instructionCompletesAt[i] > GlobalClock.getCurrentTime())
			{
				instructionCompletesAt[i] = GlobalClock.getCurrentTime();
			}
		}
	}
}
