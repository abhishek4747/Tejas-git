package pipeline.multi_issue_inorder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.Operand;
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
	RF rf;

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
		rf = execEngine.getRF();

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

		System.out.println("Decode ");

		while (ifIdLatch.isEmpty() == false && idExRS.isFull() == false
				&& !rob.rob.isFull()) {
			ins = ifIdLatch.peek(0);
			OperationType opType;
			System.out.println("Instruction: " + ins);
			opType = ins.getOperationType();
			if (opType == OperationType.inValid) {
				System.out.print("ENd here");
			}
			if (ins != null) {

				int b = rob.getFreeTail();
				if (b != -1) {
					int r = idExRS.getFree();
					if (opType == OperationType.inValid) {
						System.out.println("End Here.");
						idExRS.rs[r].busy = true;
						idExRS.rs[r].Qi = b;
						idExRS.rs[r].opType = ins.getOperationType();
						rob.add(ins, GlobalClock.getCurrentTime() + 1);
						System.out.println("Added to rob" + r + " Optype"
								+ ins.getOperationType());

					} else {
						Operand o1 = ins.getSourceOperand1();
						if (rf.rf[(int) o1.getValue()].busy) {
							int h = rf.rf[(int) o1.getValue()].Qi;
							if (rob.rob.absPeek(h).ready) {
								idExRS.rs[r].Vj = rob.rob.absPeek(h).r1;
								idExRS.rs[r].Qj = 0;
							} else {
								idExRS.rs[r].Qj = h;
							}
						} else {
							idExRS.rs[r].Vj = rf.rf[(int) o1.getValue()].value;
							idExRS.rs[r].Qj = 0;
						}

						idExRS.rs[r].busy = true;
						idExRS.rs[r].Qi = b;
						idExRS.rs[r].opType = ins.getOperationType();
						rob.add(ins, GlobalClock.getCurrentTime() + 1);
						System.out.println("Added to rob" + r + " Optype"
								+ ins.getOperationType());

						if (ins.getOperationType() == OperationType.floatALU
								|| ins.getOperationType() == OperationType.integerALU
								|| ins.getOperationType() == OperationType.store) {
							Operand o2 = ins.getSourceOperand2();
							if (rf.rf[(int) o2.getValue()].busy) {
								int h = rf.rf[(int) o2.getValue()].Qi;
								if (rob.rob.absPeek(h).ready) {
									idExRS.rs[r].Vk = rob.rob.absPeek(h).r2;
									idExRS.rs[r].Qk = 0;
								} else {
									idExRS.rs[r].Qk = h;
								}
							} else {
								idExRS.rs[r].Vk = rf.rf[(int) o2.getValue()].value;
								idExRS.rs[r].Qk = 0;
							}
						}

						if (ins.getOperationType() == OperationType.floatALU
								|| ins.getOperationType() == OperationType.integerALU) {
							Operand od = ins.getDestinationOperand();
							rf.rf[(int) od.getValue()].Qi = b;
							rf.rf[(int) od.getValue()].busy = true;
							rob.rob.absPeek(b).dest = od;
						}

						if (ins.getOperationType() == OperationType.load) {
							// rs.rs[r]
						}

						if (ins.getOperationType() == OperationType.store) {
							//
						}
					}
				}

				// if (checkDataHazard(ins)) // Data Hazard Detected,Stall
				// Pipeline
				// {
				// containingExecutionEngine.incrementDataHazardStall(1);
				// break;
				// }

				// check for structural hazards
				// long FURequest = 0;
				// if (OpTypeToFUTypeMapping.getFUType(ins.getOperationType())
				// != FunctionalUnitType.inValid) {
				// FURequest = containingExecutionEngine.getExecutionCore()
				// .requestFU(
				// OpTypeToFUTypeMapping.getFUType(ins
				// .getOperationType()));
				//
				// if (FURequest > 0) {
				// break;
				// }
				// }

				incrementNumDecodes(1);

				// add destination register of ins to list of outstanding
				// registers
				// if (ins.getOperationType() == OperationType.load) {
				// addToValueReadyArray(ins.getDestinationOperand(),
				// Long.MAX_VALUE);
				// } else if (ins.getOperationType() == OperationType.xchg) {
				// addToValueReadyArray(
				// ins.getSourceOperand1(),
				// GlobalClock.getCurrentTime()
				// + containingExecutionEngine
				// .getExecutionCore()
				// .getFULatency(
				// OpTypeToFUTypeMapping.getFUType(ins
				// .getOperationType())));
				// if (ins.getSourceOperand1().getValue() != ins
				// .getSourceOperand2().getValue()
				// || ins.getSourceOperand1().getOperandType() != ins
				// .getSourceOperand2().getOperandType()) {
				// addToValueReadyArray(
				// ins.getSourceOperand2(),
				// GlobalClock.getCurrentTime()
				// + containingExecutionEngine
				// .getExecutionCore()
				// .getFULatency(
				// OpTypeToFUTypeMapping.getFUType(ins
				// .getOperationType())));
				// }
				// } else {
				// if (ins.getDestinationOperand() != null) {
				// addToValueReadyArray(
				// ins.getDestinationOperand(),
				// GlobalClock.getCurrentTime()
				// + containingExecutionEngine
				// .getExecutionCore()
				// .getFULatency(
				// OpTypeToFUTypeMapping.getFUType(ins
				// .getOperationType())));
				// }
				// }

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
				// idExLatch.add(ins, GlobalClock.getCurrentTime() + 1);
				ifIdLatch.poll();
				// System.out.println("Removing ins from ifidlatch: "+ins);

				if (SimulationConfig.debugMode) {
					System.out.println("decoded : "
							+ GlobalClock.getCurrentTime() / core.getStepSize()
							+ "\n" + ins + "\n");
				}

				// if a branch/jump instruction is issued, no more instructions
				// to be issued this cycle
				if (opType == OperationType.branch
						|| opType == OperationType.jump) {
					break;
				}
			} else {
				break;
			}
		}
	}

	private boolean checkDataHazard(Instruction ins) {
		Operand srcOpnd;

		// operand 1
		srcOpnd = ins.getSourceOperand1();
		if (srcOpnd != null) {
			if (srcOpnd.isIntegerRegisterOperand()) {
				if (containingExecutionEngine.getValueReadyInteger()[(int) (srcOpnd
						.getValue())] > GlobalClock.getCurrentTime()) {
					return true;
				}
			}

			else if (srcOpnd.isFloatRegisterOperand()) {
				if (containingExecutionEngine.getValueReadyFloat()[(int) (srcOpnd
						.getValue())] > GlobalClock.getCurrentTime()) {
					return true;
				}
			}
		}

		// operand 2
		srcOpnd = ins.getSourceOperand2();
		if (srcOpnd != null) {
			if (srcOpnd.isIntegerRegisterOperand()) {
				if (containingExecutionEngine.getValueReadyInteger()[(int) (srcOpnd
						.getValue())] > GlobalClock.getCurrentTime()) {
					return true;
				}
			}

			else if (srcOpnd.isFloatRegisterOperand()) {
				if (containingExecutionEngine.getValueReadyFloat()[(int) (srcOpnd
						.getValue())] > GlobalClock.getCurrentTime()) {
					return true;
				}
			}
		}

		return false;
	}

	private void addToValueReadyArray(Operand destOpnd, long timeWhenValueReady) {
		if (destOpnd.isIntegerRegisterOperand()) {
			containingExecutionEngine.getValueReadyInteger()[(int) (destOpnd
					.getValue())] = timeWhenValueReady;
		}

		else if (destOpnd.isFloatRegisterOperand()) {
			containingExecutionEngine.getValueReadyFloat()[(int) (destOpnd
					.getValue())] = timeWhenValueReady;
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
