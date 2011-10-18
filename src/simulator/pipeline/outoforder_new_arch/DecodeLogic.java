package pipeline.outoforder_new_arch;

import pipeline.outoforder_new_arch.ReorderBufferEntry;
import generic.Core;
import generic.Event;
import generic.Instruction;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class DecodeLogic extends SimulationElement {
	
	Core core;
	ExecutionEngine execEngine;
	Instruction[] fetchBuffer;
	ReorderBufferEntry[] decodeBuffer;
	int decodeWidth;
	
	public DecodeLogic(Core core, ExecutionEngine execEngine)
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
				
				if(fetchBuffer[i] != null)
				{
					newROBEntry = makeROBEntries(fetchBuffer[i]);
					decodeBuffer[i] = newROBEntry;
					fetchBuffer[i] = null;
				}
				
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
		
		if(newInstruction != null &&
				tempOpType != OperationType.nop &&
				tempOpType != OperationType.inValid)
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
				newROBEntry.setLsqEntry(this.core.getExecEngine().coreMemSys.getLsqueue().addEntry(isLoad, 
									newROBEntry.getInstruction().getSourceOperand1().getValue(), newROBEntry));
			}
			
			return newROBEntry;
		}
		
		return null;
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}

}
