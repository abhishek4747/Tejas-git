package pipeline.statistical;

import memorysystem.CoreMemorySystem;
import generic.Core;
import generic.Event;
import generic.Instruction;
import generic.InstructionLinkedList;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class FetchEngine extends SimulationElement
{
	//TODO Fetch a random number of instructions
	Core core;
	
	StatisticalPipeline pipeline;
	
	private Instruction[] fetchBuffer;
	int fetchWidth;
	int inputPipeToReadNext;
	InstructionLinkedList[] inputToPipeline;
	
	public CoreMemorySystem coreMemSys;
	
	private boolean toStall;
	
	private boolean isExecutionComplete;		//TRUE indicates end of simulation
	private boolean isInputPipeEmpty[];
	private boolean allPipesEmpty;

	public FetchEngine(Core core, StatisticalPipeline pipeline)
	{
		super(PortType.Unlimited, -1, -1, core.getEventQueue(), -1, -1);
		this.core = core;
		this.pipeline = pipeline;
		fetchBuffer = pipeline.getFetchBuffer();
		fetchWidth = core.getDecodeWidth();
		inputPipeToReadNext = 0;
		
		isExecutionComplete = false;
		isInputPipeEmpty = new boolean[core.getNo_of_input_pipes()];
		allPipesEmpty = false;
	}
	
	/*
	 * if decoder consumed all of the fetchBuffer in the previous cycle,
	 * 		fetch decodeWidth more instructions
	 * else
	 * 		stall fetch
	 */
	public void performFetch()
	{
		Instruction newInstruction;
		
		if(!toStall)
		{
			int ctr = 0;
			while(isInputPipeEmpty(inputPipeToReadNext) == true
					&& ctr < core.getNo_of_input_pipes())
			{
				ctr++;
			}
			
			if(ctr == core.getNo_of_input_pipes())
			{
				setAllPipesEmpty(true);
			}
			else
			{
				if (core.isPipelineStatistical == true)
				{
					//readDecodePipe();
				}
				else
				{
					for(int i = 0; i < fetchWidth; i++)
					{
						newInstruction = inputToPipeline[inputPipeToReadNext].peekInstructionAt(0);
						if(newInstruction.getOperationType() == OperationType.inValid)
						{
							setInputPipeEmpty(inputPipeToReadNext, true);
							break;
						}
						else
						{
							fetchBuffer[i] = inputToPipeline[inputPipeToReadNext].pollFirst();
						}
					}
				}
			}
		}
		
		if(isAllPipesEmpty() == false)
		{
			inputPipeToReadNext = (inputPipeToReadNext + 1)%core.getNo_of_input_pipes();			
		}
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean isInputPipeEmpty(int threadIndex) {
		return isInputPipeEmpty[threadIndex];
	}

	public void setInputPipeEmpty(int threadIndex, boolean isInputPipeEmpty) {
		this.isInputPipeEmpty[threadIndex] = isInputPipeEmpty;
	}

	public boolean isAllPipesEmpty() {
		return allPipesEmpty;
	}

	public void setAllPipesEmpty(boolean allPipesEmpty) {
		this.allPipesEmpty = allPipesEmpty;
	}
	
	public InstructionLinkedList[] getInputToPipeline() {
		return inputToPipeline;
	}

	public void setInputToPipeline(InstructionLinkedList[] inputToPipeline) {
		this.inputToPipeline = inputToPipeline;
	}

	public boolean isToStall() {
		return toStall;
	}

	public void setToStall(boolean toStall) {
		this.toStall = toStall;
	}

}
