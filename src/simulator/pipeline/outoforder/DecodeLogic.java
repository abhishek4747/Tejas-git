package pipeline.outoforder;

import config.SimulationConfig;
import pipeline.outoforder.ReorderBufferEntry;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GenericCircularQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class DecodeLogic extends SimulationElement {
	
	Core core;
	OutOrderExecutionEngine execEngine;
	GenericCircularQueue<Instruction> fetchBuffer;
	GenericCircularQueue<ReorderBufferEntry> decodeBuffer;
	int decodeWidth;
	
	int invalidCount;
	
	public DecodeLogic(Core core, OutOrderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		this.execEngine = execEngine;
		fetchBuffer = execEngine.getFetchBuffer();
		decodeBuffer = execEngine.getDecodeBuffer();
		decodeWidth = core.getDecodeWidth();
	}
	
	public void performDecode()
	{
		if(execEngine.isToStall5() == true || execEngine.isToStall6() == true)
		{
			//pipeline stalled due to branch mis-prediction
			return;
		}
		
		ReorderBuffer ROB = execEngine.getReorderBuffer();
		ReorderBufferEntry newROBEntry;
		
		if(!execEngine.isToStall1() &&
				!execEngine.isToStall2())
		{
			for(int i = 0; i < decodeWidth; i++)
			{
				if(decodeBuffer.isFull() == true)
				{
					break;
				}
				
				Instruction headInstruction = fetchBuffer.peek(0);
				if(headInstruction != null)
				{
					if(ROB.isFull())
					{
						execEngine.setToStall4(true);
						break;
					}
					
					if(headInstruction.getOperationType() == OperationType.load ||
							headInstruction.getOperationType() == OperationType.store)
					{
						if(execEngine.getCoreMemorySystem().getLsqueue().isFull())
						{
							execEngine.setToStall3(true);
							break;
						}
					}
					
					newROBEntry = makeROBEntries(headInstruction);
					
					decodeBuffer.enqueue(newROBEntry);
					fetchBuffer.dequeue();
					
					if(SimulationConfig.debugMode)
					{
						System.out.println("decoded : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + headInstruction);
					}
				}
				
				execEngine.setToStall3(false);
				execEngine.setToStall4(false);
			}
		}
	}
	
	ReorderBufferEntry makeROBEntries(Instruction newInstruction)
	{
		if(newInstruction != null)
		{
			ReorderBufferEntry newROBEntry = execEngine.getReorderBuffer()
											.addInstructionToROB(
													newInstruction,
													newInstruction.getThreadID());
			
			//if load or store, make entry in LSQ
			if(newInstruction.getOperationType() == OperationType.load ||
					newInstruction.getOperationType() == OperationType.store)
			{
				boolean isLoad;
				if (newInstruction.getOperationType() == OperationType.load)
					isLoad = true;
				else
					isLoad = false;
					
				execEngine.getCoreMemorySystem().allocateLSQEntry(isLoad, 
						newROBEntry.getInstruction().getSourceOperand1MemValue(),
						newROBEntry);
			}
			
			return newROBEntry;
		}
		
		return null;
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
	}

}
