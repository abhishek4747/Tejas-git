package generic;

import org.apache.commons.pool.PoolableObjectFactory;

import emulatorinterface.Newmain;

public class PoolableInstructionFactory implements PoolableObjectFactory<Instruction> {
	
	int numBorrows = 0;
	int numReturns = 0;

	@Override
	public void activateObject(Instruction arg0) throws Exception {
		
		System.out.println("instruction borrow :" +  ++numBorrows);
	}

	@Override
	public void destroyObject(Instruction arg0) throws Exception {
		
		
	}

	@Override
	public Instruction makeObject() throws Exception {
		
		return new Instruction();
	}

	@Override
	public void passivateObject(Instruction arg0) throws Exception {
		
		if(arg0.getSourceOperand1() != null)
		{
			Newmain.operandPool.returnObject(arg0.getSourceOperand1());
		}
		if(arg0.getSourceOperand2() != null)
		{
			Newmain.operandPool.returnObject(arg0.getSourceOperand2());
		}
		if(arg0.getDestinationOperand() != null)
		{
			Newmain.operandPool.returnObject(arg0.getDestinationOperand());
		}
		
		System.out.println("instruction return :" +  ++numReturns);
		
	}

	@Override
	public boolean validateObject(Instruction arg0) {
		
		return true;
	}

}
