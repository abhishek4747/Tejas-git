package pipeline.outoforder_new_arch;

import generic.Instruction;

public class ICacheBuffer {
	
	Instruction[] buffer;
	boolean[] fetchComplete;
	int size;
	int head;
	int tail;
	
	public ICacheBuffer(int size)
	{
		this.size = size;
		buffer = new Instruction[size];
		fetchComplete = new boolean[size];
		head = 0;
		tail = 0;
	}
	
	public void addToBuffer(Instruction newInstruction)
	{
		/*
		 * check if buffer is full before calling this function
		 */
		buffer[tail] = newInstruction;
		fetchComplete[tail] = false;
		tail = (tail + 1)%size;
	}
	
	public Instruction getNextInstruction()
	{
		Instruction toBeReturned = null;
		
		if(fetchComplete[head] == true)
		{
			toBeReturned = buffer[head];
			if(toBeReturned == null)
			{
				System.out.println("to be returned is null");
			}
			buffer[head] = null;
			head = (head + 1)%size;
		}
		
		return toBeReturned;
	}
	
	public void updateFetchComplete(long programCounter)
	{
		for(int i = head; ; i = (i + 1)%size)
		{
			if(buffer[i] != null && buffer[i].getProgramCounter() == programCounter)
			{
				fetchComplete[i] = true;
			}
			
			if(i == tail)
				break;
		}
	}
	
	public boolean isFull()
	{
		if((tail + 1)%size == head && buffer[head] != null)
			return true;
		return false;
	}

}
