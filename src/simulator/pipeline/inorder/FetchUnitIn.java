package pipeline.inorder;


import generic.Core;
import generic.Event;
import generic.Instruction;
import generic.PortType;
import generic.SimulationElement;

public class FetchUnitIn extends SimulationElement{
	Core core;
	Instruction fetchBuffer[];
	public int fetchBufferCapacity;
	private int fetchFillCount;	//Number of instructions in the fetch buffer
	private int fetchBufferIndex;	//Index to first instruction to be popped out of fetch buffer
	private int stall;
	public FetchUnitIn(Core core) {
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		this.fetchBufferCapacity = 4;// TODO Take capacity from core! Set frequency appropriately
		this.fetchBuffer = new Instruction[4];
		this.fetchFillCount=0;
		this.fetchBufferIndex=0;
		this.stall = 0;
	}
	
	public void fillFetchBuffer(){
		//TODO Request iCache for instructions

	}
	public void performFetch(){
//		StageLatch wbDoneLatch = core.getInorderPipeline().getWbDoneLatch();
//		StageLatch ifIdLatch = core.getInorderPipeline().getIfIdLatch();

		if(this.stall==0){
			if(fetchFillCount > 0){
				core.getInorderPipeline().getIfIdLatch().setInstruction(fetchBuffer[fetchBufferIndex]);
				fetchFillCount--;			//TODO synchronize this ? with fetch buffer filling
				fetchBufferIndex = (fetchBufferIndex+1)%fetchBufferCapacity;
			}
			else{
//				wbDoneLatch.decrementStallCount();
//				ifIdLatch.incrementStallCount();
				decrementStall(1);
//				core.getInorderPipeline().getIfIdLatch().setInstruction(null);
			}
		}
		
	}
	public int getStall(){
		return this.stall;
	}
	public void incrementStall(int _stall){
		this.stall += _stall;
	}
	public void decrementStall(int _stall){
		this.stall -= _stall;
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}

}
