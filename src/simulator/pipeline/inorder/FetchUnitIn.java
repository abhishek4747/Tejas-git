package pipeline.inorder;


import java.util.Hashtable;

import main.Main;
import memorysystem.AddressCarryingEvent;
import config.SimulationConfig;
import generic.Barrier;
import generic.BarrierTable;
import generic.Core;
import generic.CoreBcastBus;
import generic.Event;
import generic.EventQueue;
import generic.GenericCircularQueue;
import generic.Instruction;
import generic.InstructionLinkedList;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;

public class FetchUnitIn extends SimulationElement
{
	Core core;
	InorderExecutionEngine containingExecutionEngine;
	Instruction fetchBuffer[];
	public int fetchBufferCapacity;
	private int fetchFillCount;	//Number of instructions in the fetch buffer
	private int fetchBufferIndex;	//Index to first instruction to be popped out of fetch buffer
	private int stall;
	private boolean sleep;		//The boolean to stall the pipeline when a sync request is received
	public GenericCircularQueue<Instruction> inputToPipeline;
	EventQueue eventQueue;
	int syncCount;
	long numRequestsSent;
	int numRequestsAcknowledged;
	private boolean fetchBufferStatus[];	
//	public CoreBcastBus coreBcastBus;

	public FetchUnitIn(Core core, EventQueue eventQueue, InorderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1, -1, -1);
		this.core = core;
		this.containingExecutionEngine = execEngine;
		this.fetchBufferCapacity=1;
		this.fetchBuffer = new Instruction[this.fetchBufferCapacity];
		this.fetchFillCount=0;
		this.fetchBufferIndex=0;
		this.stall = 0;
		this.eventQueue = eventQueue;
		this.sleep=false;
		this.syncCount=0;
		this.numRequestsSent=0;
		this.numRequestsAcknowledged=0;
		this.fetchBufferStatus = new boolean[this.fetchBufferCapacity];
//		this.coreBcastBus = coreBcastBus;
		for(int i=0;i<this.fetchBufferCapacity;i++)
		{
			this.fetchBufferStatus[i]=false;
		}
	}
	
	public void fillFetchBuffer(InorderPipeline inorderPipeline)
	{
		if(inputToPipeline.isEmpty())
			return;
		
		Instruction newInstruction=null;
		for(int i=(this.fetchBufferIndex+this.fetchFillCount)%this.fetchBufferCapacity;this.fetchFillCount<this.fetchBufferCapacity
				;i = (i+1)%this.fetchBufferCapacity){
			
			if( containingExecutionEngine.inorderCoreMemorySystem.getiMSHR().isFull() ){
				//System.err.println("Exiting due to size exceed");
				break;
			}
			
			newInstruction = inputToPipeline.pollFirst();//inputToPipeline.peekInstructionAt(0);
			
			if(newInstruction == null)
				return;
			numRequestsSent++;
			if(newInstruction.getOperationType() == OperationType.inValid){
				this.fetchBuffer[i] = newInstruction;//inputToPipeline.pollFirst();
						this.fetchBufferStatus[i]=true;
						this.fetchFillCount++;
//						this.numRequestsAcknowledged++;
						
//System.out.println("Size = "+inputToPipeline.getListSize()+" "+(this.fetchBufferIndex+this.fetchFillCount));
			}
			
			else
			{
				this.fetchBuffer[i]= newInstruction;
				this.fetchFillCount++;

				if(SimulationConfig.detachMemSys)
				{
					this.fetchBufferStatus[i]=true;
				}
				else
				{
					this.fetchBufferStatus[i]=false;
					containingExecutionEngine.inorderCoreMemorySystem.issueRequestToInstrCache(
							newInstruction.getRISCProgramCounter());
				}
			}
		}
	}
	
	public void performFetch(InorderPipeline inorderPipeline)
	{		
		fillFetchBuffer(inorderPipeline);
		
		Instruction ins;
		StageLatch ifIdLatch = inorderPipeline.getIfIdLatch();
			
		if(!this.fetchBufferStatus[this.fetchBufferIndex])
			containingExecutionEngine.incrementInstructionMemStall(1); 

		if(!this.sleep && this.fetchFillCount > 0 && 
				this.stall==0 && 
				containingExecutionEngine.getStallFetch()==0 
				&& this.fetchBufferStatus[this.fetchBufferIndex])
		{
					ins = this.fetchBuffer[this.fetchBufferIndex];

					if(ins.getOperationType()==OperationType.sync)
					{
						this.fetchFillCount--;
						this.fetchBufferIndex = (this.fetchBufferIndex+1)%this.fetchBufferCapacity;
						long barrierAddress = ins.getRISCProgramCounter();
						Barrier bar = BarrierTable.barrierList.get(barrierAddress);
						bar.incrementThreads();
						
						if(bar.timeToCross())
						{
							ifIdLatch.setInstruction(null);
							sleepThePipeline();
							for(int i=0; i<bar.getNumThreads(); i++ ){
								this.core.coreBcastBus.addToResumeCore(bar.getBlockedThreads().elementAt(i));
							}
							BarrierTable.barrierReset(barrierAddress);
							this.core.coreBcastBus.getPort().put(new AddressCarryingEvent(
									this.core.eventQueue,
									 1,
									 this.core.coreBcastBus, 
									 this.core.coreBcastBus, 
									 RequestType.PIPELINE_RESUME, 
									 0));

						}
						else
						{
							ifIdLatch.setInstruction(null);
							sleepThePipeline();
							return;
						}
						ins = this.fetchBuffer[fetchBufferIndex];
					}
					else{
						inorderPipeline.getIfIdLatch().setInstruction(ins);
						this.fetchFillCount--;			
						this.fetchBufferIndex = (this.fetchBufferIndex+1)%this.fetchBufferCapacity;
					}
			}
		
			if(this.stall>0){
				this.stall--;
			}
	}
	
	public int getStall(){
		return this.stall;
	}
	public void incrementStall(int _stall){
		this.stall = this.stall+_stall;
	}
	public void decrementStall(int _stall){
		this.stall = this.stall-_stall;
	}
	public void setStall(int _stall){
		this.stall = _stall;
	}
	public void setSleep(boolean _sleep){
		this.sleep=_sleep;
	}
	public boolean getSleep(){
		return this.sleep;
	}
	public void sleepThePipeline(){
		System.out.println("sleeping pipeline" + this.core.getCore_number()+ "...!!");
		this.syncCount--;
		this.sleep=true;
	}	
	public GenericCircularQueue<Instruction> getInputToPipeline(){
		return this.inputToPipeline;
	}
	public void setInputToPipeline(GenericCircularQueue<Instruction> inpList){
		this.inputToPipeline = inpList;
	}
	public void resumePipeline(){
		System.out.println("Resuming the pipeline "+this.core.getCore_number() + "...!!");
		this.syncCount++;
		this.sleep=false;
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
				
	}
	
	public void processCompletionOfMemRequest(long requestedAddress)
	{
		for(int i=0;i<this.fetchBufferCapacity;i++){
			if(this.fetchBuffer[i] != null && 
					this.fetchBuffer[i].getRISCProgramCounter() == requestedAddress && 
					this.fetchBufferStatus[i]==false){
				this.fetchBufferStatus[i]=true;
//				break;
			}
		}
	}

}
