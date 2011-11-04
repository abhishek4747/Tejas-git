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
	InstructionLinkedList inputToPipeline;
	EventQueue eventQueue;

	public FetchUnitIn(Core core, EventQueue eventQueue) {
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		this.fetchBuffer = new Instruction[4];
		this.fetchFillCount=0;
		this.fetchBufferIndex=0;
		this.fetchBufferCapacity=4;
		this.stall = 0;
		this.eventQueue = eventQueue;
	}
	
	public void fillFetchBuffer(){
//System.out.println("inside fill fetch buffer "+inputToPipeline.getListSize());
		//TODO Request iCache for instructions
		Instruction newInstruction = inputToPipeline.peekInstructionAt(0);
	
		for(int i=fetchBufferIndex;fetchFillCount<fetchBufferCapacity;i = (i+1)%fetchBufferCapacity){
			if(newInstruction.getOperationType() == OperationType.inValid){
				core.getExecutionEngineIn().setExecutionComplete(true);
				break;
			}
			else
			{	
				fetchBuffer[i] = inputToPipeline.pollFirst();

				newInstruction = inputToPipeline.peekInstructionAt(0);
/*				this.core.getExecutionEngineIn().getCoreMemorySystem().getiCache().getPort().put(
						new AddressCarryingEvent(
								this.eventQueue,
								this.core.getExecutionEngineIn().getCoreMemorySystem().getiCache().getLatencyDelay(),
								core.getExecutionEngineIn().getDecodeUnitIn(),//TODO add handle fun in getdecodeunit
								core.getExecutionEngineIn().getCoreMemorySystem().getiCache(),//TODO FIXME 
								RequestType.Cache_Read,
								fetchBuffer[i].getProgramCounter())); //What address to send ??
*/

//				this.core.getExecutionEngineIn().getCoreMemorySystem().issueRequestToInstrCache(
//						core.getExecutionEngineIn().getDecodeUnitIn(), 
//						fetchBuffer[i].getProgramCounter());

				fetchFillCount++;
			}
		}

	}
	public void performFetch(){
		fillFetchBuffer();

//		StageLatch wbDoneLatch = core.getInorderPipeline().getWbDoneLatch();
//		StageLatch ifIdLatch = core.getInorderPipeline().getIfIdLatch();
System.out.println(this.stall+" "+fetchFillCount+" "+fetchBufferIndex);

//		if(!core.getExecutionEngineIn().getExecutionComplete()){
		if(this.stall==0 && !core.getExecutionEngineIn().getExecutionComplete()){
			if(fetchFillCount > 0){
				core.getExecutionEngineIn().getIfIdLatch().setInstruction(fetchBuffer[fetchBufferIndex]);
				fetchFillCount--;			//TODO synchronize this ? with fetch buffer filling
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
	public InstructionLinkedList getInputToPipeline(){
		return this.inputToPipeline;
	}
	public void setInputToPipeline(InstructionLinkedList inpList){
		this.inputToPipeline = inpList;
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}

}
