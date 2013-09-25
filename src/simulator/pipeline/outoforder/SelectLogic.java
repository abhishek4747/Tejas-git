package pipeline.outoforder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;

public class SelectLogic extends SimulationElement {
	
	Core core;
	OutOrderExecutionEngine execEngine;
	InstructionWindow IW;	
	int issueWidth;
	
	public SelectLogic(Core core, OutOrderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1, core.getEventQueue(), -1, -1);
		this.core = core;
		this.execEngine = execEngine;
		IW = execEngine.getInstructionWindow();
		issueWidth = core.getIssueWidth();
	}
	
	/*
	 * ready instructions' issue are attempted (maximum of 'issueWidth' number of issues)
	 * important - all issues must be attempted first; only then must awakening be done
	 * 		this is because an awakened instruction is a
	 * 		candidate for issue ONLY in the next cycle 
	 */
	public void performSelect()
	{
		if(execEngine.isToStall5() == true)
		{
			//pipeline stalled due to branch mis-prediction
			return;
		}
		
		int noIssued = 0;
		ReorderBuffer ROB = execEngine.getReorderBuffer();		
		int i;
		ReorderBufferEntry ROBEntry;
		
		if(ROB.head != -1)
		{
			i = ROB.head;
			do
			{
				ROBEntry = ROB.ROB[i];
				
				if(ROBEntry.getIssued() == false &&
						ROBEntry.getAssociatedIWEntry() != null)
				{
					//Increment counter for power calculation
					//Window selection access is incremented as instruction is being issued here
					this.core.powerCounters.incrementWindowSelectionAccess(1);
					
					//Other window accesses are incremented in the issueInstruction() of IWEntry
					
					if(ROBEntry.getAssociatedIWEntry().issueInstruction())
					{
						//if issued
						noIssued++;						
					}
				}
				
				if(noIssued >= issueWidth)
				{
					break;
				}
				
				i = (i+1)%ROB.MaxROBSize;
				
			}while(i != (ROB.tail+1)%ROB.MaxROBSize);
		}
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
				
	}

}
