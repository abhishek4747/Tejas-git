package pipeline.inorder;


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
		//TODO Request iCache for instructions
		if(inputToPipeline.isEmpty())
			return;
		Instruction newInstruction = inputToPipeline.peekInstructionAt(0);
//System.out.println(fetchFillCount);	
		for(int i=fetchBufferIndex;fetchFillCount<fetchBufferCapacity;i = (i+1)%fetchBufferCapacity){
			if(newInstruction.getOperationType() == OperationType.inValid){
//System.out.println("sleep= "+sleep+" Total instructions = "+core.getNoOfInstructionsExecuted());
				core.getExecutionEngineIn().setExecutionComplete(true);
				break;
			}
			else
			{	
				fetchBuffer[i] = inputToPipeline.pollFirst();

				if(!inputToPipeline.isEmpty())
					newInstruction = inputToPipeline.peekInstructionAt(0);
				else
					break;
/*				this.core.getExecutionEngineIn().getCoreMemorySystem().getiCache().getPort().put(
						new AddressCarryingEvent(
								this.eventQueue,
								this.core.getExecutionEngineIn().getCoreMemorySystem().getiCache().getLatencyDelay(),
								core.getExecutionEngineIn().getDecodeUnitIn(),
								core.getExecutionEngineIn().getCoreMemorySystem().getiCache(), 
								RequestType.Cache_Read,
								fetchBuffer[i].getProgramCounter())); //What address to send ??
*/
				//TODO add handle fun in getdecodeunit. What happens if icache miss ? stalls not taken account for right now.

				this.core.getExecutionEngineIn().coreMemorySystem.issueRequestToInstrCache(
						core.getExecutionEngineIn().getDecodeUnitIn(), 
						fetchBuffer[i].getProgramCounter());
//System.out.println(fetchBuffer[i].getProgramCounter());

				fetchFillCount++;
			}
		}

	}
	public void performFetch(){
		if(!core.getExecutionEngineIn().getExecutionComplete())
			fillFetchBuffer();

//		StageLatch wbDoneLatch = core.getInorderPipeline().getWbDoneLatch();
//		StageLatch ifIdLatch = core.getInorderPipeline().getIfIdLatch();
		Instruction ins;
		if(!this.sleep && this.stall==0 && !core.getExecutionEngineIn().getExecutionComplete()){
			if(fetchFillCount > 0){
				ins = fetchBuffer[fetchBufferIndex];
				if(ins.getOperationType()==OperationType.sync){
					fetchFillCount--;			
					fetchBufferIndex = (fetchBufferIndex+1)%fetchBufferCapacity;
					ins = fetchBuffer[fetchBufferIndex];
//System.out.println("Inside Inorder :: Sync Encountered");
					if(this.syncCount>0){
						this.syncCount--;
					}
					else{
//System.out.println("Inside Inorder :: Sleeping the pipeline");
						core.getExecutionEngineIn().getIfIdLatch().setInstruction(null);
						sleepThePipeline();
						return;
					}
				}
				core.getExecutionEngineIn().getIfIdLatch().setInstruction(ins);
				fetchFillCount--;			
				fetchBufferIndex = (fetchBufferIndex+1)%fetchBufferCapacity;
			
			}
			else{
				core.getExecutionEngineIn().getIfIdLatch().setInstruction(null);
//				wbDoneLatch.decrementStallCount();
//				ifIdLatch.incrementStallCount();
//				decrementStall(1);
//				core.getInorderPipeline().getIfIdLatch().setInstruction(null);
			}
		}
		else if(this.stall>0){
			core.getExecutionEngineIn().getIfIdLatch().setInstruction(null);
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
