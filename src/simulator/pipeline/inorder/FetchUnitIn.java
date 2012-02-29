package pipeline.inorder;


import config.SimulationConfig;
import config.SystemConfig;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.Instruction;
import generic.InstructionLinkedList;
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
	}
	
	public void fillFetchBuffer(){
//System.out.println("inside fill fetch buffer "+inputToPipeline.getListSize());
		if(inputToPipeline.isEmpty())
			return;
		Instruction newInstruction = inputToPipeline.peekInstructionAt(0);
		for(int i=(this.fetchBufferIndex+this.fetchFillCount)%this.fetchBufferCapacity;this.fetchFillCount<this.fetchBufferCapacity;i = (i+1)%this.fetchBufferCapacity){
			if(newInstruction.getOperationType() == OperationType.inValid){
				core.getExecutionEngineIn().setFetchComplete(true);
				this.fetchBuffer[i] = inputToPipeline.pollFirst();
				this.fetchFillCount++;
				break;
			}
			else
			{	
				this.fetchBuffer[i] = inputToPipeline.pollFirst();
				this.fetchFillCount++;
//System.out.println("Serial Num Fetch = "+fetchBuffer[i].getSerialNo());
				if(!inputToPipeline.isEmpty())
					newInstruction = inputToPipeline.peekInstructionAt(0);
				else
					break;

				//TODO add handle fun in getdecodeunit. What happens if icache miss ? stalls not taken account for right now.

				if(!SimulationConfig.detachMemSys){
				this.core.getExecutionEngineIn().coreMemorySystem.issueRequestToInstrCache(
						core.getExecutionEngineIn().getDecodeUnitIn(), 
						this.fetchBuffer[i].getProgramCounter());
				}
			}
		}

	}
	public void performFetch(){
		if(!core.getExecutionEngineIn().getFetchComplete())
			fillFetchBuffer();

		Instruction ins;
//System.out.println(this.sleep+" "+this.stall);
		if(!this.sleep && this.stall==0){
			if(this.fetchFillCount > 0){
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
			else{
//				core.getExecutionEngineIn().getIfIdLatch().setInstruction(null);
			}
		}
		else if(this.stall>0){
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
		// TODO Auto-generated method stub
		//This should be called when the pipeline needs to wake up
//		this.sleep=false;
		
	}

}
