package pipeline.inorder;

import pipeline.outoforder.OpTypeToFUTypeMapping;
import config.SimulationConfig;
import memorysystem.CoreMemorySystem;
import memorysystem.MemorySystem;
import memorysystem.TLB;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.FunctionalUnitType;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;

public class ExecUnitIn extends SimulationElement{
	Core core;
	EventQueue eventQueue;
	public ExecUnitIn(Core core) {
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		this.eventQueue = core.getEventQueue();
		// TODO Auto-generated constructor stub
	}
	
	public void execute(InorderPipeline inorderPipeline){
		StageLatch exMemLatch = inorderPipeline.getExMemLatch();
		StageLatch idExLatch = inorderPipeline.getIdExLatch();
		StageLatch ifIdLatch = inorderPipeline.getIfIdLatch();
		Instruction ins = idExLatch.getInstruction();
//		if(exMemLatch.getStallCount()>0){
//			exMemLatch.decrementStallCount();
//			idExLatch.incrementStallCount();
//		}
		if(idExLatch.getStallCount()>0){
//System.out.println("Not executing "+idExLatch.getStallCount());
			idExLatch.decrementStallCount(1);
			ifIdLatch.setStallCount(1);
			return;
		}
		else{
			if(ins!=null){
					//TODO Account for multicycle operations.
	//System.out.println("Exec "+ins.getSerialNo());	
				long FURequest = 0;	//will be <= 0 if an FU was obtained
				//will be > 0 otherwise, indicating how long before
				//	an FU of the type will be available (not used in new arch)
				long lat=0;
				long currentTime=GlobalClock.getCurrentTime();
				if(OpTypeToFUTypeMapping.getFUType(ins.getOperationType())!=FunctionalUnitType.memory
						&& OpTypeToFUTypeMapping.getFUType(ins.getOperationType())!=FunctionalUnitType.inValid){
								
					FURequest = this.core.getExecutionEngineIn().getFunctionalUnitSet().requestFU(
						OpTypeToFUTypeMapping.getFUType(ins.getOperationType()),
						currentTime,
						core.getStepSize() );
				
					lat = this.core.getExecutionEngineIn().getFunctionalUnitSet().getFULatency(
							OpTypeToFUTypeMapping.getFUType(ins.getOperationType()));
					
					if(FURequest >0){
						//FU is not available
						//Set the appropriate pipelines to stall due to execute for FURequest/core.getStepSize() number of cycles!!
						//Execute for this pipeline will be stalled for timeWhenAvailable-1 cycles where as for others
						//it will be stalled for timeWhenAvailable cycles
						//Also stall the decode of this pipeline for one cycle 
						//(later cycle stalls will eventually propagate from execute stage itself)
						int delayCycles = (int)(FURequest-currentTime)/core.getStepSize();
						this.core.getExecutionEngineIn().setStallFetch(delayCycles);
						this.core.getExecutionEngineIn().setStallPipelinesExecute(inorderPipeline.getId(), delayCycles);
						idExLatch.setStallCount(delayCycles-1);
						ifIdLatch.setStallCount(1);
						return;
					}
					if(lat>1){
						//If it is a multicycle operation, stall appropriate pipelines for rest of the cycles
						this.core.getExecutionEngineIn().setStallFetch((int)lat-1);
						core.getExecutionEngineIn().setStallPipelinesExecute(inorderPipeline.getId(), (int)lat-1);
						//Also stall the decode of this pipeline
						ifIdLatch.setStallCount((int)lat-1);
					}
				}
/*				else if(OpTypeToFUTypeMapping.getFUType(ins.getOperationType())==FunctionalUnitType.memory){
					//Search TLB for address hit
					TLB TlbBuffer = this.core.getExecutionEngineIn().coreMemorySystem.TLBuffer;
					boolean TLBHit=TlbBuffer.searchTLBForPhyAddr(ins.getSourceOperand1().getValue());
					if(!TLBHit){
						this.core.getExecutionEngineIn().incrementStallFetch(TlbBuffer.getMissPenalty());
						core.getExecutionEngineIn().setStallPipelinesExecute(inorderPipeline.getId(),TlbBuffer.getMissPenalty());
						return;
					}
						
				}
*/
					if(ins.getOperationType()==OperationType.load){
						core.getExecutionEngineIn().updateNoOfLd(1);
						core.getExecutionEngineIn().updateNoOfMemRequests(1);
										
						//Schedule a mem read event now so that it can be completed in the mem stage
						if(!SimulationConfig.detachMemSys){
							exMemLatch.setMemDone(false);
			
							this.core.getExecutionEngineIn().coreMemorySystem.issueRequestToL1Cache(
									RequestType.Cache_Read,
									ins.getSourceOperand1().getValue(),
									inorderPipeline);
						}
					}
					else if(ins.getOperationType()==OperationType.store){
						core.getExecutionEngineIn().updateNoOfSt(1);
						core.getExecutionEngineIn().updateNoOfMemRequests(1);
						exMemLatch.setMemDone(true); //FIXME Pipeline doesn't wait for the store to complete! 
						
						//Schedule a mem read event now so that it can be completed in the mem stage
						if(!SimulationConfig.detachMemSys){
							this.core.getExecutionEngineIn().coreMemorySystem.issueRequestToL1Cache(
									RequestType.Cache_Write,
									ins.getSourceOperand1().getValue(),
									inorderPipeline);
						}
					}
					else{
						exMemLatch.setMemDone(true);
					}
					if(ins.getOperationType()==OperationType.floatDiv ||
							ins.getOperationType()==OperationType.floatMul ||
							ins.getOperationType()==OperationType.floatALU ||
							ins.getOperationType()==OperationType.integerALU ||
							ins.getOperationType()==OperationType.integerDiv ||
							ins.getOperationType()==OperationType.integerMul)
						this.core.getExecutionEngineIn().getDestRegisters().remove(ins.getDestinationOperand());
										
					exMemLatch.setInstruction(ins);
					exMemLatch.setIn1(idExLatch.getIn1());
					exMemLatch.setIn2(idExLatch.getIn2());
					exMemLatch.setOut1(idExLatch.getOut1());
					exMemLatch.setOperationType(idExLatch.getOperationType());
//					exMemLatch.setMemDone(true);
					exMemLatch.setLoadFlag(idExLatch.getLoadFlag());
					
					idExLatch.clear();

				}
				else{
	//				exMemLatch.setInstruction(null);
				}
		}
	}
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}
}
