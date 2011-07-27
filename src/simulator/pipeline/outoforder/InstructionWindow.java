package pipeline.outoforder;

import generic.Core;
import java.util.LinkedList;

public class InstructionWindow {
	
	Core core;
	LinkedList<IWEntry> IW;
	
	public InstructionWindow(Core core)
	{
		this.core = core;
		IW = new LinkedList<IWEntry>();
	}
	
	public IWEntry addToWindow(ReorderBufferEntry ROBEntry)
	{
		if(IW.size() >= core.getIWSize())
		{
			return null;
		}
		
		IWEntry newEntry = new IWEntry(core, ROBEntry.getInstruction(), ROBEntry);
		IW.addLast(newEntry);
		ROBEntry.setAssociatedIWEntry(newEntry);
		return newEntry;
	}
	
	public void removeFromWindow(IWEntry entryToBeRemoved)
	{
		IW.remove(entryToBeRemoved);
	}
	
	public void flush()
	{
		IW.removeAll(null);
	}

	public LinkedList<IWEntry> getIW() {
		return IW;
	}
	
	public boolean isFull()
	{
		if(IW.size() >= core.getIWSize())
		{
			return true;
		}
		return false;
	}

}