package pipeline.outoforder;

import java.util.Hashtable;

import config.SimulationConfig;
import config.SystemConfig;
import emulatorinterface.Newmain;
import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import memorysystem.InstructionCache;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.ExecCompleteEvent;
import generic.GlobalClock;
import generic.Instruction;
import generic.InstructionLinkedList;
import generic.OMREntry;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;
import generic.Statistics;

public class FetchLogic extends SimulationElement {
	
	Core core;
	OutOrderExecutionEngine execEngine;
	ICacheBuffer iCacheBuffer;
	Instruction[] fetchBuffer;
	int fetchWidth;
	int inputPipeToReadNext;
	InstructionLinkedList[] inputToPipeline;
	CoreMemorySystem coreMemSys;
	Hashtable<Long,OMREntry> missStatusHoldingRegister;


	public FetchLogic(Core core, OutOrderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1, core.getEventQueue(), -1, -1);
		this.core = core;
		this.execEngine = execEngine;
		iCacheBuffer = execEngine.getiCacheBuffer();
		fetchBuffer = execEngine.getFetchBuffer();
		fetchWidth = core.getDecodeWidth();
		inputPipeToReadNext = 0;
		coreMemSys = execEngine.coreMemSys;
		this.missStatusHoldingRegister = new Hashtable<Long,OMREntry>();

	}
	
	/*
	 * if decoder consumed all of the fetchBuffer in the previous cycle,
	 * 		fetch decodeWidth more instructions
	 * else
	 * 		stall fetch
	 */
	public Hashtable<Long, OMREntry> getMissStatusHoldingRegister() {
		return missStatusHoldingRegister;
	}

	public void setMissStatusHoldingRegister(
			Hashtable<Long, OMREntry> missStatusHoldingRegister) {
		this.missStatusHoldingRegister = missStatusHoldingRegister;
	}	
	public void performFetch()
	{
		Instruction newInstruction;
		
		for(int i = 0; i < iCacheBuffer.size; i++)
		{
			if(inputToPipeline[inputPipeToReadNext].getListSize() <= 0)
			{
				break;
			}
			
			newInstruction = inputToPipeline[inputPipeToReadNext].peekInstructionAt(0);
			
			if(newInstruction.getOperationType() == OperationType.inValid)
			{
				execEngine.setInputPipeEmpty(inputPipeToReadNext, true);
				break;
			}
			
			if(newInstruction.getOperationType() == OperationType.load ||
					newInstruction.getOperationType() == OperationType.store)
			{
				if(SimulationConfig.detachMemSys == true)
				{
					inputToPipeline[inputPipeToReadNext].pollFirst();
					Newmain.instructionPool.returnObject(newInstruction);
					i--;
					continue;
				}
			}
			
			if(!iCacheBuffer.isFull())
			{
				iCacheBuffer.addToBuffer(inputToPipeline[inputPipeToReadNext].pollFirst());
				//System.out.println(core.getCore_number() + "\tfetched : " + newInstruction);
				if(SimulationConfig.detachMemSys == false)
				{
						execEngine.coreMemSys.issueRequestToInstrCacheFromOutofOrder(this, newInstruction.getRISCProgramCounter(),this.core.getCore_number());
				}
				//System.out.println(core.getCoreMode() + " - no of insts  : " + noOfInstructionsThisEpoch);
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
			int ctr = 0;
			while(execEngine.isInputPipeEmpty(inputPipeToReadNext) == true
					&& ctr < core.getNo_of_input_pipes())
			{
				inputPipeToReadNext = (inputPipeToReadNext + 1)%core.getNo_of_input_pipes();
				
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
						if( this.core.getExecEngine().getFetcher().getMissStatusHoldingRegister().size() >= fetchWidth){
							System.out.println("Exiting due to size exceed");
							break;
						}
						
						newInstruction = iCacheBuffer.getNextInstruction();
						if(newInstruction != null)
						{
							if(newInstruction.getOperationType() == OperationType.inValid)
							{
								execEngine.setInputPipeEmpty(inputPipeToReadNext, true);
								break;
							}
							fetchBuffer[fetchBufferIndex++] = newInstruction;
						}
						else
						{
							this.core.getExecEngine().incrementInstructionMemStall(1); 
							break;
						}
					}
					
					//this is a bad hack TODO
					for(int i = 0; i < fetchWidth; i++)
					{
						if(inputToPipeline[inputPipeToReadNext].getListSize() > i &&
								inputToPipeline[inputPipeToReadNext].peekInstructionAt(i).getOperationType()
								== OperationType.inValid)
						{
							execEngine.setInputPipeEmpty(inputPipeToReadNext, true);
							break;
						}
					}
				}
			}
		}
		
		if(execEngine.isAllPipesEmpty() == false)
		{
			inputPipeToReadNext = (inputPipeToReadNext + 1)%core.getNo_of_input_pipes();
		}
		/*
		if(core.getCore_number() == 2)
		{
			System.out.println(GlobalClock.getCurrentTime()/27
								+ "\tinsts : " + noOfInstructionsThisEpoch
								+ "\ttotal insts : " + noFetched
								+ "\tepochs : " + core.getNoOfEpochs()
								+ "\tpipe to read next : " + inputPipeToReadNext);
		}
		*/
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
