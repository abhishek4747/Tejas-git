package pipeline.outoforder_new_arch;

import generic.Core;
import generic.Event;
import generic.PortType;
import generic.SimulationElement;

public class IWPushLogic extends SimulationElement {
	
	Core core;
	ExecutionEngine execEngine;
	ReorderBufferEntry[] renameBuffer;
	int decodeWidth;
	
	InstructionWindow IW;
	
	public IWPushLogic(Core core, ExecutionEngine execEngine)
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
					//add to IW
					IW.addToWindow(renameBuffer[i]);
					
					//set rename done flag
					renameBuffer[i].setRenameDone(true);
					
					renameBuffer[i] = null;
					execEngine.setToStall1(false);
				}
			}
		}
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}

}
