package pipeline.inorder;


import java.util.Hashtable;

import memorysystem.AddressCarryingEvent;
import memorysystem.InstructionCache;

import config.SimulationConfig;
import config.SystemConfig;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.Instruction;
import generic.InstructionLinkedList;
import generic.OMREntry;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;

public class FetchUnitIn extends SimulationElement{
	Core core;
	Instruction fetchBuffer[];
	public int fetchBufferCapacity;
	private int fetchFillCount;	//Number of instructions in the fetch buffer
	private int fetchBufferIndex;	//Index to first instruction to be popped out of fetch buffer
	private int stall;
	private boolean sleep;		//The boolean to stall the pipeline when a sync request is received
	InstructionLinkedList inputToPipeline;
	EventQueue eventQueue;
	int syncCount;
	int numRequestsSent;
	int numRequestsAcknowledged;
	Hashtable<Long,OMREntry> missStatusHoldingRegister;


	public FetchUnitIn(Core core, EventQueue eventQueue) {
		super(PortType.Unlimited, -1, -1, -1, -1);
		this.core = core;
		this.fetchBuffer = new Instruction[4];
		this.fetchFillCount=0;
		this.fetchBufferIndex=0;
		this.fetchBufferCapacity=4;
		this.stall = 0;
		this.eventQueue = eventQueue;
		this.sleep=false;
		this.syncCount=0;
		this.missStatusHoldingRegister = new Hashtable<Long,OMREntry>();
		this.numRequestsSent=0;
		this.numRequestsAcknowledged=0;
		
	}

	public Hashtable<Long, OMREntry> getMissStatusHoldingRegister() {
		return missStatusHoldingRegister;
	}

	public void setMissStatusHoldingRegister(
			Hashtable<Long, OMREntry> missStatusHoldingRegister) {
		this.missStatusHoldingRegister = missStatusHoldingRegister;
	}	
	public void fillFetchBuffer(){
//System.out.println("inside fill fetch buffer "+inputToPipeline.getListSize());
		if(inputToPipeline.isEmpty())
			return;
		Instruction newInstruction = inputToPipeline.peekInstructionAt(0);
		for(int i=(this.fetchBufferIndex+this.fetchFillCount)%this.fetchBufferCapacity;
					this.fetchFillCount + this.numRequestsSent - this.numRequestsAcknowledged <this.fetchBufferCapacity; 
					i = (i+1)%this.fetchBufferCapacity){
			if(!SimulationConfig.detachMemSys){//TODO is the following check required ?
				if(inputToPipeline.length() > this.numRequestsSent - this.numRequestsAcknowledged)
				this.core.getExecutionEngineIn().coreMemorySystem.issueRequestToInstrCache(
						core.getExecutionEngineIn().getFetchUnitIn(), 
						inputToPipeline.peekInstructionAt(this.numRequestsSent - this.numRequestsAcknowledged).getProgramCounter(),
						this.core.getCore_number());
				this.numRequestsSent++;
				}
				else{
					handleEvent(null, null);					
				}
			}
	}
	public void performFetch(){
		if(!core.getExecutionEngineIn().getFetchComplete())
			fillFetchBuffer();

		Instruction ins;
		StageLatch ifIdLatch = core.getExecutionEngineIn().getIfIdLatch();
//System.out.println(this.sleep+" "+this.stall);
		
			if(!this.sleep && this.fetchFillCount > 0 && this.stall==0 && ifIdLatch.getStallCount()==0 && missStatusHoldingRegister.isEmpty()){
					ins = this.fetchBuffer[this.fetchBufferIndex];
	//System.out.println("Fetch "+ins.getSerialNo());			
					if(ins.getOperationType()==OperationType.sync){
						this.fetchFillCount--;			
						this.fetchBufferIndex = (this.fetchBufferIndex+1)%this.fetchBufferCapacity;
						ins = this.fetchBuffer[fetchBufferIndex];
						if(this.syncCount>0){
							this.syncCount--;
						}
						else{
							core.getExecutionEngineIn().getIfIdLatch().setInstruction(null);
							sleepThePipeline();
							return;
						}
					}
					else{
						core.getExecutionEngineIn().getIfIdLatch().setInstruction(ins);
						this.fetchFillCount--;			
						this.fetchBufferIndex = (this.fetchBufferIndex+1)%this.fetchBufferCapacity;
					}
			}
			if(ifIdLatch.getStallCount()>0){
				ifIdLatch.decrementStallCount();
			}
			if(this.stall>0){
	//			core.getExecutionEngineIn().getIfIdLatch().setInstruction(null);
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
		//System.out.println("sleeping ..!!");
		this.syncCount--;
		this.sleep=true;
	}
	
	public InstructionLinkedList getInputToPipeline(){
		return this.inputToPipeline;
	}
	public void setInputToPipeline(InstructionLinkedList inpList){
		this.inputToPipeline = inpList;
	}
	public void resumePipeline(){
//System.out.println("Inside Inorder :: Resuming the pipeline");
		this.syncCount++;
		this.sleep=false;
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		long address = ((AddressCarryingEvent)event).getAddress();
		InstructionCache iCache = (InstructionCache)event.getRequestingElement();
		//OMREntry omrEntry = missStatusHoldingRegister.remove(address >> iCache.blockSizeBits);
		//int numOfOutStandingRequest = omrEntry.outStandingEvents.size();
		//for(int i=0;i<numOfOutStandingRequest;i++)
//		{
			if(inputToPipeline.isEmpty())
			{
				return;
			}
			Instruction newInstruction = inputToPipeline.peekInstructionAt(0);
			if(newInstruction.getOperationType() == OperationType.inValid){
				core.getExecutionEngineIn().setFetchComplete(true);
				this.fetchBuffer[(this.fetchBufferIndex+this.fetchFillCount)%this.fetchBufferCapacity] = inputToPipeline.pollFirst();
				this.fetchFillCount++;
				this.numRequestsAcknowledged++;
				return;
			}
			else{
				this.fetchBuffer[(this.fetchBufferIndex+this.fetchFillCount)%this.fetchBufferCapacity]= inputToPipeline.pollFirst();
				this.fetchFillCount++;
				this.numRequestsAcknowledged++;
			}
//		}
		// TODO Auto-generated method stub
		//This should be called when the pipeline needs to wake up
//		this.sleep=false;
		
	}

}
