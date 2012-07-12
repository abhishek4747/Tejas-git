package generic;

import emulatorinterface.Newmain;

public class CustomOperandPool {
	
	Operand[] pool;
	int head;
	int tail;
	int poolSize;
	
	public CustomOperandPool(int poolSize)
	{
		this.poolSize = poolSize;
		pool = new Operand[poolSize];
		for(int i = 0; i < poolSize; i++)
		{
			pool[i] = new Operand();
		}
		head = 0;
		tail = poolSize - 1;
	}
	
	public Operand borrowObject()
	{
		if(head == -1)
		{
			System.out.println("operand pool empty!!");
			return null;
		}
		
		Operand toBeReturned = pool[head];
		if(head == tail)
		{
			head = tail = -1;
		}
		else
		{
			head = (head + 1)%poolSize;
		}
		return toBeReturned;		
	}
	
	public void returnObject(Operand arg0)
	{
		if(arg0.getMemoryLocationFirstOperand() != null)
		{
			Newmain.operandPool.returnObject(arg0.getMemoryLocationFirstOperand());
		}
		if(arg0.getMemoryLocationSecondOperand() != null)
		{
			Newmain.operandPool.returnObject(arg0.getMemoryLocationSecondOperand());
		}
		
		if(tail == -1)
		{
			head = tail = 0;
		}
		else
		{
			tail = (tail + 1)%poolSize;
		}
		pool[tail] = arg0;
	}

}
