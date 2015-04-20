package pipeline.multi_issue_inorder;

import generic.Core;
import generic.GenericCircularQueue;
import generic.Instruction;
import generic.Operand;
import generic.OperationType;
import generic.PinPointsProcessing;
import config.SimulationConfig;

class ROBSlot {
	Instruction instr;
	boolean busy;
	boolean ready;
	Operand r1;
	boolean r1avail;
	Operand r2;
	boolean r2avail;
	Operand dest;
	long instructionCompletesAt;
}

public class ROB {
	GenericCircularQueue<ROBSlot> rob;
	public int ROBSize;

	Core core;
	MultiIssueInorderExecutionEngine containingExecutionEngine;

	long lastValidIpSeen;
	int numMispredictedBranches;
	int numBranches;

	ROB(Core core, MultiIssueInorderExecutionEngine execEngine, int ROBSize) {
		this.core = core;
		this.containingExecutionEngine = execEngine;
		this.ROBSize = ROBSize;
		rob = new GenericCircularQueue<ROBSlot>(ROBSlot.class, ROBSize);
		lastValidIpSeen = -1;
		numMispredictedBranches = 0;
		numBranches = 0;
	}

	public boolean add(Instruction i, long insCompletesAt) {
		if (!rob.isFull()) {
			ROBSlot r = new ROBSlot();
			r.busy = true;
			r.instr = i;
			r.ready = false;
			r.r1avail = false;
			r.r2avail = false;
			r.r1 = i.getSourceOperand1();
			r.r2 = i.getSourceOperand2();
			r.dest = i.getDestinationOperand();
			r.instructionCompletesAt = insCompletesAt;
			rob.enqueue(r);
			return true;
		}
		return false;
	}

	public int getTail() {
		if (rob.isFull())
			return -2;
		if (rob.isEmpty())
			return -1;
		return rob.getTail();
	}
	
	public int getFreeTail(){
		return (getTail()+1)%rob.getBufferSize();
	}

	public void removeFromHead() {
		System.out.println("inside removefromhead");
		rob.dequeue();
	}

	void flush() {
		rob.clear();
	}

	public void performCommit(RF rf) {
		System.out.println("6--> In commit Unit");
		if (rob.isEmpty()){
			System.out.println("\tROB empty. Nothing to be done.");
			return;
		}
		Instruction ins= rob.peek(0).instr;
		System.out.println(1);
		if (ins!=null){
			// check if simulation complete
			if (ins.getOperationType() == OperationType.inValid) {
				this.core.currentThreads--;
	
				if (this.core.currentThreads == 0) { // set exec complete
														// only if there are
														// n other thread
														// already
														// assigned to this
														// pipeline
					containingExecutionEngine.setExecutionComplete(true);
					if (SimulationConfig.pinpointsSimulation == false) {
						containingExecutionEngine.setTimingStatistics();
						containingExecutionEngine
								.setPerCoreMemorySystemStatistics();
					} else {
						PinPointsProcessing.processEndOfSlice();
					}
				}
			} 
//				else {
//				if (core.getNoOfInstructionsExecuted() % 1000000 == 0) {
//					System.out.println(core.getNoOfInstructionsExecuted()
//							/ 1000000 + " million done" + " by core "
//							+ core.getCore_number()
//							+ " global clock cycle "
//							+ GlobalClock.getCurrentTime());
//				}
//				core.incrementNoOfInstructionsExecuted();
//			}

			if (rob.peek(0).instr.getCISCProgramCounter() != -1) {
				lastValidIpSeen = rob.peek(0).instr.getCISCProgramCounter();
			}
			
			if (rob.peek(0).ready) {
				System.out.print("\tRob head is ready to be committed.");
				if (rob.peek(0).instr.getOperationType() == OperationType.branch) {
					System.out.print("\tBranch Instruction");
					boolean prediction = containingExecutionEngine
							.getBranchPredictor().predict(lastValidIpSeen,
									rob.peek(0).instr.isBranchTaken());
					this.containingExecutionEngine.getBranchPredictor()
							.incrementNumAccesses(1);
	
					containingExecutionEngine.getBranchPredictor().Train(
							rob.peek(0).instr.getCISCProgramCounter(),
							rob.peek(0).instr.isBranchTaken(), prediction);
					this.containingExecutionEngine.getBranchPredictor()
							.incrementNumAccesses(1);
	
					numBranches++;
	
					if (prediction != rob.peek(0).instr.isBranchTaken()) {
						System.out.println("\tFlushing ROB coz mispredicted");
						containingExecutionEngine.setMispredStall(core
								.getBranchMispredictionPenalty());
						numMispredictedBranches++;
						flush();
						rf.flush();
					}
				}
				
				if (rf.rf[(int) rob.peek(0).dest.getValue()].Qi == rob.getHead()) {
					rf.rf[(int) rob.peek(0).dest.getValue()].busy = false;
				}
				
				removeFromHead();
	
				core.incrementNoOfInstructionsExecuted();
				if (core.getNoOfInstructionsExecuted() % 1000000 == 0)
					System.out.println(core.getNoOfInstructionsExecuted() / 1000000
							+ " million done on " + core.getCore_number());
			}
		}
	}

	public static int getROBSize() {
		return 10;
	}
}