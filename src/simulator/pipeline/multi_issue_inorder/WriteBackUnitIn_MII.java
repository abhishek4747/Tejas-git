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

import main.CustomObjectPool;
import config.EnergyConfig;
import config.SimulationConfig;

public class WriteBackUnitIn_MII extends SimulationElement {

	Core core;
	MultiIssueInorderExecutionEngine containingExecutionEngine;
	StageLatch_MII memWbLatch;
	ReservationStation rs;
	CommonDataBus cdb;

	long instCtr; // for debug

	long numIntRegFileAccesses;
	long numFloatRegFileAccesses;
	private ROB rob;

	public WriteBackUnitIn_MII(Core core,
			MultiIssueInorderExecutionEngine execEngine) {
		super(PortType.Unlimited, -1, -1, -1, -1);
		this.core = core;
		containingExecutionEngine = execEngine;
		memWbLatch = execEngine.getMemWbLatch();
		rs = execEngine.getIdExRS();
		rob = execEngine.getROB();
		cdb = execEngine.getCDB();

		instCtr = 0;
	}

	public void performWriteBack(MultiIssueInorderPipeline inorderPipeline) {
		System.out.println("In WB");
		if (containingExecutionEngine.getMispredStall() > 0) {
			return;
		}

		Instruction ins = null;

		while (memWbLatch.isEmpty() == false) {
			System.out.println("Tring to write to CDB");
			ins = memWbLatch.peek(0);
			int r = -1;
			for (int i = 0; i < rs.size; i++) {
				if (rs.rs[i].busy && rs.rs[i].executionComplete
						&& rob.rob.absPeek(rs.rs[i].Qi).instr == ins) {
					r = i;
					break;
				}
			}

			if (r == -1) {
				System.out.println("ins not in RS");
			}
			rs.rs[r].busy = false;
			int b = rs.rs[r].Qi;

			for (int i = 0; i < rs.size; i++) {
				if (rs.rs[i].Qj == b) {
					rs.rs[i].Qj = 0;
				}
				if (rs.rs[i].Qk == b) {
					rs.rs[i].Qk = 0;
				}
			}

			//rob.rob[b].ready = true;
			System.out.println("Ready "+b+" slot in cdb.");
			cdb.insert(b, 0);

			if (ins != null) {

				// increment register file accesses for power statistics
				if (!(ins.getOperationType() == OperationType.store && rs.rs[r].Qk == 0)) {
					continue;
				}
				// operand fetch
				incrementNumRegFileAccesses(ins.getSourceOperand1(), 1);
				incrementNumRegFileAccesses(ins.getSourceOperand2(), 1);

				// write-back
				incrementNumRegFileAccesses(ins.getDestinationOperand(), 1);
				if (ins.getOperationType() == OperationType.xchg) {
					incrementNumRegFileAccesses(ins.getSourceOperand1(), 1);
					if (ins.getSourceOperand1().getValue() != ins
							.getSourceOperand2().getValue()
							|| ins.getSourceOperand1().getOperandType() != ins
									.getSourceOperand2().getOperandType()) {
						incrementNumRegFileAccesses(ins.getSourceOperand2(), 1);
					}
				}

				// if (ins.getSerialNo() != instCtr
				// && ins.getOperationType() != OperationType.inValid) {
				// misc.Error.showErrorAndExit("wb out of order!!");
				// }
				instCtr++;

				if (SimulationConfig.debugMode) {
					System.out.println("write back : "
							+ GlobalClock.getCurrentTime() / core.getStepSize()
							+ "\n" + ins + "\n");
				}

				memWbLatch.poll();
				try {
					CustomObjectPool.getInstructionPool().returnObject(ins);
					core.numReturns++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				break;
			}
		}
		// cdb.flushCDB();
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {

	}

	void incrementNumRegFileAccesses(Operand operand, int incrementBy) {
		if (operand == null) {
			return;
		}

		if (operand.isIntegerRegisterOperand()) {
			incrementNumIntRegFileAccesses(incrementBy);
		} else if (operand.isFloatRegisterOperand()) {
			incrementNumFloatRegFileAccesses(incrementBy);
		}
	}

	void incrementNumIntRegFileAccesses(int incrementBy) {
		numIntRegFileAccesses += incrementBy;
	}

	void incrementNumFloatRegFileAccesses(int incrementBy) {
		numFloatRegFileAccesses += incrementBy;
	}

	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter,
			String componentName) throws IOException {
		EnergyConfig totalPower = new EnergyConfig(0, 0);
		EnergyConfig intRegFilePower = new EnergyConfig(
				core.getIntRegFilePower(), numIntRegFileAccesses);
		totalPower.add(totalPower, intRegFilePower);
		EnergyConfig floatRegFilePower = new EnergyConfig(
				core.getFpRegFilePower(), numFloatRegFileAccesses);
		totalPower.add(totalPower, floatRegFilePower);

		intRegFilePower.printEnergyStats(outputFileWriter, componentName
				+ ".int");
		floatRegFilePower.printEnergyStats(outputFileWriter, componentName
				+ ".float");

		return totalPower;
	}
}
