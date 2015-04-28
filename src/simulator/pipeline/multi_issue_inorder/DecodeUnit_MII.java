package pipeline.multi_issue_inorder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

import java.io.FileWriter;
import java.io.IOException;

import config.EnergyConfig;
import config.SimulationConfig;

public class DecodeUnit_MII extends SimulationElement {

	Core core;
	MultiIssueInorderExecutionEngine containingExecutionEngine;
	StageLatch_MII ifIdLatch;
	ReservationStation idExRS;
	ROB rob;
	LoadStoreQueue lsq;
	RF irf, frf;

	long numBranches;
	long numMispredictedBranches;
	long lastValidIPSeen;

	long numAccesses;

	long instCtr; // for debug

	public DecodeUnit_MII(Core core, MultiIssueInorderExecutionEngine execEngine) {
		/*
		 * numPorts and occupancy = -1 => infinite ports Latency = 1 .
		 */
		super(PortType.Unlimited, -1, -1, -1, -1);
		this.core = core;
		containingExecutionEngine = execEngine;
		ifIdLatch = execEngine.getIfIdLatch();
		idExRS = execEngine.getIdExRS();
		rob = execEngine.getROB();
		lsq = execEngine.getLSQ();
		irf = execEngine.getIntRF();
		frf = execEngine.getFloatRF();

		numBranches = 0;
		numMispredictedBranches = 0;
		lastValidIPSeen = -1;

		instCtr = 0;
	}

	public void performDecode(MultiIssueInorderPipeline inorderPipeline) {

		if (containingExecutionEngine.getMispredStall() > 0) {
			return;
		}

		containingExecutionEngine.getExecutionCore().clearPortUsage();

		Instruction ins = null;

		System.out.println("2--> Decode ");

		while (ifIdLatch.isEmpty() == false && idExRS.isFull() == false
				&& !rob.rob.isFull()) {
			ins = ifIdLatch.peek(0);
			OperationType opType;
			System.out.println("\tIns: " + ins);
			opType = ins.getOperationType();
			if (opType == OperationType.inValid) {
				System.out.print("End here");
			}
			int b = rob.getFreeTail();
			if (b != -1) {
				if (!(ins.getOperationType() == OperationType.load || ins
						.getOperationType() == OperationType.store)) {
					int r = idExRS.getFree();
					if (opType == OperationType.inValid) {
						System.out.println("End Here.");

					} else {
						Operand o1 = ins.getSourceOperand1();
						if (o1.getOperandType() == OperandType.memory) {
							RF.getRegister(irf, frf,
									o1.getMemoryLocationFirstOperand()).Qi = b;
							RF.getRegister(irf, frf,
									o1.getMemoryLocationFirstOperand()).busy = true;
							RF.getRegister(irf, frf,
									o1.getMemoryLocationSecondOperand()).Qi = b;
							RF.getRegister(irf, frf,
									o1.getMemoryLocationSecondOperand()).busy = true;
						} else if (o1.getOperandType() != OperandType.immediate
								&& RF.getRegister(irf, frf, o1).busy) {
							int h = RF.getRegister(irf, frf, o1).Qi;
							if (rob.rob.absPeek(h).ready) {
								idExRS.rs[r].Vj = rob.rob.absPeek(h).r1;
								idExRS.rs[r].Qj = 0;
							} else {
								idExRS.rs[r].Qj = h;
							}
						} else if (o1.getOperandType() != OperandType.immediate
								&& !RF.getRegister(irf, frf, o1).busy) {
							idExRS.rs[r].Vj = RF.getRegister(irf, frf, o1).value;
							idExRS.rs[r].Qj = 0;
						} else {
							idExRS.rs[r].Vj = o1;
							idExRS.rs[r].Qj = 0;
						}
					}
					idExRS.rs[r].busy = true;
					idExRS.rs[r].Qi = b;
					idExRS.rs[r].opType = ins.getOperationType();
					idExRS.rs[r].executionComplete = false;

					int slot = rob.add(ins, GlobalClock.getCurrentTime() + 1);
					System.out.println("\tAdded to rob" + r + " Optype"
							+ ins.getOperationType());
					if (!(ins.getOperationType() == OperationType.load || ins
							.getOperationType() == OperationType.store)) {
						if (ins.getOperationType() == OperationType.floatALU
								|| ins.getOperationType() == OperationType.floatMul
								|| ins.getOperationType() == OperationType.floatDiv
								|| ins.getOperationType() == OperationType.integerALU
								|| ins.getOperationType() == OperationType.integerMul
								|| ins.getOperationType() == OperationType.integerDiv
								|| ins.getOperationType() == OperationType.store) {
							Operand o2 = ins.getSourceOperand2();
							if (o2.getOperandType() == OperandType.memory) {
								RF.getRegister(irf, frf,
										o2.getMemoryLocationFirstOperand()).Qi = b;
								RF.getRegister(irf, frf,
										o2.getMemoryLocationFirstOperand()).busy = true;
								RF.getRegister(irf, frf,
										o2.getMemoryLocationSecondOperand()).Qi = b;
								RF.getRegister(irf, frf,
										o2.getMemoryLocationSecondOperand()).busy = true;
							} else if (o2.getOperandType() != OperandType.immediate
									&& RF.getRegister(irf, frf, o2).busy) {
								int h = RF.getRegister(irf, frf, o2).Qi;
								if (rob.rob.absPeek(h).ready) {
									idExRS.rs[r].Vk = rob.rob.absPeek(h).r2;
									idExRS.rs[r].Qk = 0;
								} else {
									idExRS.rs[r].Qk = h;
								}
							} else if (o2.getOperandType() != OperandType.immediate) {
								idExRS.rs[r].Vk = RF.getRegister(irf, frf, o2).value;
								idExRS.rs[r].Qk = 0;
							} else {
								idExRS.rs[r].Vk = o2;
								idExRS.rs[r].Qk = 0;
							}
						}
					}

					if (ins.getOperationType() == OperationType.load
							|| ins.getOperationType() == OperationType.store)
						lsq.enqueue(
								ins.getOperationType() == OperationType.load,
								slot, ins.getSourceOperand1MemValue(), ins);

					if (ins.getOperationType() == OperationType.floatALU
							|| ins.getOperationType() == OperationType.floatDiv
							|| ins.getOperationType() == OperationType.floatMul
							|| ins.getOperationType() == OperationType.integerALU
							|| ins.getOperationType() == OperationType.integerDiv
							|| ins.getOperationType() == OperationType.integerMul
							|| ins.getOperationType() == OperationType.load) {
						Operand od = ins.getDestinationOperand();
						if (od != null
								&& od.getOperandType() == OperandType.memory) {
							RF.getRegister(irf, frf,
									od.getMemoryLocationFirstOperand()).Qi = b;
							RF.getRegister(irf, frf,
									od.getMemoryLocationFirstOperand()).busy = true;
							RF.getRegister(irf, frf,
									od.getMemoryLocationSecondOperand()).Qi = b;
							RF.getRegister(irf, frf,
									od.getMemoryLocationSecondOperand()).busy = true;
						} else if (od != null) {
							RF.getRegister(irf, frf, od).Qi = b;
							RF.getRegister(irf, frf, od).busy = true;
						}
						rob.rob.absPeek(b).dest = od;
					}

					if (ins.getOperationType() == OperationType.store)
						idExRS.rs[r].A = ins.getSourceOperand2().getValue();

					if (ins.getOperationType() == OperationType.xchg) {
						RF.getRegister(irf, frf, ins.getSourceOperand1()).Qi = b;
						RF.getRegister(irf, frf, ins.getSourceOperand1()).busy = true;
						RF.getRegister(irf, frf, ins.getSourceOperand2()).Qi = b;
						RF.getRegister(irf, frf, ins.getSourceOperand2()).busy = true;
					}
				}
			}

			incrementNumDecodes(1);

			// update last valid IP seen
			if (ins.getCISCProgramCounter() != -1) {
				lastValidIPSeen = ins.getCISCProgramCounter();
			}

			// perform branch prediction
			if (ins.getOperationType() == OperationType.branch) {
				boolean prediction = containingExecutionEngine
						.getBranchPredictor().predict(lastValidIPSeen,
								ins.isBranchTaken());
				if (prediction != ins.isBranchTaken()) {
					// Branch mispredicted
					// stall pipeline for appropriate cycles
					containingExecutionEngine.setMispredStall(core
							.getBranchMispredictionPenalty());
					numMispredictedBranches++;
				}
				this.containingExecutionEngine.getBranchPredictor()
						.incrementNumAccesses(1);

				// Train Branch Predictor
				containingExecutionEngine.getBranchPredictor().Train(
						ins.getCISCProgramCounter(), ins.isBranchTaken(),
						prediction);
				this.containingExecutionEngine.getBranchPredictor()
						.incrementNumAccesses(1);

				numBranches++;
			}

			if (ins.getSerialNo() != instCtr
					&& ins.getOperationType() != OperationType.inValid) {
				misc.Error.showErrorAndExit("decode out of order!!");
			}
			instCtr++;

			// move ins to next stage
			ifIdLatch.poll();

			if (SimulationConfig.debugMode) {
				System.out.println("decoded : " + GlobalClock.getCurrentTime()
						/ core.getStepSize() + "\n" + ins + "\n");
			}

			// if a branch/jump instruction is issued, no more instructions
			// to be issued this cycle
			if (opType == OperationType.branch || opType == OperationType.jump) {
				break;
			}
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

	void incrementNumDecodes(int incrementBy) {
		numAccesses += incrementBy;
	}

	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter,
			String componentName) throws IOException {
		EnergyConfig power = new EnergyConfig(containingExecutionEngine
				.getContainingCore().getDecodePower(), numAccesses);
		power.printEnergyStats(outputFileWriter, componentName);
		return power;
	}
}
