package generic;

import main.CustomObjectPool;

public class CustomOperandPool {
	
	/*Operand[] pool;
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
	*/
	
	GenericCircularBuffer<Operand> pool;
	
	public CustomOperandPool(int poolSize)
	{
		pool = new GenericCircularBuffer<Operand>(Operand.class, poolSize, false);
	}
	
	public Operand borrowObject()
	{
		if(pool.isEmpty()) {
			misc.Error.showErrorAndExit("operand pool empty!! : instructionPoolSize = " + CustomObjectPool.getInstructionPool().getNumIdle());
			return null;
		}
		
		return pool.removeObjectAtHead();		
	}
	
	public void returnObject(Operand arg0)
	{
		arg0.decrementNumReferences();
		if(arg0.getNumReferences()>0) {
			return;
		} else if(arg0.getNumReferences()<0) {
			misc.Error.showErrorAndExit("numReferences < 0 !!");
		}
		
		if(arg0.getMemoryLocationFirstOperand() != null)
		{
			CustomObjectPool.getOperandPool().returnObject(arg0.getMemoryLocationFirstOperand());
		}
		if(arg0.getMemoryLocationSecondOperand() != null)
		{
			CustomObjectPool.getOperandPool().returnObject(arg0.getMemoryLocationSecondOperand());
		}
		
		pool.append(arg0);
	}
	
	public void returnUnusedObject(Operand arg0) {
		if(arg0.getNumReferences()!=0) {
			misc.Error.showErrorAndExit(arg0 + " object is in use !!");
		}
		
		// The components of arg0 will not be removed
		if(arg0.getMemoryLocationFirstOperand()!=null) {
			arg0.setMemoryLocationFirstOperand(null);
		}
		
		if(arg0.getMemoryLocationSecondOperand()!=null) {
			arg0.setMemoryLocationSecondOperand(null);
		}
		
		arg0.incrementNumReferences();
		returnObject(arg0);
	}
	
	public int getSize() {
		return pool.size();
	}

	public long getNumIdle() {
		return getSize();
	}

}
