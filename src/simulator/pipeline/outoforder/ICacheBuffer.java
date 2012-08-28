package pipeline.outoforder;

import config.SimulationConfig;
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
		head = -1;
		tail = -1;
	}
	
	public void addToBuffer(Instruction newInstruction)
	{
		/*
		 * check if buffer is full before calling this function
		 */
		
		if(head == -1)
		{
			head = tail = 0;
		}
		else
		{
			tail = (tail + 1)%size;
		}
		
		buffer[tail] = newInstruction;
		if(SimulationConfig.detachMemSys == false)
		{
			fetchComplete[tail] = false;
		}
		else
		{
			fetchComplete[tail] = true;
		}
		
		//System.out.println("adding ; " + head + " " + tail + " " + newInstruction);
	}
	
	public Instruction getNextInstruction()
	{
		Instruction toBeReturned = null;
		
		if(head == -1)
			return null;
		
		if(fetchComplete[head] == true)
		{
			toBeReturned = buffer[head];
			if(toBeReturned == null)
			{
				System.out.println("to be returned is null");
			}
			buffer[head] = null;
			if(head == tail)
			{
				head = tail = -1;
			}
			else
			{
				head = (head + 1)%size;
			}
		}
		
		//System.out.println("removing ; " + head + " " + tail + " " + toBeReturned);
		return toBeReturned;
	}
	
	public void updateFetchComplete(long programCounter)
	{
		if(head == -1)
			return;
		
		for(int i = head; ; i = (i + 1)%size)
		{
			if(buffer[i] != null && buffer[i].getRISCProgramCounter() == programCounter)
			{
				fetchComplete[i] = true;
			}
			
			if(i == tail)
				break;
		}
	}
	
	public boolean isFull()
	{
		if((tail + 1)%size == head && head != -1 && buffer[head] != null)
			return true;
		return false;
	}
	
	public void dump()
	{
		if(head == -1)
			return;
		
		for(int i = head; ; i = (i + 1)%size)
		{
			if(buffer[i] != null)
				System.out.println(buffer[i].getRISCProgramCounter() + " : " + fetchComplete[i]);
			
			if(i == tail)
				break;
		}
	}

}
