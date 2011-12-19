package pipeline.outoforder_new_arch;

import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import memorysystem.InstructionCache;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.ExecCompleteEvent;
import generic.Instruction;
import generic.InstructionLinkedList;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;

public class FetchLogic extends SimulationElement {
	
	Core core;
	ExecutionEngine execEngine;
	ICacheBuffer iCacheBuffer;
	Instruction[] fetchBuffer;
	int fetchWidth;
	int inputPipeToReadNext;
	InstructionLinkedList[] inputToPipeline;
	CoreMemorySystem coreMemSys;

	public FetchLogic(Core core, ExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1, core.getEventQueue(), -1, -1);
		this.core = core;
		this.execEngine = execEngine;
		iCacheBuffer = execEngine.getiCacheBuffer();
		fetchBuffer = execEngine.getFetchBuffer();
		fetchWidth = core.getDecodeWidth();
		inputPipeToReadNext = 0;
		coreMemSys = execEngine.coreMemSys;
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
					int fetchBufferIndex = 0;
					for(int i = 0; i < fetchWidth; i++)
					{
						newInstruction = iCacheBuffer.getNextInstruction();
						if(newInstruction != null)
						{
							fetchBuffer[fetchBufferIndex++] = newInstruction;
						}
						else
						{
							break;
						}
					}
					
					for(int i = 0; i < fetchWidth; i++)
					{
						newInstruction = inputToPipeline[inputPipeToReadNext].peekInstructionAt(0);
						
						if(newInstruction == null)
						{
							System.out.println("number of instructions fetched < width");
						}
						
						if(newInstruction.getOperationType() == OperationType.inValid)
						{
							execEngine.setInputPipeEmpty(inputPipeToReadNext, true);
							break;
						}
						else
						{
							if(!iCacheBuffer.isFull())
							{
								iCacheBuffer.addToBuffer(inputToPipeline[inputPipeToReadNext].pollFirst());
								execEngine.coreMemSys.issueRequestToInstrCache(this, newInstruction.getProgramCounter());
							}
							else
							{
								break;
							}
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
	public void handleEvent(EventQueue eventQ, Event event) {
		
		if(event.getRequestType() != RequestType.Mem_Response)
		{
			System.out.println("fetcher received some random event" + event);
			System.exit(1);
		}
		
		long fetchedPC = ((AddressCarryingEvent)event).getAddress();
		
		iCacheBuffer.updateFetchComplete(fetchedPC);
		
	}
	
	public InstructionLinkedList[] getInputToPipeline() {
		return inputToPipeline;
	}

	public void setInputToPipeline(InstructionLinkedList[] inputToPipeline) {
		this.inputToPipeline = inputToPipeline;
	}

}
