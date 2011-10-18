package pipeline.outoforder_new_arch;

import generic.Core;
import generic.Event;
import generic.Instruction;
import generic.InstructionLinkedList;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class FetchLogic extends SimulationElement {
	
	Core core;
	ExecutionEngine execEngine;
	Instruction[] fetchBuffer;
	int fetchWidth;
	int inputPipeToReadNext;
	InstructionLinkedList[] inputToPipeline;

	public FetchLogic(Core core, ExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1, core.getEventQueue(), -1, -1);
		this.core = core;
		this.execEngine = execEngine;
		fetchBuffer = execEngine.getFetchBuffer();
		fetchWidth = core.getDecodeWidth();
		inputPipeToReadNext = 0;
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
		
		if(!execEngine.isToStall1() &&
				!execEngine.isToStall2() &&
				!execEngine.isToStall3() &&
				!execEngine.isToStall4() &&
				!execEngine.isToStall5())
		{
			int ctr = 0;
			while(execEngine.isInputPipeEmpty(inputPipeToReadNext) == true
					&& ctr < core.getNo_of_input_pipes())
			{
				ctr++;
			}
			
			if(ctr == core.getNo_of_input_pipes())
			{
				execEngine.setAllPipesEmpty(true);
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
							execEngine.setInputPipeEmpty(inputPipeToReadNext, true);
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
		
		if(execEngine.isAllPipesEmpty() == false)
		{
			inputPipeToReadNext = (inputPipeToReadNext + 1)%core.getNo_of_input_pipes();			
		}
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}
	
	public InstructionLinkedList[] getInputToPipeline() {
		return inputToPipeline;
	}

	public void setInputToPipeline(InstructionLinkedList[] inputToPipeline) {
		this.inputToPipeline = inputToPipeline;
	}

}
