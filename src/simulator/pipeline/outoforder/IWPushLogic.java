package pipeline.outoforder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;

public class IWPushLogic extends SimulationElement {
	
	Core core;
	OutOrderExecutionEngine execEngine;
	ReorderBufferEntry[] renameBuffer;
	int decodeWidth;
	
	InstructionWindow IW;
	
	public IWPushLogic(Core core, OutOrderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		this.execEngine = execEngine;
		renameBuffer = execEngine.getRenameBuffer();
		decodeWidth = core.getDecodeWidth();
		
		IW = execEngine.getInstructionWindow();
	}
	
	/*
	 * for each instruction in the renameBuffer, if there is place in the IW, make an entry
	 * else, indicate that all preceding stages must stall from the next cycle
	 */
	public void performIWPush()
	{
		if(execEngine.isToStall5() == true)
		{
			//pipeline stalled due to branch mis-prediction
			return;
		}
		
		for(int i = 0; i < decodeWidth; i++)
		{
			if(renameBuffer[i] != null)
			{
				if(IW.isFull())
				{
					execEngine.setToStall1(true);
					break;
				}
				else
				{
					if(renameBuffer[i].isRenameDone == false)
					{
						System.out.println("cannot push an instruction that hasn't been renamed");
					}
					//add to IW
					IW.addToWindow(renameBuffer[i]);
					
					renameBuffer[i] = null;
					execEngine.setToStall1(false);
				}
			}
		}
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}

}
