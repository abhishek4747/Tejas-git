package pipeline.inorder;

import memorysystem.AddressCarryingEvent;
import config.SimulationConfig;
import generic.Barrier;
import generic.BarrierTable;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GenericCircularQueue;
import generic.GlobalClock;
import generic.Instruction;
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
		this.fetchBufferStatus = new boolean[this.fetchBufferCapacity];  // To check whether request to ICache is complete or not
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
			
			if(containingExecutionEngine.inorderCoreMemorySystem.getiCache().getMissStatusHoldingRegister().getCurrentSize() >= containingExecutionEngine.inorderCoreMemorySystem.getiCache().getMissStatusHoldingRegister().getMSHRStructSize()){
				break;
			}
			
			newInstruction = inputToPipeline.pollFirst();
			
			if(newInstruction == null)
				return;
			numRequestsSent++;
			if(newInstruction.getOperationType() == OperationType.inValid) {
				this.fetchBuffer[i] = newInstruction;
				this.fetchBufferStatus[i]=true;
				this.fetchFillCount++;
			}
			
			else
			{
				this.fetchBuffer[i]= newInstruction;
				this.fetchFillCount++;

				// The first micro-operation of an instruction has a valid CISC IP. All the subsequent 
				// micro-ops will have IP = -1(meaning invalid). We must not forward this requests to iCache.
				if(SimulationConfig.detachMemSys || newInstruction.getCISCProgramCounter()==-1)
				{
					this.fetchBufferStatus[i]=true;
				}
				else
				{
					this.fetchBufferStatus[i]=false;
					containingExecutionEngine.inorderCoreMemorySystem.issueRequestToInstrCache(
							newInstruction.getCISCProgramCounter());
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
						long barrierAddress = ins.getCISCProgramCounter();
						Barrier bar = BarrierTable.barrierList.get(barrierAddress);
						bar.incrementThreads();
						if(this.core.TreeBarrier == true){
							setSleep(true);
							int coreId = this.core.getCore_number();
							this.core.coreBcastBus.getPort().put(new AddressCarryingEvent(
									this.core.eventQueue,
									 this.core.barrier_latency,
									 this.core.coreBcastBus, 
									 this.core.coreBcastBus, 
									 RequestType.TREE_BARRIER, 
									 barrierAddress,
									 coreId));
						}
						else{
							if(bar.timeToCross())
							{
								ifIdLatch.setInstruction(null);
								sleepThePipeline();
								int bar_lat;
								
								if(this.core.barrierUnit == 0){
									if(GlobalClock.getCurrentTime() < bar.time + 35)
									{
										bar_lat = (int)(this.core.barrier_latency + GlobalClock.getCurrentTime() - bar.time);
									}
									else
										bar_lat = this.core.barrier_latency;
								}
								else{
									if(GlobalClock.getCurrentTime() < bar.time + 4)
									{
										bar_lat = (int)(this.core.barrier_latency + GlobalClock.getCurrentTime() - bar.time);
									}
									else
										bar_lat = this.core.barrier_latency;
								}
								for(int i=0; i<bar.getNumThreads(); i++ ){
									this.core.coreBcastBus.addToResumeCore(bar.getBlockedThreads().elementAt(i));
								}
								this.core.coreBcastBus.getPort().put(new AddressCarryingEvent(
										 this.core.eventQueue,
										 bar_lat,
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
						}
						ifIdLatch.setInstruction(null);
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
					this.fetchBuffer[i].getCISCProgramCounter() == requestedAddress && 
					this.fetchBufferStatus[i]==false){
				this.fetchBufferStatus[i]=true;
			}
		}
	}

}
