package pipeline.inorder;

import memorysystem.CoreMemorySystem;
import power.Counters;
import emulatorinterface.Newmain;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;
import generic.Statistics;

public class WriteBackUnitIn extends SimulationElement{
	Core core;
	int j;
	public WriteBackUnitIn(Core core) {
		super(PortType.Unlimited, core.getNoOfRegFilePorts(), -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		this.j=0;
		// TODO Auto-generated constructor stub
	}
	
	public void performWriteBack(){
		StageLatch memWbLatch = core.getExecutionEngineIn().getMemWbLatch();
		if(memWbLatch.getInstruction()!=null){ 
			OperationType opType = memWbLatch.getInstruction().getOperationType(); 
			
			if(!(opType==OperationType.branch || opType==OperationType.jump)){
				this.core.powerCounters.incrementWindowAccess(1);
				this.core.powerCounters.incrementWindowPregAccess(1);
				this.core.powerCounters.incrementWindowWakeupAccess(1);
				this.core.powerCounters.incrementResultbusAccess(1);
			}
			
			if(!(opType==OperationType.store || opType == OperationType.branch 
					|| opType == OperationType.nop || opType == OperationType.jump)){
				this.core.powerCounters.incrementRegfileAccess(1);
			}

				
				
//System.out.println("wb "+memWbLatch.getInstruction().getSerialNo());			
			if(memWbLatch.getInstruction().getOperationType()==OperationType.inValid){
//				this.core.powerCounters.updatePowerStatsPerCycle();
//				this.core.powerCounters.clearAccessStats();
				setTimingStatistics();			
				setPerCoreMemorySystemStatistics();
				setPerCorePowerStatistics();
				core.getExecutionEngineIn().setExecutionComplete(true);
			}
			else {
				if(core.getNoOfInstructionsExecuted()%1000000==0){
					System.out.println(this.j++ + " million done");
				}
//				if (core.getNoOfInstructionsExecuted()!=memWbLatch.getInstruction().getSerialNo()) {
//System.out.println("Wrong...!"+core.getNoOfInstructionsExecuted()+"  "+memWbLatch.getInstruction().getSerialNo());
//				}
				core.incrementNoOfInstructionsExecuted();
				try {
/*					System.out.println(Newmain.instructionPool.getNumIdle()+"  "+Newmain.instructionPool.poolSize
							+"  "+Newmain.instructionPool.head
							+"  "+Newmain.instructionPool.tail);
*/					Newmain.instructionPool.returnObject(memWbLatch.getInstruction());
//					Newmain.operandPool.returnObject(memWbLatch.getIn1());
//					Newmain.operandPool.returnObject(memWbLatch.getIn2());
//					Newmain.operandPool.returnObject(memWbLatch.getOut1());
					core.numReturns++;
				} catch (Exception e) {
					e.printStackTrace();
				}
				memWbLatch.clear();

			}
		}
	}

	public void setTimingStatistics()
	{
		Statistics.setCoreCyclesTaken(core.getCoreCyclesTaken(), core.getCore_number());
		Statistics.setCoreFrequencies(core.getFrequency(), core.getCore_number());
		Statistics.setNumCoreInstructions(core.getNoOfInstructionsExecuted(), core.getCore_number());
	}
	
	public void setPerCoreMemorySystemStatistics()
	{
		CoreMemorySystem coreMemSys = core.getExecutionEngineIn().coreMemorySystem;
		Statistics.setNoOfMemRequests(coreMemSys.getLsqueue().noOfMemRequests, core.getCore_number());
		Statistics.setNoOfLoads(coreMemSys.getLsqueue().NoOfLd, core.getCore_number());
		Statistics.setNoOfStores(coreMemSys.getLsqueue().NoOfSt, core.getCore_number());
		Statistics.setNoOfL1Requests(coreMemSys.getL1Cache().noOfRequests, core.getCore_number());
		Statistics.setNoOfL1Hits(coreMemSys.getL1Cache().hits, core.getCore_number());
		Statistics.setNoOfL1Misses(coreMemSys.getL1Cache().misses, core.getCore_number());
		Statistics.setNoOfIRequests(coreMemSys.getiCache().noOfRequests, core.getCore_number());
		Statistics.setNoOfIHits(coreMemSys.getiCache().hits, core.getCore_number());
		Statistics.setNoOfIMisses(coreMemSys.getiCache().misses, core.getCore_number());
	}
	public void setPerCorePowerStatistics(){
		Statistics.setPerCorePowerStatistics(core.powerCounters, core.getCore_number());
	}
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}
}
