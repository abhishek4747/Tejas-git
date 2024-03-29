package pipeline.multi_issue_inorder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

import java.io.FileWriter;
import java.io.IOException;

import pipeline.FunctionalUnitType;
import pipeline.OpTypeToFUTypeMapping;
import config.EnergyConfig;
import config.SimulationConfig;

public class ExecUnitIn_MII extends SimulationElement {
	Core core;
	EventQueue eventQueue;
	MultiIssueInorderExecutionEngine containingExecutionEngine;
	ReservationStation idExRS;
	long[] instructionCompletesAt;
	CommonDataBus cdb;
	ROB rob;
	LoadStoreQueue lsq;

	FunctionalUnitType futype;
	StageLatch_MII exMemLatch;

	long instCtr; // for debug

	long numResultsBroadCastBusAccess;
	public int id;

	public ExecUnitIn_MII(Core core,
			MultiIssueInorderExecutionEngine execEngine,
			FunctionalUnitType futype) {
		super(PortType.Unlimited, -1, -1, -1, -1);
		this.core = core;
		this.eventQueue = core.getEventQueue();
		containingExecutionEngine = execEngine;
		idExRS = execEngine.getIdExRS();
		exMemLatch = execEngine.getExMemLatch();
		lsq = execEngine.getLSQ();

		cdb = execEngine.getCDB();
		rob = execEngine.getROB();
		this.futype = futype;

		instructionCompletesAt = new long[containingExecutionEngine
				.getIssueWidth()];
		for (int i = 0; i < containingExecutionEngine.getIssueWidth(); i++) {
			instructionCompletesAt[i] = -1;
		}

		instCtr = 0;
	}

	public void execute(MultiIssueInorderPipeline inorderPipeline) {
		if (containingExecutionEngine.getMispredStall() > 0) {
			return;
		}
		System.out.println("3--> In exec " + id);
		Instruction ins = null;
		long insCompletesAt;

		if (exMemLatch.isFull() == false) {
			if (idExRS.isEmpty(futype) == false) {
				int rsid = idExRS.getIWithFu(futype);

				if (rsid != -1) {
					ins = rob.rob.absPeek(idExRS.rs[rsid].Qi).instr;
					System.out
							.println("\tExecuting (" + rsid + ") ins= " + ins);
					insCompletesAt = rob.rob.absPeek(idExRS.rs[rsid].Qi).instructionCompletesAt;
					idExRS.rs[rsid].Qk = 0;
					idExRS.rs[rsid].Qj = 0;

					if (ins.getOperationType() == OperationType.inValid) {
						System.out.println("End here");
					}

					long lat = 1;

					FunctionalUnitType FUType = OpTypeToFUTypeMapping
							.getFUType(ins.getOperationType());

					if (FUType != FunctionalUnitType.inValid) {
						lat = containingExecutionEngine.getExecutionCore()
								.getFULatency(FUType);
					}
					/*
					 * memory address computation for loads/stores happens in
					 * this cycle assumed as one cycle operation
					 */

					instCtr++;

					// move ins to next stage
					exMemLatch.add(ins, insCompletesAt + lat);
					System.out.println("\tadding it to exMemLatch");

					if (ins.getDestinationOperand() != null
							|| ins.getOperationType() == OperationType.xchg) {
						incrementResultsBroadcastBusAccesses(1);
					}

					if (SimulationConfig.debugMode) {
						System.out.println("executed : "
								+ GlobalClock.getCurrentTime()
								/ core.getStepSize() + "\n" + ins + "\n");
					}
					idExRS.rs[rsid].executionComplete = true;
				}
			}
		} else if (futype == FunctionalUnitType.memory) {
			LSQEntry lsqd = lsq.dequeue();
			ins = lsqd.instruction;
			instCtr++;
			if (ins.getOperationType() == OperationType.load) {
				int ind = lsq.getIndex(ins);
				if (lsq.noStoresBefore(ind))
					lsq.dequeue(ind);
			}
			insCompletesAt = rob.rob.absPeek(lsqd.ROBEntry).instructionCompletesAt;
			long lat = 1;
			FunctionalUnitType FUType = OpTypeToFUTypeMapping.getFUType(ins
					.getOperationType());

			if (FUType != FunctionalUnitType.inValid) {
				lat = containingExecutionEngine.getExecutionCore()
						.getFULatency(FUType);
			}

			if (ins.getOperationType() == OperationType.store) {
				if (!SimulationConfig.detachMemSysData
						&& lsq.getIndex(ins) == 0)
					lsq.dequeue();
				else
					return;
			}
			// move ins to next stage
			exMemLatch.add(ins, insCompletesAt + lat);
			System.out.println("\tadding it to exMemLatch");

		}
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {

	}

	void incrementResultsBroadcastBusAccesses(int incrementBy) {
		numResultsBroadCastBusAccess += incrementBy;
	}

	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter,
			String componentName) throws IOException {
		EnergyConfig power = new EnergyConfig(
				core.getResultsBroadcastBusPower(),
				numResultsBroadCastBusAccess);
		power.printEnergyStats(outputFileWriter, componentName);
		return power;
	}
}
