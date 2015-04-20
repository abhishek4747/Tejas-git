package pipeline.multi_issue_inorder;

import java.io.FileWriter;
import java.io.IOException;

import pipeline.FunctionalUnitType;
import pipeline.OpTypeToFUTypeMapping;
import pipeline.multi_issue_inorder.MultiIssueInorderExecutionEngine;
import pipeline.multi_issue_inorder.StageLatch_MII;
import config.EnergyConfig;
import config.SimulationConfig;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class ExecUnitIn_MII extends SimulationElement {
	Core core;
	EventQueue eventQueue;
	MultiIssueInorderExecutionEngine containingExecutionEngine;
	ReservationStation idExRS;
	long[] instructionCompletesAt;
	CommonDataBus cdb;
	ROB rob;
	FunctionalUnitType futype;
	StageLatch_MII exMemLatch;

	long instCtr; // for debug

	long numResultsBroadCastBusAccess;
	public int id;

	public ExecUnitIn_MII(Core core, MultiIssueInorderExecutionEngine execEngine, FunctionalUnitType futype) {
		super(PortType.Unlimited, -1, -1, -1, -1);
		this.core = core;
		this.eventQueue = core.getEventQueue();
		containingExecutionEngine = execEngine;
		idExRS = execEngine.getIdExRS();
		exMemLatch = execEngine.getExMemLatch();

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
		System.out.print("3--> In exec "+id);
		Instruction ins = null;
		
		while (idExRS.isEmpty(futype) == false && exMemLatch.isFull() == false) {
			int rsid = idExRS.getIWithFu(futype);
			
			if (rsid!=-1){
				ins = rob.rob.absPeek(idExRS.rs[rsid].Qi).instr;
				System.out.println("\tExecuting ("+rsid+") ins= "+ins);
				long insCompletesAt = rob.rob.absPeek(idExRS.rs[rsid].Qi).instructionCompletesAt;
				idExRS.rs[rsid].Qk = 0;
				idExRS.rs[rsid].Qj = 0;
//			}
//			
//			if (ins != null) {
				if (ins.getOperationType()==OperationType.inValid){
					System.out.println("End here");
				}
				FunctionalUnitType FUType = OpTypeToFUTypeMapping.getFUType(ins
						.getOperationType());
				long lat = 1;

				if (FUType != FunctionalUnitType.memory
						&& FUType != FunctionalUnitType.inValid) {
					lat = containingExecutionEngine.getExecutionCore()
							.getFULatency(FUType);
				}
				/*
				 * memory address computation for loads/stores happens in this
				 * cycle assumed as one cycle operation
				 */

//				if (ins.getSerialNo() != instCtr
//						&& ins.getOperationType() != OperationType.inValid) {
//					misc.Error.showErrorAndExit("exec out of order!!");
//				}
				instCtr++;

				// move ins to next stage
				exMemLatch.add(ins, insCompletesAt+ lat);
				System.out.println("\tadding it to exMemLatch");
//				idExLatch.poll();

				if (ins.getDestinationOperand() != null
						|| ins.getOperationType() == OperationType.xchg) {
					incrementResultsBroadcastBusAccesses(1);
				}

				if (SimulationConfig.debugMode) {
					System.out.println("executed : "
							+ GlobalClock.getCurrentTime() / core.getStepSize()
							+ "\n" + ins + "\n");
				}
				idExRS.rs[rsid].executionComplete = true;
			} else {
				break;
			}
		}
		System.out.println();
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

	public static int getSize() {
		return ReservationStation.getRSSize()/2;
	}
}
