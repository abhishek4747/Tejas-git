package pipeline.outoforder_new_arch;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;

public class InstructionWindow extends SimulationElement {
	
	Core core;
	IWEntry[] IW;
	int maxIWSize;
	
	//int indexOfLastAssignedIWEntry;				//when a new IW entry has to be made,
													//the array IW[] has to be searched for
													//an entry whose isValid = false
													//such a search begins from indexOfLastAssignedIWEntry
													//(instead of from 0 to max_size-1)
													//(search is circular)
	
	int[] availList;
	int availListHead;
	int availListTail;
	
	public InstructionWindow(Core core, ExecutionEngine executionEngine)
	{
		super(PortType.Unlimited, -1, -1, core.getEventQueue(), -1, -1);
		this.core = core;
		maxIWSize = core.getIWSize();
		IW = new IWEntry[maxIWSize];
		availList = new int[maxIWSize];
		for(int i = 0; i < maxIWSize; i++)
		{
			IW[i] = new IWEntry(core, i, executionEngine, this);
			availList[i] = i;
		}
		availListHead = 0;
		availListTail = maxIWSize - 1;
		//indexOfLastAssignedIWEntry = maxIWSize - 1;
		
	}
	
	public IWEntry addToWindow(ReorderBufferEntry ROBEntry)
	{
		int index = findInvalidEntry();
		if(index == -1)
		{
			//Instruction window full
			return null;
		}
		
		IWEntry newEntry = IW[index];
		
		newEntry.setInstruction(ROBEntry.getInstruction());
		newEntry.setAssociatedROBEntry(ROBEntry);
		newEntry.setValid(true);
		
		//indexOfLastAssignedIWEntry = index;
		
		ROBEntry.setAssociatedIWEntry(newEntry);
		return newEntry;
	}
	/*
	int findInvalidEntry()
	{
		int i = (indexOfLastAssignedIWEntry + 1)%maxIWSize;
		while(true)
		{
			if(i == indexOfLastAssignedIWEntry)
			{
				return -1;
			}
			if(IW[i].isValid() == false)
			{
				return i;
			}
			i = (indexOfLastAssignedIWEntry + 1)%maxIWSize;
		}
	}
	*/
	
	int findInvalidEntry()
	{
		if(availListHead == -1)
		{
			return -1;
		}
		
		int temp = availListHead;
		if(availListHead == availListTail)
		{
			availListHead = -1;
			availListTail = -1;
		}
		else
		{
			availListHead = (availListHead + 1)%maxIWSize;
		}
		
		return availList[temp];
	}
	
	public void removeFromWindow(IWEntry entryToBeRemoved)
	{
		entryToBeRemoved.setValid(false);
		availListTail = (availListTail + 1)%maxIWSize;
		availList[availListTail] = entryToBeRemoved.pos;
		if(availListHead == -1)
		{
			availListHead = availListTail;
		}
	}
	
	public void flush()
	{
		for(int i = 0; i < maxIWSize; i++)
		{
			IW[i].setValid(false);
		}
	}

	public IWEntry[] getIW() {
		return IW;
	}
	
	public boolean isFull()
	{
		//if(findInvalidEntry() == -1)
		if(availListHead == -1)
		{
			return true;
		}
		return false;
	}
	
	public int getMaxIWSize() {
		return maxIWSize;
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}

}