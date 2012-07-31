package pipeline.inorder;

import java.util.Hashtable;
import config.SimulationConfig;
import emulatorinterface.Newmain;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.Instruction;
import generic.InstructionLinkedList;
import generic.OMREntry;
import generic.OperationType;
import generic.PortType;
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
	private boolean fetchBufferStatus[];


	public FetchUnitIn(Core core, EventQueue eventQueue) {
		super(PortType.Unlimited, -1, -1, -1, -1);
		this.core = core;
		this.fetchBufferCapacity=8;
		this.fetchBuffer = new Instruction[this.fetchBufferCapacity];
		this.fetchFillCount=0;
		this.fetchBufferIndex=0;
		this.stall = 0;
		this.eventQueue = eventQueue;
		this.sleep=false;
		this.syncCount=0;
		this.missStatusHoldingRegister = new Hashtable<Long,OMREntry>();
		this.numRequestsSent=0;
		this.numRequestsAcknowledged=0;
		this.fetchBufferStatus = new boolean[this.fetchBufferCapacity];
		for(int i=0;i<this.fetchBufferCapacity;i++)
			this.fetchBufferStatus[i]=false;
	}

	public Hashtable<Long, OMREntry> getMissStatusHoldingRegister() {
		return missStatusHoldingRegister;
	}

	public void setMissStatusHoldingRegister(
			Hashtable<Long, OMREntry> missStatusHoldingRegister) {
		this.missStatusHoldingRegister = missStatusHoldingRegister;
	}
	
	public void fillFetchBuffer(InorderPipeline inorderPipeline){
		if(inputToPipeline.isEmpty())
			return;
		Instruction newInstruction=null;// = inputToPipeline.peekInstructionAt(0);
		for(int i=(this.fetchBufferIndex+this.fetchFillCount)%this.fetchBufferCapacity;this.fetchFillCount<this.fetchBufferCapacity
				;i = (i+1)%this.fetchBufferCapacity){
			
			if( missStatusHoldingRegister.size() >= this.fetchBufferCapacity){
				System.err.println("Exiting due to size exceed");
				break;
			}
			newInstruction = inputToPipeline.pollFirst();//inputToPipeline.peekInstructionAt(0);
			if(newInstruction == null)
				return;
			if(newInstruction.getOperationType() == OperationType.inValid){
				core.getExecutionEngineIn().setFetchComplete(true);
				this.fetchBuffer[i] = newInstruction;//inputToPipeline.pollFirst();
						this.fetchBufferStatus[i]=true;
						this.fetchFillCount++;
			}
			//else if (newInstruction.getOperationType() == OperationType.store)
			//{
			//	Newmain.instructionPool.returnObject(newInstruction);
			//	i--;
			//	continue;
			//}
			else{
				this.fetchBuffer[i]= newInstruction;//inputToPipeline.pollFirst();
				this.fetchFillCount++;

				if(SimulationConfig.detachMemSys){
					this.fetchBufferStatus[i]=true;
				}
				else{
					this.fetchBufferStatus[i]=false;
					this.core.getExecutionEngineIn().coreMemorySystem.issueRequestToInstrCache(
							newInstruction.getRISCProgramCounter(),
							inorderPipeline);
				}
			}
		}
	}
	
	public void performFetch(InorderPipeline inorderPipeline){
		if(!core.getExecutionEngineIn().getFetchComplete())
			fillFetchBuffer(inorderPipeline);
		Instruction ins;
		StageLatch ifIdLatch = inorderPipeline.getIfIdLatch();
			
		if(!this.fetchBufferStatus[this.fetchBufferIndex])
			this.core.getExecutionEngineIn().incrementInstructionMemStall(1); 
		if(!this.sleep && this.fetchFillCount > 0 && this.stall==0 && this.core.getExecutionEngineIn().getStallFetch()==0 
					&& this.fetchBufferStatus[this.fetchBufferIndex]){
					
					ins = this.fetchBuffer[this.fetchBufferIndex];
					if(ins.getOperationType()==OperationType.sync){
						this.fetchFillCount--;			
						this.fetchBufferIndex = (this.fetchBufferIndex+1)%this.fetchBufferCapacity;
						ins = this.fetchBuffer[fetchBufferIndex];
						if(this.syncCount>0){
							this.syncCount--;
						}
						else{
							ifIdLatch.setInstruction(null);
							sleepThePipeline();
							return;
						}
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
		this.syncCount++;
		this.sleep=false;
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
				
	}
	
	public void processCompletionOfMemRequest(long requestedAddress)
	{
		for(int i=0;i<this.fetchBufferCapacity;i++){
			if(this.fetchBuffer[i].getRISCProgramCounter() == requestedAddress &&
					this.fetchBufferStatus[i]==false){
				this.fetchBufferStatus[i]=true;
			}
		}
		//System.out.println("recevied : " + requestedAddress + " : " + GlobalClock.getCurrentTime());
	}

}
