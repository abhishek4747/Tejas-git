package pipeline.multi_issue_inorder;

import config.SimulationConfig;
import memorysystem.AddressCarryingEvent;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.Operand;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;

public class MemUnitIn_MII extends SimulationElement {

	Core core;
	MultiIssueInorderExecutionEngine containingExecutionEngine;
	EventQueue eventQueue;
	StageLatch_MII exMemLatch;
	StageLatch_MII memWbLatch;
	LoadStoreQueue lsq;
	ROB rob;

	long instCtr; // for debug

	public MemUnitIn_MII(Core core, MultiIssueInorderExecutionEngine execEngine) {
		super(PortType.Unlimited, -1, -1, -1, -1);
		this.core = core;
		this.eventQueue = core.getEventQueue();
		containingExecutionEngine = execEngine;
		exMemLatch = execEngine.getExMemLatch();
		memWbLatch = execEngine.getMemWbLatch();
		lsq = execEngine.getLSQ();
		rob = execEngine.getROB();

		instCtr = 0;
	}

	public void performMemEvent(MultiIssueInorderPipeline inorderPipeline) {
		if (containingExecutionEngine.getMispredStall() > 0) {
			return;
		}
		System.out.println("4--> In mem unit");
		Instruction ins = null;

		while (exMemLatch.isEmpty() == false && memWbLatch.isFull() == false) {
			ins = exMemLatch.peek(0);
			System.out.println("\tWriting to memWbLatch");

			if (ins != null) {
				if (ins.getOperationType() == OperationType.inValid) {
					System.out.println("End here");
				}
				long lat = 1;
				if (ins.getOperationType() == OperationType.load) {
					if (!SimulationConfig.detachMemSysData
							&& rob.storesAtThisAddressBefore(ins)) {
						boolean memReqIssued = containingExecutionEngine.multiIssueInorderCoreMemorySystem
								.issueRequestToL1Cache(RequestType.Cache_Read,
										ins.getSourceOperand1MemValue());

						if (memReqIssued == false) {
							break;
						}
					}

					// set instruction's MEM stage completion time to
					// Long.MAX_VALUE
					lat = Long.MAX_VALUE - GlobalClock.getCurrentTime();
				}
				
				instCtr++;

				// move ins to next stage
				memWbLatch.add(ins, GlobalClock.getCurrentTime() + lat);
				exMemLatch.poll();

				if (SimulationConfig.debugMode) {
					System.out.println("MEM : " + GlobalClock.getCurrentTime()
							/ core.getStepSize() + "\n" + ins + "\n");
				}
			} else {
				break;
			}
		}
		System.out.println();
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		updateInstructionCompletions(((AddressCarryingEvent) event)
				.getAddress());
	}

	public void processCompletionOfMemRequest(long requestedAddress) {
		updateInstructionCompletions(requestedAddress);
		containingExecutionEngine.noOfOutstandingLoads--;
	}

	void updateInstructionCompletions(long address) {
		Instruction instructions[] = memWbLatch.getInstructions();
		long instructionCompletesAt[] = memWbLatch.getInstructionCompletesAt();

		for (int i = 0; i < containingExecutionEngine.getIssueWidth(); i++) {
			if (instructions[i] != null
					&& instructions[i].getOperationType() == OperationType.load
					&& instructions[i].getSourceOperand1MemValue() == address
					&& instructionCompletesAt[i] > GlobalClock.getCurrentTime()) {
				instructionCompletesAt[i] = GlobalClock.getCurrentTime();

				Operand destOpnd = instructions[i].getDestinationOperand();
				if (destOpnd.isIntegerRegisterOperand()) {
					containingExecutionEngine.getValueReadyInteger()[(int) destOpnd
							.getValue()] = GlobalClock.getCurrentTime();
				} else if (destOpnd.isFloatRegisterOperand()) {
					containingExecutionEngine.getValueReadyFloat()[(int) destOpnd
							.getValue()] = GlobalClock.getCurrentTime();
				}
			}
		}
	}
}
