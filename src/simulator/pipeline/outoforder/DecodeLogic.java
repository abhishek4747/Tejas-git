package pipeline.outoforder;

import config.SimulationConfig;
import emulatorinterface.Newmain;
import pipeline.outoforder.ReorderBufferEntry;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class DecodeLogic extends SimulationElement {
	
	Core core;
	OutOrderExecutionEngine execEngine;
	Instruction[] fetchBuffer;
	ReorderBufferEntry[] decodeBuffer;
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
	
	/*
	 * if renamer consumed all of the decodeBuffer in the previous cycle,
	 * 		make ROB entries for all instructions in fetch buffer, as long as there is space
	 * 		in the ROB
	 * else
	 * 		stall decode
	 */
	public void performDecode()
	{
		if(execEngine.isToStall5() == true)
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
				if(ROB.isFull())
				{
					execEngine.setToStall4(true);
					break;
				}
				
				if(fetchBuffer[i] != null
						&& fetchBuffer[i].getOperationType() != OperationType.interrupt
						//&& fetchBuffer[i].getOperationType() != OperationType.store
						)
				{
					if(fetchBuffer[i].getOperationType() == OperationType.load ||
							fetchBuffer[i].getOperationType() == OperationType.store)
					{
						if(execEngine.getCoreMemorySystem().getLsqueue().isFull())
						{
							execEngine.setToStall3(true);
							break;
						}
					}
					
					newROBEntry = makeROBEntries(fetchBuffer[i]);
					decodeBuffer[i] = newROBEntry;
					fetchBuffer[i] = null;
					
					if(SimulationConfig.debugMode)
					{
						System.out.println("decoded : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + fetchBuffer[i]);
					}
				}
				
				/*if(fetchBuffer[i] != null)
				{
					if(fetchBuffer[i].getOperationType() == OperationType.nop)
					{
						try {
							Newmain.instructionPool.returnObject(fetchBuffer[i]);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}*/
				
				execEngine.setToStall3(false);
				execEngine.setToStall4(false);
			}
		}
	}
	
	ReorderBufferEntry makeROBEntries(Instruction newInstruction)
	{
		OperationType tempOpType = null;
		if(newInstruction != null)
		{
			tempOpType = newInstruction.getOperationType();
		}
		
		if(newInstruction != null/* &&
				tempOpType != OperationType.nop &&
				tempOpType != OperationType.inValid*/)
		{			
			ReorderBufferEntry newROBEntry = execEngine.getReorderBuffer()
											.addInstructionToROB(newInstruction, 0);	//TODO
																						//threadID to be attribute of Instruction
																						//instead of 0, write newInstruction.getThreadID()
			
			//if load or store, make entry in LSQ
			if(tempOpType == OperationType.load ||
					tempOpType == OperationType.store)
			{
				boolean isLoad;
				if (newInstruction.getOperationType() == OperationType.load)
					isLoad = true;
				else
					isLoad = false;
					
				//TODO
				execEngine.getCoreMemorySystem().allocateLSQEntry(isLoad, 
						newROBEntry.getInstruction().getSourceOperand1().getValue(),
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
