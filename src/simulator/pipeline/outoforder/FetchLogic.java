package pipeline.outoforder;

import config.SimulationConfig;
import main.CustomObjectPool;
import memorysystem.AddressCarryingEvent;
import generic.Barrier;
import generic.BarrierTable;
import generic.Core;
import generic.CustomInstructionPool;
import generic.Event;
import generic.EventQueue;
import generic.GenericCircularQueue;
import generic.Instruction;
import generic.InstructionLinkedList;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;

public class FetchLogic extends SimulationElement {
	
	Core core;
	OutOrderExecutionEngine execEngine;
	ICacheBuffer iCacheBuffer;
	Instruction[] fetchBuffer;
	int fetchWidth;
	int inputPipeToReadNext;
	GenericCircularQueue<Instruction>[] inputToPipeline;
	boolean sleep;
	
	OperationType[] instructionsToBeDropped;
	int invalidCount;

	public FetchLogic(Core core, OutOrderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1, core.getEventQueue(), -1, -1);
		this.core = core;
		this.execEngine = execEngine;
		//iCacheBuffer = execEngine.getiCacheBuffer();
		fetchBuffer = execEngine.getFetchBuffer();
		fetchWidth = core.getDecodeWidth();
		inputPipeToReadNext = 0;
		sleep = false;

		instructionsToBeDropped = new OperationType[] {
															OperationType.interrupt,
															OperationType.sync
													};
		
		invalidCount = 0;
	}
	
	public void performFetch()
	{
		boolean checkTranslatorSpeed = false;
		
		if(checkTranslatorSpeed)
		{
			Instruction inst;
			while((inst = inputToPipeline[0].dequeue()) != null)
			{
				if(inst.getOperationType() == OperationType.inValid)
				{
					execEngine.setExecutionComplete(true);
				}
				CustomObjectPool.getInstructionPool().returnObject(inst);
			}
			
			return;
		}
		
		if(sleep == true)
		{
			return;
		}
		
		Instruction newInstruction;
		
		//this loop reads from inputToPipeline and places the instruction in iCacheBuffer
		//fetch of the instruction is also issued to the iCache
		for(int i = 0; i < iCacheBuffer.size; i++)
		{
			if(inputToPipeline[inputPipeToReadNext].size() <= 0)
			{
				break;
			}
			
			newInstruction = inputToPipeline[inputPipeToReadNext].peek(0);
			
			//process sync operation
			if(newInstruction.getOperationType() == OperationType.sync){
				long barrierAddress = newInstruction.getRISCProgramCounter();
				System.out.println(barrierAddress);
				Barrier bar = BarrierTable.barrierList.get(barrierAddress);
				bar.incrementThreads();
				
				if(bar.timeToCross())
				{
					System.out.println("Time to cross");
					setSleep(true);
					for(int j=0; j<bar.getNumThreads(); j++ ){
						this.core.coreBcastBus.addToResumeCore(bar.getBlockedThreads().elementAt(j));
					}
					BarrierTable.barrierReset(barrierAddress);
					this.core.coreBcastBus.getPort().put(new AddressCarryingEvent(
							this.core.eventQueue,
							 1,
							 this.core.coreBcastBus, 
							 this.core.coreBcastBus, 
							 RequestType.PIPELINE_RESUME, 
							 0));

				}
				else
				{
					setSleep(true);
					//return;
				}
			}
			
			//drop instructions on the drop list
			if(shouldInstructionBeDropped(newInstruction) == true)
			{
				inputToPipeline[inputPipeToReadNext].pollFirst();
				CustomObjectPool.getInstructionPool().returnObject(newInstruction);
				i--;
				continue;
			}
			
			//drop memory operations if specified in configuration file
			if(newInstruction.getOperationType() == OperationType.load ||
					newInstruction.getOperationType() == OperationType.store)
			{
				if(SimulationConfig.detachMemSys == true)
				{
					inputToPipeline[inputPipeToReadNext].pollFirst();
					CustomObjectPool.getInstructionPool().returnObject(newInstruction);
					i--;
					continue;
				}
			}
			
			if(!iCacheBuffer.isFull() && !execEngine.getCoreMemorySystem().getiMSHR().isFull())
			{
				iCacheBuffer.addToBuffer(inputToPipeline[inputPipeToReadNext].pollFirst());
				if(SimulationConfig.detachMemSys == false && newInstruction.getOperationType() != OperationType.inValid)
				{
						execEngine.getCoreMemorySystem().issueRequestToInstrCache(newInstruction.getRISCProgramCounter());
				}
			}
			else
			{
				break;
			}
		}
		
		if(!execEngine.isToStall1() &&
				!execEngine.isToStall2() &&
				!execEngine.isToStall3() &&
				!execEngine.isToStall4() &&
				!execEngine.isToStall5())
		{
			int fetchBufferIndex = 0;
			
			//add instructions, for whom "fetch" from iCache has completed, to fetch buffer
			//decode stage reads from this buffer
			for(int i = 0; i < fetchWidth; i++)
			{
				newInstruction = iCacheBuffer.getNextInstruction();
				if(newInstruction != null)
				{
					fetchBuffer[fetchBufferIndex++] = newInstruction;
				}
				else
				{
					this.core.getExecEngine().incrementInstructionMemStall(1); 
					break;
				}
			}
			
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
	
	boolean shouldInstructionBeDropped(Instruction instruction)
	{
		for(int i = 0; i < instructionsToBeDropped.length; i++)
		{
			if(instructionsToBeDropped[i] == instruction.getOperationType())
			{
				return true;
			}
		}
		return false;
	}
	
	public void processCompletionOfMemRequest(long address)
	{
		iCacheBuffer.updateFetchComplete(address);
	}
	
	public GenericCircularQueue<Instruction>[] getInputToPipeline() {
		return inputToPipeline;
	}

	public void setInputToPipeline(GenericCircularQueue<Instruction>[] inputToPipeline) {
		this.inputToPipeline = inputToPipeline;
	}

	public void setICacheBuffer(ICacheBuffer iCacheBuffer)
	{
		this.iCacheBuffer = iCacheBuffer;
	}

	public boolean isSleep() {
		return sleep;
	}

	public void setSleep(boolean sleep) {
		if(sleep == true)
			System.out.println("sleeping pipeline " + this.core.getCore_number());
		else
			System.out.println("resuming pipeline " + this.core.getCore_number());
		this.sleep = sleep;
	}
}
